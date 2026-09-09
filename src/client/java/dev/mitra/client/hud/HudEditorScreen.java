package dev.mitra.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.mitra.client.config.MitrasConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import dev.mitra.client.config.HudAnchor;

import static dev.mitra.client.config.MitrasConfig.MAX_SCALE;
import static dev.mitra.client.config.MitrasConfig.MIN_SCALE;

public final class HudEditorScreen extends Screen {

    private static final Component INSTRUCTIONS_MAIN =
            Component.translatable("hud.mitrasautosprinter.editor.instructions");
    private static final Component INSTRUCTIONS_SNAPPING =
            Component.translatable("hud.mitrasautosprinter.editor.instructions_snapping");

    private static final int GRAB_TOLERANCE = 4;
    private static final int BORDER_PADDING = 3;
    private static final double SCALE_STEP = 0.1;
    private static final int OVERLAY_SHADE = 0x66000000;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int BORDER_COLOR_DRAGGING = 0xFF00FF00;
    private static final int GRID_SIZE = 8;
    private static final int GRID_MAJOR_PERIOD = 4;
    private static final int GRID_COLOR_MINOR = 0x18FFFFFF;
    private static final int GRID_COLOR_MAJOR = 0x30FFFFFF;
    private static final int SNAP_THRESHOLD = 6;
    private static final int GUIDE_COLOR = 0xFFFF00FF;
    private static final int NUDGE_LARGE_STEP = 10;
    private static final int READOUT_COLOR = 0xFFFFFFFF;
    private static final int READOUT_MARGIN = 2;
    private static final int NO_GUIDE = -1;

    private final MitrasConfig config;
    private final SprintHud hud;

    private boolean dragging;
    private boolean moved;
    private boolean scaleChanged;
    private boolean gridEnabled;
    private double grabOffsetX;
    private double grabOffsetY;
    private int hudX;
    private int hudY;
    private int guideX = NO_GUIDE;
    private int guideY = NO_GUIDE;

    public HudEditorScreen(MitrasConfig config, SprintHud hud) {
        super(Component.translatable("hud.mitrasautosprinter.editor.title"));
        this.config = config;
        this.hud = hud;
        hud.resetRenderFailure();
    }

