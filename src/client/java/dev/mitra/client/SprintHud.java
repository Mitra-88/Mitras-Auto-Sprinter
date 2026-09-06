package dev.mitra.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

final class SprintHud {

    private static final Logger LOGGER = LoggerFactory.getLogger("mitrasautosprinter");
    private static final Identifier ELEMENT_ID = Identifier.fromNamespaceAndPath("mitrasautosprinter", "sprint");

    private static final int BACKGROUND_PADDING = 3;
    private static final long WIDTH_RECHECK_NANOS = 1_000_000_000L;

    private final SprintConfig config;

    private Component textOn;
    private Component textOff;
    private Component textJoining;
    private Component textTerrain;
    private final Map<SprintBlocker, Component> blockedText = new EnumMap<>(SprintBlocker.class);

    private Component text;
    private int color;

    private int settleTicks;
    private LocalPlayer lastPlayer;
    private Vec3 lastPosition;

    private static final int SETTLE_MAX_TICKS = 100;
    private static final double TELEPORT_JUMP_BLOCKS_SQR = 16.0 * 16.0;

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
        textJoining = Component.literal(config.textJoining);
        textTerrain = Component.literal(config.textTerrain);
        blockedText.clear();
        for (SprintBlocker reason : SprintBlocker.values()) {
            String label = String.format(config.textBlockedFormat, config.reasonText(reason));
            blockedText.put(reason, Component.literal(label));
        }
        fixedWidth = null;
    }

    void settleFor() {
        settleTicks = SETTLE_MAX_TICKS;
    }

    void attach() {
        try {
            HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, ELEMENT_ID, this::render);
        } catch (Throwable t) {
            LOGGER.warn("Could not attach the HUD element; auto sprint keeps working without it.", t);
        }
    }

    void update(Minecraft client, boolean sprintEnabled) {
        detectWorldChange(client);

        if (settleTicks > 0) {
            Component settling = settlingLabel(client);
            if (settling != null) {
                text = settling;
                color = config.colorOff;
                settleTicks--;
                return;
            }
            settleTicks = 0;
        }
        updateSprintState(client, sprintEnabled);
    }

    private void detectWorldChange(Minecraft client) {
        LocalPlayer player = client.player;

        if (player != lastPlayer) {
            lastPlayer = player;
            lastPosition = player != null ? player.position() : null;
            if (player != null) {
                settleFor();
            }
        } else if (player != null) {
            Vec3 position = player.position();
            if (lastPosition != null && position.distanceToSqr(lastPosition) > TELEPORT_JUMP_BLOCKS_SQR) {
                settleFor();
            }
            lastPosition = position;
        }
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
            Optional<SprintBlocker> reason = SprintBlocker.blocking(player);
            if (reason.isPresent()) {
                text = blockedText.get(reason.get());
                color = config.colorBlocked;
            } else {
                text = textOn;
                color = config.colorOn;
            }
        }
    }

    private Component settlingLabel(Minecraft client) {
        if (client.player == null) {
            return textJoining;
        }
        if (client.gui.screen() instanceof LevelLoadingScreen || client.gui.screen() instanceof ProgressScreen) {
            return textTerrain; // vanilla is still swapping the world
        }
        ClientLevel level = client.level;
        if (level != null) {
            BlockPos pos = client.player.blockPosition();
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                return textTerrain;
            }
        }
        return null;
    }

    boolean isSettling() {
        return settleTicks > 0;
    }

    void drawAtConfigured(GuiGraphicsExtractor graphics) {
        if (renderBroken) {
            return;
        }
        try {
            var font = Minecraft.getInstance().font;
            int boxWidth = fixedTextWidth() + BACKGROUND_PADDING * 2;
            int boxHeight = font.lineHeight + BACKGROUND_PADDING * 2;
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

    int width() {
        return fixedTextWidth();
    }

    private int fixedTextWidth() {
        long now = System.nanoTime();
        if (fixedWidth == null || now - widthCheckedAt >= WIDTH_RECHECK_NANOS) {
            var font = Minecraft.getInstance().font;
            int w = Math.max(font.width(textOn), font.width(textOff));
            w = Math.max(w, font.width(textJoining));
            w = Math.max(w, font.width(textTerrain));
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
        drawAtConfigured(graphics);
    }

    private static boolean editorIsOpen() {
        return Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
    }
}
