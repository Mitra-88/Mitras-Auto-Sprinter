package dev.mitra.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

final class SprintHud {

    private static final Logger LOGGER = LoggerFactory.getLogger("mitrasautosprinter");
    private static final Identifier ELEMENT_ID = Identifier.fromNamespaceAndPath("mitrasautosprinter", "sprint");

    private static final int BACKGROUND_PADDING = 3;
    private static final long WIDTH_RECHECK_NANOS = 1_000_000_000L;

    private final SprintConfig config;

    private Component textOn;
    private Component textOff;
    private final Map<SprintBlocker, Component> blockedText = new EnumMap<>(SprintBlocker.class);

    private Component text;
    private int color;

    private Integer fixedWidth;
    private long widthCheckedAt;

    private boolean renderBroken;

    SprintHud(SprintConfig config) {
        this.config = config;
        refreshLabels();
        this.text = textOff;
        this.color = config.colorOff;
    }

    void refreshLabels() {
        textOn = Component.literal(config.textOn);
        textOff = Component.literal(config.textOff);
        blockedText.clear();
        for (SprintBlocker reason : SprintBlocker.values()) {
            String label = String.format(config.textBlockedFormat, config.reasonText(reason));
            blockedText.put(reason, Component.literal(label));
        }
        fixedWidth = null;
    }

    void attach() {
        try {
            HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, ELEMENT_ID, this::render);
        } catch (Throwable t) {
            LOGGER.warn("Could not attach the HUD element; auto sprint keeps working without it.", t);
        }
    }

    void update(Minecraft client, boolean sprintEnabled) {
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
            if (reason == null) {
                text = textOn;
                color = config.colorOn;
            } else {
                text = blockedText.get(reason);
                color = config.colorBlocked;
            }
        }
    }

    int width() {
        return fixedTextWidth();
    }

    private int fixedTextWidth() {
        long now = System.nanoTime();
        if (fixedWidth == null || now - widthCheckedAt >= WIDTH_RECHECK_NANOS) {
            var font = Minecraft.getInstance().font;
            int w = Math.max(font.width(textOn), font.width(textOff));
            for (Component component : blockedText.values()) {
                w = Math.max(w, font.width(component));
            }
            fixedWidth = w;
            widthCheckedAt = now;
        }
        return fixedWidth;
    }

    void drawAt(GuiGraphicsExtractor graphics, int x, int y) {
        var font = Minecraft.getInstance().font;
        int boxWidth = fixedTextWidth();
        if (config.hudBackground) {
            graphics.fill(
                    x - BACKGROUND_PADDING,
                    y - BACKGROUND_PADDING,
                    x + boxWidth + BACKGROUND_PADDING,
                    y + font.lineHeight + BACKGROUND_PADDING,
                    config.backgroundColor);
        }
        graphics.text(font, text, x + (boxWidth - font.width(text)) / 2, y, color, true);
    }

    static int clampToScreen(int position, int screenSize, int elementSize) {
        return Math.clamp(position, 0, Math.max(0, screenSize - elementSize));
    }

    private void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!config.hudVisible || editorIsOpen() || renderBroken) {
            return;
        }

        try {
            var font = Minecraft.getInstance().font;
            int boxWidth = fixedTextWidth() + BACKGROUND_PADDING * 2;
            int boxHeight = font.lineHeight + BACKGROUND_PADDING * 2;
            int boxX = config.hudX == SprintConfig.X_CENTER ? (graphics.guiWidth() - boxWidth) / 2 : config.hudX - BACKGROUND_PADDING;
            boxX = clampToScreen(boxX, graphics.guiWidth(), boxWidth);
            int boxY = config.hudY - BACKGROUND_PADDING;
            boxY = clampToScreen(boxY, graphics.guiHeight(), boxHeight);
            drawAt(graphics, boxX + BACKGROUND_PADDING, boxY + BACKGROUND_PADDING);
        } catch (Throwable t) {
            renderBroken = true;
            LOGGER.debug("HUD rendering failed once; disabling it for this session.", t);
        }
    }

    private static boolean editorIsOpen() {
        return Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
    }
}
