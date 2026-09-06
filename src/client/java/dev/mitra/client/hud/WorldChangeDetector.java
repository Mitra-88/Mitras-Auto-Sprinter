package dev.mitra.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class WorldChangeDetector {

    public enum Reason {
        JOINING,
        LOADING_TERRAIN
    }

    private static final int SETTLE_MAX_TICKS = 100;
    private static final double TELEPORT_JUMP_BLOCKS_SQR = 16.0 * 16.0;

    private int settleTicks;
    private LocalPlayer lastPlayer;
    private Vec3 lastPosition;

    public void tick(Minecraft client) {
        detectWorldChange(client);
        if (settleTicks > 0 && isWorldReady(client)) {
            settleTicks = 0;
        }
    }

    public boolean isSettling() {
        return settleTicks > 0;
    }

    public Reason currentReason(Minecraft client) {
        if (client.player == null) {
            return Reason.JOINING;
        }
        return Reason.LOADING_TERRAIN;
    }

    public void countDownDisplayTick() {
        if (settleTicks > 0) {
            settleTicks--;
        }
    }

    public void arm() {
        settleTicks = SETTLE_MAX_TICKS;
    }

    private void detectWorldChange(Minecraft client) {
        LocalPlayer player = client.player;

        if (player != lastPlayer) {
            lastPlayer = player;
            lastPosition = player != null ? player.position() : null;
            if (player != null) {
                arm();
            }
        } else if (player != null) {
            Vec3 position = player.position();
            if (lastPosition != null && position.distanceToSqr(lastPosition) > TELEPORT_JUMP_BLOCKS_SQR) {
                arm();
            }
            lastPosition = position;
        }
    }

    private boolean isWorldReady(Minecraft client) {
        if (client.player == null) {
            return false;
        }
        var screen = client.gui.screen();
        if (screen instanceof LevelLoadingScreen || screen instanceof ProgressScreen) {
            return false;
        }
        ClientLevel level = client.level;
        if (level != null) {
            BlockPos pos = client.player.blockPosition();
            return level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
        }
        return false;
    }
}
