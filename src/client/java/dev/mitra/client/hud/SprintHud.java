package dev.mitra.client.hud;

import dev.mitra.client.config.DisplayMode;
import dev.mitra.client.config.HudAnchor;
import dev.mitra.client.config.MitrasConfig;
import dev.mitra.client.sprint.SprintBlocker;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SprintHud {

    private static final Logger LOGGER = LoggerFactory.getLogger("mitrasautosprinter");
    private static final Identifier ELEMENT_ID = Identifier.fromNamespaceAndPath("mitrasautosprinter", "sprint");

    private static final int BACKGROUND_PADDING = 3;
    private static final long WIDTH_RECHECK_NANOS = 1_000_000_000L;
    private static final int ICON_BASE_SIZE = 18;
    private static final float ICON_DIMMED_ALPHA = 0.35f;
    private static final Identifier SPEED_ICON = Hud.getMobEffectSprite(MobEffects.SPEED);
    private static final int ON_CHANGE_TICKS = 60;
    private static final int AUTO_CENTER_TOP_Y = 33;
    private static final int BOTTOM_RESERVED_HUD_HEIGHT = 50;

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
                config.reasons.reasonDead, config.reasons.reasonSpectator, config.reasons.reasonStanding,
                config.reasons.reasonBlind, config.reasons.reasonVehicle, config.reasons.reasonHungry,
                config.reasons.reasonShallowWater, config.reasons.reasonUsingItem, config.reasons.reasonElytra,
                config.reasons.reasonSneaking, config.reasons.reasonSlow, config.reasons.reasonWall,
                config.reasons.reasonRiding)) {
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
        blockedText.put(SprintBlocker.DEAD, Component.literal(config.reasons.reasonDead.get()));
        blockedText.put(SprintBlocker.SPECTATOR, Component.literal(config.reasons.reasonSpectator.get()));
        blockedText.put(SprintBlocker.NOT_MOVING, Component.literal(config.reasons.reasonStanding.get()));
        blockedText.put(SprintBlocker.BLINDNESS, Component.literal(config.reasons.reasonBlind.get()));
        blockedText.put(SprintBlocker.IN_VEHICLE, Component.literal(config.reasons.reasonVehicle.get()));
        blockedText.put(SprintBlocker.TOO_HUNGRY, Component.literal(config.reasons.reasonHungry.get()));
        blockedText.put(SprintBlocker.SHALLOW_WATER, Component.literal(config.reasons.reasonShallowWater.get()));
        blockedText.put(SprintBlocker.USING_ITEM, Component.literal(config.reasons.reasonUsingItem.get()));
        blockedText.put(SprintBlocker.ELYTRA, Component.literal(config.reasons.reasonElytra.get()));
        blockedText.put(SprintBlocker.SNEAKING, Component.literal(config.reasons.reasonSneaking.get()));
        blockedText.put(SprintBlocker.CRAWLING, Component.literal(config.reasons.reasonSlow.get()));
        blockedText.put(SprintBlocker.HIT_WALL, Component.literal(config.reasons.reasonWall.get()));
        blockedText.put(SprintBlocker.RIDING, Component.literal(config.reasons.reasonRiding.get()));
        fixedTextWidthCache = null;
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
            SprintBlocker reason = SprintBlocker.blocking(player);
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
        }
    }

    static int clampToScreen(int position, int screenSize, int elementSize) {
        return Math.clamp(position, 0, Math.max(0, screenSize - elementSize));
    }

    record ElementBox(int x, int y, int width, int height) {}

    ElementBox resolvedBox(int guiWidth, int guiHeight) {
        int boxWidth = elementWidth() + BACKGROUND_PADDING * 2;
        int boxHeight = elementHeight() + BACKGROUND_PADDING * 2;
        HudAnchor anchor = config.hud.hudAnchor.get();
        int x = switch (anchor) {
            case AUTO_CENTER_TOP, TOP_CENTER, BOTTOM_CENTER -> (guiWidth - boxWidth) / 2;
            case TOP_LEFT, BOTTOM_LEFT -> 0;
            case TOP_RIGHT, BOTTOM_RIGHT -> guiWidth - boxWidth;
            case CUSTOM -> config.hud.hudX.get() - BACKGROUND_PADDING;
        };
        int y = switch (anchor) {
            case AUTO_CENTER_TOP -> AUTO_CENTER_TOP_Y - BACKGROUND_PADDING;
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> guiHeight - BOTTOM_RESERVED_HUD_HEIGHT - boxHeight;
            case CUSTOM -> config.hud.hudY.get() - BACKGROUND_PADDING;
        };
        x = clampToScreen(x, guiWidth, boxWidth);
        y = clampToScreen(y, guiHeight, boxHeight);
        return new ElementBox(x, y, boxWidth, boxHeight);
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
            LOGGER.warn("HUD rendering failed once; disabling it for this session.", t);
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
