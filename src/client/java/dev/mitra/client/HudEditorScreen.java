package dev.mitra.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

final class HudEditorScreen extends Screen {

    private static final Component INSTRUCTIONS =
            Component.translatable("hud.mitrasautosprinter.editor.instructions");

    private static final int GRAB_TOLERANCE = 4;
    private static final int BORDER_PADDING = 3;

    private final SprintConfig config;
    private final SprintHud hud;

    private boolean dragging;
    private double grabOffsetX;
    private double grabOffsetY;
    private int hudX;
    private int hudY;

    HudEditorScreen(SprintConfig config, SprintHud hud) {
        super(Component.translatable("hud.mitrasautosprinter.editor.title"));
        this.config = config;
        this.hud = hud;
        this.hudX = config.hudX;
        this.hudY = config.hudY;
    }

    @Override
    protected void init() {
        keepOnScreen();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (isOnHud(event.x(), event.y())) {
            dragging = true;
            grabOffsetX = event.x() - hudX;
            grabOffsetY = event.y() - hudY;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (!dragging) {
            return super.mouseDragged(event, dragX, dragY);
        }
        hudX = (int) (event.x() - grabOffsetX);
        hudY = (int) (event.y() - grabOffsetY);
        keepOnScreen();
        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        super.removed();
        config.hudX = hudX;
        config.hudY = hudY;
        config.save();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.fill(0, 0, width, height, 0x66000000);
        graphics.text(font, INSTRUCTIONS, (width - font.width(INSTRUCTIONS)) / 2, height / 2 - 40, 0xFFFFFFFF, true);

        hud.drawAt(graphics, hudX, hudY);

        int borderColor = dragging ? 0xFF00FF00 : 0xFFFFFFFF;
        graphics.outline(
                hudX - BORDER_PADDING,
                hudY - BORDER_PADDING,
                hudTextWidth() + BORDER_PADDING * 2,
                font.lineHeight + BORDER_PADDING * 2,
                borderColor);
    }

    private boolean isOnHud(double x, double y) {
        return x >= hudX - GRAB_TOLERANCE && x <= hudX + hudTextWidth() + GRAB_TOLERANCE
                && y >= hudY - GRAB_TOLERANCE && y <= hudY + font.lineHeight + GRAB_TOLERANCE;
    }

    private int hudTextWidth() {
        return font.width(hud.text());
    }

    private void keepOnScreen() {
        hudX = SprintHud.clampToScreen(hudX, width, hudTextWidth());
        hudY = SprintHud.clampToScreen(hudY, height, font.lineHeight);
    }
}