    @Override
    protected void init() {
        SprintHud.ElementBox box = hud.resolvedBox(width, height);
        hudX = box.x() + BORDER_PADDING;
        hudY = box.y() + BORDER_PADDING;
        guideX = NO_GUIDE;
        guideY = NO_GUIDE;
        keepOnScreen();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (isOnHud(event.x(), event.y())) {
            if (doubleClick) {
                centerHorizontally();
            }
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
        moveTo((int) (event.x() - grabOffsetX), (int) (event.y() - grabOffsetY), !event.hasControlDown());
        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOnHud(mouseX, mouseY) && scrollY != 0) {
            changeScale(scrollY > 0 ? SCALE_STEP : -SCALE_STEP);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (event.key() == InputConstants.KEY_G) {
            gridEnabled = !gridEnabled;
            return true;
        }
        int step = event.hasShiftDown() ? (gridEnabled ? GRID_SIZE : NUDGE_LARGE_STEP) : 1;
        switch (event.key()) {
            case GLFW.GLFW_KEY_LEFT -> {
                nudge(-step, 0);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                nudge(step, 0);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                nudge(0, -step);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                nudge(0, step);
                return true;
            }
        }
        if (isOnHud(scaledMouseX(), scaledMouseY())) {
            if (event.key() == InputConstants.KEY_R) {
                resetToDefaultPosition();
                return true;
            }
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
        return super.keyPressed(event);
    }

    private void changeScale(double delta) {
        if (hud.isIconMode()) {
            double scale = Math.round((config.hud.hudIconScale.get() + delta) * 10.0) / 10.0;
            config.hud.hudIconScale.accept(Math.clamp(scale, MIN_SCALE, MAX_SCALE));
        } else {
            double scale = Math.round((config.hud.hudTextScale.get() + delta) * 10.0) / 10.0;
            config.hud.hudTextScale.accept(Math.clamp(scale, MIN_SCALE, MAX_SCALE));
        }
        scaleChanged = true;
        keepOnScreen();
    }

    private void moveTo(int x, int y, boolean snapActive) {
        hudX = snapX(x, snapActive);
        hudY = snapY(y, snapActive);
        moved = true;
        keepOnScreen();
    }

    private void nudge(int dx, int dy) {
        hudX += dx;
        hudY += dy;
        guideX = NO_GUIDE;
        guideY = NO_GUIDE;
        moved = true;
        keepOnScreen();
    }

    private void centerHorizontally() {
        hudX = width / 2 - hud.elementWidth() / 2;
        guideX = width / 2;
        moved = true;
        keepOnScreen();
    }

    private void resetToDefaultPosition() {
        config.hud.hudAnchor.accept(HudAnchor.AUTO_CENTER_TOP);
        config.save();
        SprintHud.ElementBox box = hud.resolvedBox(width, height);
        hudX = box.x() + BORDER_PADDING;
        hudY = box.y() + BORDER_PADDING;
        guideX = NO_GUIDE;
        guideY = NO_GUIDE;
        moved = false;
    }

    private int visualPadding() {
        return config.hud.hudBackground.get() && !hud.isIconMode() ? SprintHud.BACKGROUND_PADDING : 0;
    }

    private int snapX(int x, boolean snapActive) {
        guideX = NO_GUIDE;
        if (!snapActive) {
            return x;
        }
        int pad = visualPadding();
        int elementWidth = hud.elementWidth();
        int best = x;
        int bestDistance = SNAP_THRESHOLD;
        int guide = NO_GUIDE;
        int center = width / 2 - elementWidth / 2;
        int right = width - elementWidth - pad;
        int distance = Math.abs(x - pad);
        if (distance < bestDistance) {
            bestDistance = distance;
            best = pad;
            guide = 0;
        }
        distance = Math.abs(x - center);
        if (distance < bestDistance) {
            bestDistance = distance;
            best = center;
            guide = width / 2;
        }
        distance = Math.abs(x - right);
        if (distance < bestDistance) {
            best = right;
            guide = width;
        }
        if (guide == NO_GUIDE && gridEnabled) {
            best = Math.round(x / (float) GRID_SIZE) * GRID_SIZE;
            guide = best;
        }
        guideX = guide;
        return best;
    }

    private int snapY(int y, boolean snapActive) {
        guideY = NO_GUIDE;
        if (!snapActive) {
            return y;
        }
        int pad = visualPadding();
        int elementHeight = hud.elementHeight();
        int best = y;
        int bestDistance = SNAP_THRESHOLD;
        int guide = NO_GUIDE;
        int center = height / 2 - elementHeight / 2;
        int aboveBottomHud = height - SprintHud.BOTTOM_RESERVED_HUD_HEIGHT;
        int bottom = height - elementHeight - pad;
        int distance = Math.abs(y - pad);
        if (distance < bestDistance) {
            bestDistance = distance;
            best = pad;
            guide = 0;
        }
        distance = Math.abs(y - (SprintHud.AUTO_CENTER_TOP_Y - pad));
        if (distance < bestDistance) {
            bestDistance = distance;
            best = SprintHud.AUTO_CENTER_TOP_Y - pad;
            guide = SprintHud.AUTO_CENTER_TOP_Y;
        }
        distance = Math.abs(y - center);
        if (distance < bestDistance) {
            bestDistance = distance;
            best = center;
            guide = height / 2;
        }
        distance = Math.abs(y - (aboveBottomHud - elementHeight - pad));
        if (distance < bestDistance) {
            bestDistance = distance;
            best = aboveBottomHud - elementHeight - pad;
            guide = aboveBottomHud;
        }
        distance = Math.abs(y - bottom);
        if (distance < bestDistance) {
            best = bottom;
            guide = height;
        }
        if (guide == NO_GUIDE && gridEnabled) {
            best = Math.round(y / (float) GRID_SIZE) * GRID_SIZE;
            guide = best;
        }
        guideY = guide;
        return best;
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
            config.hud.hudAnchor.accept(HudAnchor.CUSTOM);
            config.hud.hudX.accept(SprintHud.normalizeCoordinate(hudX, width, hud.elementWidth()));
            config.hud.hudY.accept(SprintHud.normalizeCoordinate(hudY, height, hud.elementHeight()));
        }
        if (moved || scaleChanged) {
            config.save();
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.fill(0, 0, width, height, OVERLAY_SHADE);
        if (gridEnabled) {
            drawGrid(graphics);
        }
        graphics.text(font, INSTRUCTIONS_MAIN, (width - font.width(INSTRUCTIONS_MAIN)) / 2,
                height / 2 - 48, 0xFFFFFFFF, true);
        graphics.text(font, INSTRUCTIONS_SNAPPING, (width - font.width(INSTRUCTIONS_SNAPPING)) / 2,
                height / 2 - 36, 0xFFFFFFFF, true);

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

        if (dragging) {
            drawPositionReadout(graphics);
        }
        if (guideX != NO_GUIDE) {
            graphics.fill(guideX, 0, guideX + 1, height, GUIDE_COLOR);
        }
        if (guideY != NO_GUIDE) {
            graphics.fill(0, guideY, width, guideY + 1, GUIDE_COLOR);
        }
    }

    private void drawGrid(GuiGraphicsExtractor graphics) {
        int line = 0;
        for (int gridX = 0; gridX < width; gridX += GRID_SIZE, line++) {
            graphics.fill(gridX, 0, gridX + 1, height,
                    line % GRID_MAJOR_PERIOD == 0 ? GRID_COLOR_MAJOR : GRID_COLOR_MINOR);
        }
        line = 0;
        for (int gridY = 0; gridY < height; gridY += GRID_SIZE, line++) {
            graphics.fill(0, gridY, width, gridY + 1,
                    line % GRID_MAJOR_PERIOD == 0 ? GRID_COLOR_MAJOR : GRID_COLOR_MINOR);
        }
    }

    private void drawPositionReadout(GuiGraphicsExtractor graphics) {
        String readout = hudX + ", " + hudY;
        int textWidth = font.width(readout);
        int x = Math.clamp(hudX + hud.elementWidth() / 2 - textWidth / 2,
                READOUT_MARGIN, Math.max(READOUT_MARGIN, width - textWidth - READOUT_MARGIN));
        int y = hudY - font.lineHeight - READOUT_MARGIN * 2;
        if (y < READOUT_MARGIN) {
            y = hudY + hud.elementHeight() + READOUT_MARGIN * 2;
        }
        graphics.text(font, Component.literal(readout), x, y, READOUT_COLOR, true);
    }

    private boolean isOnHud(double x, double y) {
        return x >= hudX - GRAB_TOLERANCE && x <= hudX + hud.elementWidth() + GRAB_TOLERANCE
                && y >= hudY - GRAB_TOLERANCE && y <= hudY + hud.elementHeight() + GRAB_TOLERANCE;
    }

    private void keepOnScreen() {
        int pad = visualPadding();
        hudX = Math.clamp(hudX, pad, Math.max(pad, width - hud.elementWidth() - pad));
        hudY = Math.clamp(hudY, pad, Math.max(pad, height - hud.elementHeight() - pad));
    }
}
