package dev.mitra.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public final class HudEditorScreen extends Screen {

    private static final Component INSTRUCTIONS =
            Component.translatable("hud.mitrasautosprinter.editor.instructions");

    private static final int CLICK_TOLERANCE = 4;
    private static final int BORDER_PADDING = 3;

    private final Screen parent;

    private boolean dragging = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    public HudEditorScreen(Screen parent) {
        super(Component.translatable("hud.mitrasautosprinter.editor.title"));
        this.parent = parent;
        MitrasAutoSprinterClient.beginHudEdit();
    }

    @Override
    protected void init() {
        MitrasAutoSprinterClient.clampHudPreviewToScreen(this.width, this.height);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        int[] bounds = MitrasAutoSprinterClient.getHudBoundsForEditor();

        if (bounds != null) {
            double mouseX = event.x();
            double mouseY = event.y();

            if (mouseX >= bounds[0] - CLICK_TOLERANCE && mouseX <= bounds[2] + CLICK_TOLERANCE
                    && mouseY >= bounds[1] - CLICK_TOLERANCE && mouseY <= bounds[3] + CLICK_TOLERANCE) {
                this.dragging = true;
                this.dragOffsetX = mouseX - bounds[0];
                this.dragOffsetY = mouseY - bounds[1];
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragging) {
            MitrasAutoSprinterClient.setHudPreviewPosition(
                    (int) (event.x() - this.dragOffsetX),
                    (int) (event.y() - this.dragOffsetY),
                    this.width,
                    this.height
            );
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        this.dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        MitrasAutoSprinterClient.endHudEdit(true);
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.fill(0, 0, this.width, this.height, 0x66000000);

        int textX = (this.width - this.font.width(INSTRUCTIONS)) / 2;
        int textY = this.height / 2 - 40;
        graphics.text(this.font, INSTRUCTIONS, textX, textY, 0xFFFFFFFF, true);

        MitrasAutoSprinterClient.renderHudPreviewForEditor(graphics);

        int[] bounds = MitrasAutoSprinterClient.getHudBoundsForEditor();
        if (bounds != null) {
            int borderX = Math.max(0, bounds[0] - BORDER_PADDING);
            int borderY = Math.max(0, bounds[1] - BORDER_PADDING);
            int borderW = (bounds[2] - bounds[0]) + BORDER_PADDING * 2;
            int borderH = (bounds[3] - bounds[1]) + BORDER_PADDING * 2;
            int borderColor = this.dragging ? 0xFF00FF00 : 0xFFFFFFFF;
            graphics.outline(borderX, borderY, borderW, borderH, borderColor);
        }
    }
}