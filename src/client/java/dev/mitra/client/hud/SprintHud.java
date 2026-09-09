package dev.mitra.client.hud;

import dev.mitra.client.config.DisplayMode;
import dev.mitra.client.config.HudAnchor;
import dev.mitra.client.config.MitrasConfig;
import dev.mitra.client.config.TextColorMode;
import dev.mitra.client.sprint.SprintBlocker;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SprintHud {

    private static final Logger LOGGER = LoggerFactory.getLogger("mitrasautosprinter");
    private static final Identifier ELEMENT_ID = Identifier.fromNamespaceAndPath("mitrasautosprinter", "sprint");

    static final int BACKGROUND_PADDING = 3;
    private static final long WIDTH_RECHECK_NANOS = 1_000_000_000L;
    private static final int ICON_BASE_SIZE = 18;
    private static final float ICON_DIMMED_ALPHA = 0.35f;
    private static final Identifier SPEED_ICON = Hud.getMobEffectSprite(MobEffects.SPEED);
    private static final int ON_CHANGE_TICKS = 60;
    static final int AUTO_CENTER_TOP_Y = 33;
    static final int BOTTOM_RESERVED_HUD_HEIGHT = 50;
    private static final int HUE_STEPS = 360;
    private static final long HUE_CYCLE_MS = 3000;
    private static final int CHROMA_CHAR_SPREAD_DEGREES = 30;
    private static final int GLYPH_CACHE_SIZE = 128;
    private static final int[] HUE_LUT = new int[HUE_STEPS];
    private static final String[] GLYPH_CACHE = new String[GLYPH_CACHE_SIZE];
    private static final int[] GLYPH_WIDTH_CACHE = new int[GLYPH_CACHE_SIZE];
    private static Font glyphCacheFont;

    static {
        for (int i = 0; i < HUE_STEPS; i++) {
            HUE_LUT[i] = 0xFF000000 | Mth.hsvToRgb(i / (float) HUE_STEPS, 0.85f, 1.0f);
        }
    }

    private final MitrasConfig config;
    private final WorldChangeDetector worldChange = new WorldChangeDetector();

    private Component textOn;
    private Component textOff;
    private Component textJoining;
    private Component textTerrain;
    private final Map<SprintBlocker, Component> blockedText = new EnumMap<>(SprintBlocker.class);
    private volatile boolean labelsDirty = true;

    private Component text;
    private int color;
    private boolean stateOn;
    private boolean blocked;
    private Component lastText;
    private int onChangeTicks;

    private Integer fixedTextWidthCache;
    private long widthCheckedAt;

    private boolean renderBroken;
    public SprintHud(MitrasConfig config) {
        this.config = config;
        rebuildLabels();
        for (me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString label : List.of(
                config.text.textOn, config.text.textOff, config.text.textJoining,
                config.text.textTerrain, config.text.textBlockedFormat,
                config.reasons.reasonStanding, config.reasons.reasonRestricted,
                config.reasons.reasonVehicle, config.reasons.reasonHungry,
                config.reasons.reasonShallowWater, config.reasons.reasonUsingItem, config.reasons.reasonElytra,
                config.reasons.reasonSneaking, config.reasons.reasonSlow, config.reasons.reasonWall)) {
            label.listenToEntry(_ -> labelsDirty = true);
        }

        this.text = textOff;
        this.color = config.hud.colorOff.get().argb();
    }

    public void attach() {
        try {
            HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, ELEMENT_ID, this::render);
        } catch (Throwable t) {
            LOGGER.warn("Could not attach the HUD element; auto sprint keeps working without it.", t);
        }
    }

    public void update(Minecraft client, boolean sprintEnabled) {
        worldChange.tick(client);

        if (worldChange.consumeWorldChanged()) {
            renderBroken = false;
        }

        if (labelsDirty) {
            rebuildLabels();
            labelsDirty = false;
        }

        if (onChangeTicks > 0) {
            onChangeTicks--;
        }

        if (worldChange.isSettling()) {
            text = labelFor(worldChange.currentReason(client));
            color = config.hud.colorOff.get().argb();
            stateOn = false;
            blocked = false;
            worldChange.countDownDisplayTick();
            return;
        }
        updateSprintState(client, sprintEnabled);
    }

    private Component labelFor(WorldChangeDetector.Reason reason) {
        return switch (reason) {
            case JOINING -> textJoining;
            case LOADING_TERRAIN -> textTerrain;
        };
    }

    private void rebuildLabels() {
        textOn = Component.literal(config.text.textOn.get());
        textOff = Component.literal(config.text.textOff.get());
        textJoining = Component.literal(config.text.textJoining.get());
        textTerrain = Component.literal(config.text.textTerrain.get());
        blockedText.clear();
        blockedText.put(SprintBlocker.NOT_MOVING, Component.literal(config.reasons.reasonStanding.get()));
        blockedText.put(SprintBlocker.RESTRICTED, Component.literal(config.reasons.reasonRestricted.get()));
        blockedText.put(SprintBlocker.IN_VEHICLE, Component.literal(config.reasons.reasonVehicle.get()));
        blockedText.put(SprintBlocker.TOO_HUNGRY, Component.literal(config.reasons.reasonHungry.get()));
        blockedText.put(SprintBlocker.SHALLOW_WATER, Component.literal(config.reasons.reasonShallowWater.get()));
        blockedText.put(SprintBlocker.USING_ITEM, Component.literal(config.reasons.reasonUsingItem.get()));
        blockedText.put(SprintBlocker.ELYTRA, Component.literal(config.reasons.reasonElytra.get()));
        blockedText.put(SprintBlocker.SNEAKING, Component.literal(config.reasons.reasonSneaking.get()));
        blockedText.put(SprintBlocker.CRAWLING, Component.literal(config.reasons.reasonSlow.get()));
        blockedText.put(SprintBlocker.HIT_WALL, Component.literal(config.reasons.reasonWall.get()));
        fixedTextWidthCache = null;
        widthCheckedAt = 0;
    }

    private void updateSprintState(Minecraft client, boolean sprintEnabled) {
        LocalPlayer player = client.player;
        int onColor = config.hud.colorOn.get().argb();
        int offColor = config.hud.colorOff.get().argb();
        int blockedColor = config.hud.colorBlocked.get().argb();

        if (!sprintEnabled) {
            text = textOff;
            color = offColor;
            blocked = false;
        } else if (player == null) {
            text = textOn;
            color = offColor;
            blocked = false;
        } else if (player.isSprinting()) {
            text = textOn;
            color = onColor;
            blocked = false;
        } else {
            SprintBlocker reason = SprintBlocker.whyNotSprinting(player);
            if (reason != null) {
                text = blockedText.get(reason);
                color = blockedColor;
                blocked = true;
            } else {
                text = textOn;
                color = onColor;
                blocked = false;
            }
        }
        stateOn = color == onColor;

        if (text != lastText) {
            lastText = text;
            onChangeTicks = ON_CHANGE_TICKS;
        }
    }

    public boolean isSettling() {
        return worldChange.isSettling();
    }

    public void settleFor() {
        worldChange.arm();
    }

    boolean isIconMode() {
        return config.hud.displayMode.get() == DisplayMode.ICON;
    }

    int elementWidth() {
        return isIconMode() ? iconSize() : fixedTextWidth();
    }

    int elementHeight() {
        return isIconMode() ? iconSize() : Minecraft.getInstance().font.lineHeight;
    }

    private int iconSize() {
        return (int) Math.round(ICON_BASE_SIZE * config.hud.hudIconScale.get());
    }

    private int fixedTextWidth() {
        long now = System.nanoTime();
        if (fixedTextWidthCache == null || now - widthCheckedAt >= WIDTH_RECHECK_NANOS) {
            var font = Minecraft.getInstance().font;
            int w = Math.max(font.width(textOn), font.width(textOff));
            w = Math.max(w, font.width(textJoining));
            w = Math.max(w, font.width(textTerrain));
            for (Component component : blockedText.values()) {
                w = Math.max(w, font.width(component));
            }
            fixedTextWidthCache = w;
            widthCheckedAt = now;
        }
        return fixedTextWidthCache;
    }

    void drawAt(GuiGraphicsExtractor graphics, int x, int y) {
        var font = Minecraft.getInstance().font;
        int elementWidth = elementWidth();
        int elementHeight = elementHeight();
        if (config.hud.hudBackground) {
            graphics.fill(
                    x - BACKGROUND_PADDING,
                    y - BACKGROUND_PADDING,
                    x + elementWidth + BACKGROUND_PADDING,
                    y + elementHeight + BACKGROUND_PADDING,
                    config.hud.backgroundColor.get().argb());
        }
        if (isIconMode()) {
            float alpha = stateOn ? 1.0f : ICON_DIMMED_ALPHA;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    SPEED_ICON,
                    x, y, elementWidth, elementHeight,
                    ARGB.white(alpha));
        } else {
            graphics.text(font, text, x + (fixedTextWidth() - font.width(text)) / 2, y, color, config.hud.hudTextShadow);
            int textX = x + (fixedTextWidth() - font.width(text)) / 2;
            TextColorMode colorMode = config.hud.textColorMode.get();
            if (colorMode == TextColorMode.SOLID) {
                graphics.text(font, text, textX, y, color, config.hud.hudTextShadow);
            } else {
                drawCyclingText(graphics, font, textX, y, colorMode == TextColorMode.CHROMA);
            }
        }
    }

    private void drawCyclingText(GuiGraphicsExtractor graphics, Font font, int x, int y, boolean chroma) {
        String string = text.getString();
        int length = string.length();
        if (length == 0) {
            return;
        }
        int hueBase = (int) (Util.getMillis() % HUE_CYCLE_MS * HUE_STEPS / HUE_CYCLE_MS);
        int charX = x;
        for (int i = 0; i < length; i++) {
            char c = string.charAt(i);
            int width = glyphWidth(font, c);
            if (c != ' ') {
                int hue = chroma ? (hueBase + i * CHROMA_CHAR_SPREAD_DEGREES) % HUE_STEPS : hueBase;
                graphics.text(font, glyph(c), charX, y, HUE_LUT[hue], config.hud.hudTextShadow);
            }
            charX += width;
        }
    }

    private static String glyph(char c) {
        if (c >= GLYPH_CACHE_SIZE) {
            return String.valueOf(c);
        }
        String cached = GLYPH_CACHE[c];
        if (cached == null) {
            cached = String.valueOf(c);
            GLYPH_CACHE[c] = cached;
        }
        return cached;
    }

    private static int glyphWidth(Font font, char c) {
        if (c >= GLYPH_CACHE_SIZE) {
            return font.width(glyph(c));
        }
        if (glyphCacheFont != font) {
            Arrays.fill(GLYPH_WIDTH_CACHE, -1);
            glyphCacheFont = font;
        }
        int cached = GLYPH_WIDTH_CACHE[c];
        if (cached < 0) {
            cached = font.width(glyph(c));
            GLYPH_WIDTH_CACHE[c] = cached;
        }
        return cached;
    }

    static int clampToScreen(int position, int screenSize, int elementSize) {
        return Math.clamp(position, 0, Math.max(0, screenSize - elementSize));
    }


    static double normalizeCoordinate(int position, int screenSize, int elementSize) {
        int travel = Math.max(1, screenSize - elementSize);
        return Math.clamp(position / (double) travel, 0.0, 1.0);
    }

    static int denormalizeCoordinate(double normalized, int screenSize, int elementSize) {
        return (int) Math.round(normalized * Math.max(0, screenSize - elementSize));
    }

    record ElementBox(int x, int y, int width, int height) {}

    ElementBox resolvedBox(int guiWidth, int guiHeight) {
        int elementWidth = elementWidth();
        int elementHeight = elementHeight();
        int boxWidth = elementWidth + BACKGROUND_PADDING * 2;
        int boxHeight = elementHeight + BACKGROUND_PADDING * 2;
        HudAnchor anchor = config.hud.hudAnchor.get();
        int x = switch (anchor) {
            case AUTO_CENTER_TOP, TOP_CENTER, BOTTOM_CENTER -> (guiWidth - boxWidth) / 2;
            case TOP_LEFT, BOTTOM_LEFT -> 0;
            case TOP_RIGHT, BOTTOM_RIGHT -> guiWidth - boxWidth;

            case CUSTOM -> denormalizeCoordinate(config.hud.hudX.get(), guiWidth, elementWidth) - BACKGROUND_PADDING;
        };
        int y = switch (anchor) {
            case AUTO_CENTER_TOP -> AUTO_CENTER_TOP_Y - BACKGROUND_PADDING;
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> guiHeight - BOTTOM_RESERVED_HUD_HEIGHT - boxHeight;
            case CUSTOM -> denormalizeCoordinate(config.hud.hudY.get(), guiHeight, elementHeight) - BACKGROUND_PADDING;
        };
        x = clampToScreen(x, guiWidth, boxWidth);
        y = clampToScreen(y, guiHeight, boxHeight);
        return new ElementBox(x, y, boxWidth, boxHeight);
    }

    public void resetRenderFailure() {
        renderBroken = false;
    }

    public void drawAtConfiguredPosition(GuiGraphicsExtractor graphics) {
        if (renderBroken) {
            return;
        }
        try {
            ElementBox box = resolvedBox(graphics.guiWidth(), graphics.guiHeight());
            drawAt(graphics, box.x() + BACKGROUND_PADDING, box.y() + BACKGROUND_PADDING);
        } catch (Throwable t) {
            renderBroken = true;
            LOGGER.warn("HUD rendering failed once; disabling it until the HUD editor is opened or the world is changed.", t);
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.mitrasautosprinter.hud_failed"));
            }
        }
    }

    private void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!config.hud.hudVisible || editorIsOpen() || renderBroken) {
            return;
        }
        if (!shouldRender()) {
            return;
        }
        drawAtConfiguredPosition(graphics);
    }

    private boolean shouldRender() {
        return switch (config.hud.showMode.get()) {
            case ALWAYS -> true;
            case BLOCKED_ONLY -> blocked || worldChange.isSettling();
            case ON_CHANGE -> onChangeTicks > 0 || worldChange.isSettling();
        };
    }

    private static boolean editorIsOpen() {
        return Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
    }
}
