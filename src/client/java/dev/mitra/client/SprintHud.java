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

    private final SprintConfig config;

    private final Component textOn;
    private final Component textOff;
    private final Map<SprintBlocker, Component> blockedText = new EnumMap<>(SprintBlocker.class);

    private Component text;
    private int color;

    private boolean renderBroken;

    SprintHud(SprintConfig config) {
        this.config = config;
        this.textOn = Component.literal(config.textOn);
        this.textOff = Component.literal(config.textOff);
        for (SprintBlocker reason : SprintBlocker.values()) {
            String label = String.format(config.textBlockedFormat, config.reasonText(reason));
            blockedText.put(reason, Component.literal(label));
        }
        this.text = textOff;
        this.color = config.colorOff;
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
            text = blockedText.get(SprintBlocker.blocking(player));
            color = config.colorBlocked;
        }
    }

    Component text() {
        return text;
    }

    void drawAt(GuiGraphicsExtractor graphics, int x, int y) {
        var font = Minecraft.getInstance().font;
        if (config.hudBackground) {
            graphics.fill(
                    x - BACKGROUND_PADDING,
                    y - BACKGROUND_PADDING,
                    x + font.width(text) + BACKGROUND_PADDING,
                    y + font.lineHeight + BACKGROUND_PADDING,
                    config.backgroundColor);
        }
        graphics.text(font, text, x, y, color, true);
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
            int x = clampToScreen(config.hudX, graphics.guiWidth(), font.width(text));
            int y = clampToScreen(config.hudY, graphics.guiHeight(), font.lineHeight);
            drawAt(graphics, x, y);
        } catch (Throwable t) {
            renderBroken = true;
            LOGGER.debug("HUD rendering failed once; disabling it for this session.", t);
        }
    }

    private static boolean editorIsOpen() {
        return Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
    }
}