package dev.mitra.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.UseEffects;

import java.util.function.Predicate;

enum SprintBlocker {

    DEAD("reasonDead", "Dead", SprintBlocker::isDeadOrGone),
    SPECTATOR("reasonSpectator", "Spectating", LocalPlayer::isSpectator),
    NOT_MOVING("reasonStanding", "Not Moving", SprintBlocker::isStandingStill),
    BLINDNESS("reasonBlind", "Blindness", LocalPlayer::isMobilityRestricted),
    IN_VEHICLE("reasonVehicle", "In Vehicle", SprintBlocker::ridesNonSprintingVehicle),
    TOO_HUNGRY("reasonHungry", "Too Hungry", SprintBlocker::lacksSprintFood),
    SHALLOW_WATER("reasonShallowWater", "Shallow Water", SprintBlocker::isStuckInShallowWater),
    USING_ITEM("reasonUsingItem", "Using Item", SprintBlocker::isSlowedByItemUse),
    ELYTRA("reasonElytra", "Flying", SprintBlocker::isGliding),
    SNEAKING("reasonSneaking", "Sneaking", SprintBlocker::isSneaking),
    CRAWLING("reasonSlow", "Crawling", SprintBlocker::isCrawling),
    HIT_WALL("reasonWall", "Hit Wall", player -> player.horizontalCollision),

    STARTING("reasonWaiting", "Starting...", _ -> false);

    private final String key;
    private final String defaultText;
    private final Predicate<LocalPlayer> blocks;

    SprintBlocker(String key, String defaultText, Predicate<LocalPlayer> blocks) {
        this.key = key;
        this.defaultText = defaultText;
        this.blocks = blocks;
    }

    String key() {
        return key;
    }

    String defaultText() {
        return defaultText;
    }

    static SprintBlocker blocking(LocalPlayer player) {
        for (SprintBlocker reason : values()) {
            if (reason.blocks.test(player)) {
                return reason;
            }
        }
        return STARTING;
    }

    private static boolean isDeadOrGone(LocalPlayer player) {
        return !player.isAlive() || player.isRemoved();
    }

    private static boolean isStandingStill(LocalPlayer player) {
        return !player.input.hasForwardImpulse();
    }

    private static boolean ridesNonSprintingVehicle(LocalPlayer player) {
        if (!player.isPassenger()) {
            return false;
        }
        Entity vehicle = player.getVehicle();
        return vehicle == null || !vehicle.canSprint() || !vehicle.isLocalInstanceAuthoritative();
    }

    private static boolean lacksSprintFood(LocalPlayer player) {
        if (player.isPassenger()) {
            return false;
        }
        return !player.getFoodData().hasEnoughFood() && !player.getAbilities().mayfly;
    }

    private static boolean isStuckInShallowWater(LocalPlayer player) {
        return !player.getAbilities().flying && player.isInShallowWater();
    }

    private static boolean isSlowedByItemUse(LocalPlayer player) {
        return player.isUsingItem()
                && !player.getUseItem().getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).canSprint();
    }

    private static boolean isGliding(LocalPlayer player) {
        return player.isFallFlying() && !player.isUnderWater();
    }

    private static boolean isSneaking(LocalPlayer player) {
        return isMovingSlowly(player) && player.isCrouching();
    }

    private static boolean isCrawling(LocalPlayer player) {
        return isMovingSlowly(player) && !player.isCrouching();
    }

    private static boolean isMovingSlowly(LocalPlayer player) {
        return player.isMovingSlowly() && !player.isUnderWater();
    }
}