package dev.mitra.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.mitra.client.config.SprintConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import static dev.mitra.client.config.SprintConfig.AUTO_POSITION;
import static dev.mitra.client.config.SprintConfig.DEFAULT_HUD_Y;
import static dev.mitra.client.config.SprintConfig.MAX_ICON_SCALE;
import static dev.mitra.client.config.SprintConfig.MIN_ICON_SCALE;

public final class HudEditorScreen extends Screen {

    private static final Component INSTRUCTIONS =
            Component.translatable("hud.mitrasautosprinter.editor.instructions");

    private static final int GRAB_TOLERANCE = 4;
    private static final int BORDER_PADDING = 3;
    private static final double SCALE_STEP = 0.1;
    private static final int OVERLAY_SHADE = 0x66000000;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int BORDER_COLOR_DRAGGING = 0xFF00FF00;

    private final SprintConfig config;
    private final SprintHud hud;

    private boolean dragging;
    private boolean moved;
    private boolean scaleChanged;
    private double grabOffsetX;
    private double grabOffsetY;
    private int hudX;
    private int hudY;

    public HudEditorScreen(SprintConfig config, SprintHud hud) {
        super(Component.translatable("hud.mitrasautosprinter.editor.title"));
        this.config = config;
        this.hud = hud;
        this.hudX = config.hudX;
        this.hudY = config.hudY;
    }

    @Override
    protected void init() {
        if (hudX == AUTO_POSITION) {
            int boxWidth = hud.elementWidth() + BORDER_PADDING * 2;
            hudX = (width - boxWidth) / 2 + BORDER_PADDING;
        }
        if (hudY == AUTO_POSITION) {
            int boxHeight = hud.elementHeight() + BORDER_PADDING * 2;
            hudY = (height - boxHeight) / 2 + BORDER_PADDING;
        }
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
        moved = true;
        keepOnScreen();
        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hud.isIconMode() && isOnHud(mouseX, mouseY) && scrollY != 0) {
            changeScale(scrollY > 0 ? SCALE_STEP : -SCALE_STEP);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (isOnHud(scaledMouseX(), scaledMouseY())) {
            if (event.key() == InputConstants.KEY_R) {
                resetToDefaultPosition();
                return true;
            }
            if (hud.isIconMode()) {
                if (event.key() == InputConstants.KEY_EQUALS
                        || event.key() == InputConstants.KEY_ADD) {
                    changeScale(SCALE_STEP);
                    return true;
                }
                if (event.key() == InputConstants.KEY_MINUS
                        || event.key() == GLFW.GLFW_KEY_KP_SUBTRACT) {
                    changeScale(-SCALE_STEP);
                    return true;
                }
            }
        }
        return super.keyPressed(event);
    }

    private void changeScale(double delta) {
        double scale = Math.round((config.hudIconScale + delta) * 10.0) / 10.0;
        config.hudIconScale = Math.clamp(scale, MIN_ICON_SCALE, MAX_ICON_SCALE);
        scaleChanged = true;
        keepOnScreen();
    }

    private void resetToDefaultPosition() {
        config.hudX = AUTO_POSITION;
        config.hudY = DEFAULT_HUD_Y;
        config.save();
        int boxWidth = hud.elementWidth() + BORDER_PADDING * 2;
        hudX = (width - boxWidth) / 2 + BORDER_PADDING;
        hudY = DEFAULT_HUD_Y;
        moved = false;
    }

    private double scaledMouseX() {
        return minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
    }

    private double scaledMouseY() {
        return minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
    }

    @Override
    public void removed() {
        super.removed();
        if (moved) {
            config.hudX = hudX;
            config.hudY = hudY;
        }
        if (moved || scaleChanged) {
            config.save();
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.fill(0, 0, width, height, OVERLAY_SHADE);
        graphics.text(font, INSTRUCTIONS, (width - font.width(INSTRUCTIONS)) / 2, height / 2 - 40, 0xFFFFFFFF, true);

        try {
            hud.drawAt(graphics, hudX, hudY);
        } catch (Throwable ignored) {
        }

        graphics.outline(
                hudX - BORDER_PADDING,
                hudY - BORDER_PADDING,
                hud.elementWidth() + BORDER_PADDING * 2,
                hud.elementHeight() + BORDER_PADDING * 2,
                dragging ? BORDER_COLOR_DRAGGING : BORDER_COLOR);
    }

    private boolean isOnHud(double x, double y) {
        return x >= hudX - GRAB_TOLERANCE && x <= hudX + hud.elementWidth() + GRAB_TOLERANCE
                && y >= hudY - GRAB_TOLERANCE && y <= hudY + hud.elementHeight() + GRAB_TOLERANCE;
    }

    private void keepOnScreen() {
        hudX = SprintHud.clampToScreen(hudX - BORDER_PADDING, width, hud.elementWidth() + BORDER_PADDING * 2)
                + BORDER_PADDING;
        hudY = SprintHud.clampToScreen(hudY - BORDER_PADDING, height, hud.elementHeight() + BORDER_PADDING * 2)
                + BORDER_PADDING;
    }
}
