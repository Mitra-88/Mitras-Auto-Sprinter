package dev.mitra.client.hud;

import dev.mitra.client.config.SprintConfig;
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
import java.util.Map;

public final class SprintHud {

    private static final Logger LOGGER = LoggerFactory.getLogger("mitrasautosprinter");
    private static final Identifier ELEMENT_ID = Identifier.fromNamespaceAndPath("mitrasautosprinter", "sprint");

    private static final int BACKGROUND_PADDING = 3;
    private static final long WIDTH_RECHECK_NANOS = 1_000_000_000L;
    private static final int ICON_BASE_SIZE = 18;
    private static final float ICON_DIMMED_ALPHA = 0.35f;
    private static final Identifier SPEED_ICON = Hud.getMobEffectSprite(MobEffects.SPEED);

    private final SprintConfig config;
    private final WorldChangeDetector worldChange = new WorldChangeDetector();

    private Component textOn;
    private Component textOff;
    private Component textJoining;
    private Component textTerrain;
    private final Map<SprintBlocker, Component> blockedText = new EnumMap<>(SprintBlocker.class);

    private Component text;
    private int color;
    private boolean stateOn;

    private Integer fixedTextWidthCache;
    private long widthCheckedAt;

    private boolean renderBroken;

    public SprintHud(SprintConfig config) {
        this.config = config;
        refreshLabels();
        this.text = textOff;
        this.color = config.colorOff;
    }

    public void refreshLabels() {
        textOn = Component.literal(config.textOn);
        textOff = Component.literal(config.textOff);
        textJoining = Component.literal(config.textJoining);
        textTerrain = Component.literal(config.textTerrain);
        blockedText.clear();
        for (SprintBlocker reason : SprintBlocker.values()) {
            String label = String.format(config.textBlockedFormat, config.reasonText(reason));
            blockedText.put(reason, Component.literal(label));
        }
        fixedTextWidthCache = null;
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

        if (worldChange.isSettling()) {
            text = labelFor(worldChange.currentReason(client));
            color = config.colorOff;
            stateOn = false;
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

    private void updateSprintState(Minecraft client, boolean sprintEnabled) {
        LocalPlayer player = client.player;

        if (!sprintEnabled) {
            text = textOff;
            color = config.colorOff;
        } else if (player == null) {
            text = textOn;
            color = config.colorOff;
        } else if (player.isSprinting()) {
            text = textOn;
            color = config.colorOn;
        } else {
            SprintBlocker reason = SprintBlocker.blocking(player);
            if (reason != null) {
                text = blockedText.get(reason);
                color = config.colorBlocked;
            } else {
                text = textOn;
                color = config.colorOn;
            }
        }
        stateOn = color == config.colorOn;
    }

    public void settleFor() {
        worldChange.arm();
    }

    public boolean isSettling() {
        return worldChange.isSettling();
    }

    boolean isIconMode() {
        return SprintConfig.DISPLAY_MODE_ICON.equals(config.displayMode);
    }

    int elementWidth() {
        return isIconMode() ? iconSize() : fixedTextWidth();
    }

    int elementHeight() {
        return isIconMode() ? iconSize() : Minecraft.getInstance().font.lineHeight;
    }

    private int iconSize() {
        return Math.round((float) (ICON_BASE_SIZE * config.hudIconScale));
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
        if (config.hudBackground) {
            graphics.fill(
                    x - BACKGROUND_PADDING,
                    y - BACKGROUND_PADDING,
                    x + elementWidth + BACKGROUND_PADDING,
                    y + elementHeight + BACKGROUND_PADDING,
                    config.backgroundColor);
        }
        if (isIconMode()) {
            float alpha = stateOn ? 1.0f : ICON_DIMMED_ALPHA;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    SPEED_ICON,
                    x, y, elementWidth, elementHeight,
                    ARGB.white(alpha));
        } else {
            graphics.text(font, text, x + (fixedTextWidth() - font.width(text)) / 2, y, color, true);
        }
    }

    static int clampToScreen(int position, int screenSize, int elementSize) {
        return Math.clamp(position, 0, Math.max(0, screenSize - elementSize));
    }

    public void drawAtConfiguredPosition(GuiGraphicsExtractor graphics) {
        if (renderBroken) {
            return;
        }
        try {
            int boxWidth = elementWidth() + BACKGROUND_PADDING * 2;
            int boxHeight = elementHeight() + BACKGROUND_PADDING * 2;
            int boxX = config.hudX == SprintConfig.AUTO_POSITION
                    ? (graphics.guiWidth() - boxWidth) / 2
                    : config.hudX - BACKGROUND_PADDING;
            boxX = clampToScreen(boxX, graphics.guiWidth(), boxWidth);
            int boxY = config.hudY == SprintConfig.AUTO_POSITION
                    ? (graphics.guiHeight() - boxHeight) / 2
                    : config.hudY - BACKGROUND_PADDING;
            boxY = clampToScreen(boxY, graphics.guiHeight(), boxHeight);
            drawAt(graphics, boxX + BACKGROUND_PADDING, boxY + BACKGROUND_PADDING);
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
        if (!config.hudVisible || editorIsOpen() || renderBroken) {
            return;
        }
        drawAtConfiguredPosition(graphics);
    }

    private static boolean editorIsOpen() {
        return Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
    }
}
