package dev.mitra.client.sprint;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.UseEffects;

import java.util.function.Predicate;

public enum SprintBlocker {

    NOT_MOVING(SprintBlocker::isStandingStill),
    RESTRICTED(LocalPlayer::isMobilityRestricted),
    IN_VEHICLE(SprintBlocker::ridesNonSprintingVehicle),
    TOO_HUNGRY(SprintBlocker::lacksSprintFood),
    SHALLOW_WATER(SprintBlocker::isStuckInShallowWater),
    USING_ITEM(SprintBlocker::isSlowedByItemUse),
    ELYTRA(SprintBlocker::isGliding),
    SNEAKING(SprintBlocker::isSneaking),
    CRAWLING(SprintBlocker::isCrawling),
    HIT_WALL(SprintBlocker::hitWall, false);

    private final Predicate<LocalPlayer> blocks;
    private final boolean startBlocker;

    private static final SprintBlocker[] VALUES = values();

    SprintBlocker(Predicate<LocalPlayer> blocks) {
        this(blocks, true);
    }

    SprintBlocker(Predicate<LocalPlayer> blocks, boolean startBlocker) {
        this.blocks = blocks;
        this.startBlocker = startBlocker;
    }

    public static SprintBlocker blocking(LocalPlayer player) {
        for (SprintBlocker reason : VALUES) {
            if (reason.startBlocker && reason.blocks.test(player)) {
                return reason;
            }
        }
        return null;
    }

    public static boolean shouldStopSprinting(LocalPlayer player) {
        return player.isSprinting()
                && (player.isSwimming() ? shouldStopSwimSprinting(player) : shouldStopRunSprinting(player));
    }

    public static SprintBlocker stopReason(LocalPlayer player) {
        if (player.isSwimming()) {
            if (!isSprintingPossible(player, true)) {
                return sprintingPossibleReason(player, true);
            }
            if (!player.isInWater()) {
                return null;
            }
            if (!player.input.hasForwardImpulse() && !player.onGround() && !player.isShiftKeyDown()) {
                return NOT_MOVING;
            }
            return null;
        }
        if (!isSprintingPossible(player, player.getAbilities().flying)) {
            return sprintingPossibleReason(player, player.getAbilities().flying);
        }
        if (!player.input.hasForwardImpulse()) {
            return NOT_MOVING;
        }
        if (HIT_WALL.blocks.test(player)) {
            return HIT_WALL;
        }
        return null;
    }

    public static SprintBlocker whyNotSprinting(LocalPlayer player) {
        if (player.isSprinting()) {
            return null;
        }
        SprintBlocker start = blocking(player);
        return start != null ? start : stopReason(player);
    }

    private static boolean shouldStopRunSprinting(LocalPlayer player) {
        return !isSprintingPossible(player, player.getAbilities().flying)
                || !player.input.hasForwardImpulse()
                || player.horizontalCollision && !player.minorHorizontalCollision;
    }

    private static boolean shouldStopSwimSprinting(LocalPlayer player) {
        return !isSprintingPossible(player, true)
                || !player.isInWater()
                || !player.input.hasForwardImpulse() && !player.onGround() && !player.isShiftKeyDown();
    }

    private static boolean isSprintingPossible(LocalPlayer player, boolean allowedInShallowWater) {
        return !player.isMobilityRestricted()
                && (player.isPassenger()
                ? vehicleCanSprint(player.getVehicle())
                : hasEnoughFoodToDoExhaustiveManoeuvres(player))
                && (allowedInShallowWater || !player.isInShallowWater());
    }

    private static SprintBlocker sprintingPossibleReason(LocalPlayer player, boolean allowedInShallowWater) {
        if (player.isMobilityRestricted()) {
            return RESTRICTED;
        }
        if (player.isPassenger() ? !vehicleCanSprint(player.getVehicle()) : !hasEnoughFoodToDoExhaustiveManoeuvres(player)) {
            return player.isPassenger() ? IN_VEHICLE : TOO_HUNGRY;
        }
        if (!allowedInShallowWater && player.isInShallowWater()) {
            return SHALLOW_WATER;
        }
        return null;
    }

    private static boolean vehicleCanSprint(Entity vehicle) {
        return vehicle != null && vehicle.canSprint() && vehicle.isLocalInstanceAuthoritative();
    }

    private static boolean hasEnoughFoodToDoExhaustiveManoeuvres(LocalPlayer player) {
        return player.getFoodData().hasEnoughFood() || player.getAbilities().mayfly;
    }

    private static boolean isStandingStill(LocalPlayer player) {
        return !player.input.hasForwardImpulse();
    }

    private static boolean ridesNonSprintingVehicle(LocalPlayer player) {
        return player.isPassenger() && !vehicleCanSprint(player.getVehicle());
    }

    private static boolean lacksSprintFood(LocalPlayer player) {
        return !player.isPassenger() && !hasEnoughFoodToDoExhaustiveManoeuvres(player);
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

    private static boolean hitWall(LocalPlayer player) {
        return player.horizontalCollision && !player.minorHorizontalCollision;
    }
}
