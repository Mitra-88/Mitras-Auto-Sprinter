package dev.mitra.client.sprint;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.UseEffects;

import java.util.function.Predicate;

public enum SprintBlocker {

    DEAD(SprintBlocker::isDeadOrGone),
    SPECTATOR(LocalPlayer::isSpectator),
    NOT_MOVING(SprintBlocker::isStandingStill),
    BLINDNESS(LocalPlayer::isMobilityRestricted),
    IN_VEHICLE(SprintBlocker::ridesNonSprintingVehicle),
    TOO_HUNGRY(SprintBlocker::lacksSprintFood),
    SHALLOW_WATER(SprintBlocker::isStuckInShallowWater),
    USING_ITEM(SprintBlocker::isSlowedByItemUse),
    ELYTRA(SprintBlocker::isGliding),
    SNEAKING(SprintBlocker::isSneaking),
    CRAWLING(SprintBlocker::isCrawling),
    HIT_WALL(player -> player.horizontalCollision),
    RIDING(LocalPlayer::isPassenger);

    private final Predicate<LocalPlayer> blocks;

    private static final SprintBlocker[] VALUES = values();

    SprintBlocker(Predicate<LocalPlayer> blocks) {
        this.blocks = blocks;
    }

    public static SprintBlocker blocking(LocalPlayer player) {
        for (SprintBlocker reason : VALUES) {
            if (reason.blocks.test(player)) {
                return reason;
            }
        }
        return null;
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
