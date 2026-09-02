package dev.mitra.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MitrasAutoSprinterClient implements ClientModInitializer {

    private static final String MOD_ID = "mitrasautosprinter";
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "sprint");

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int MAX_CONSECUTIVE_ERRORS = 5;
    private static final int MAX_HUD_ERRORS = 10;
    private static final int CONFLICT_WARN_TICKS = 100;

    private static final Component HUD_TEXT = Component.literal("Sprint ON");
    private static final int HUD_COLOR = 0xFF55FF55;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.mitrasautosprinter.toggle", InputConstants.Type.KEYSYM, InputConstants.KEY_K, CATEGORY));

    private static boolean enabled;
    private static int consecutiveErrors;
    private static boolean autoDisabled;
    private static boolean hudDisabled;
    private static int hudErrors;

    private static boolean sprintSetLastTick;
    private static int externallyClearedStreak;
    private static boolean conflictWarned;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(MitrasAutoSprinterClient::onEndClientTick);

        try {
            HudElementRegistry.attachElementAfter(
                    VanillaHudElements.MISC_OVERLAYS, HUD_ID, MitrasAutoSprinterClient::renderHud);
        } catch (Throwable t) {
            hudDisabled = true;
            LOGGER.warn("[{}] HUD layer could not be attached (another mod may have modified the " + "vanilla HUD). Auto-sprint still works; the ON/OFF indicator is disabled.", MOD_ID, t);
        }

        LOGGER.info("[{}] Initialized. Toggle key: K (rebindable in Controls).", MOD_ID);
    }

    private static void onEndClientTick(Minecraft client) {
        if (autoDisabled) return;

        try {
            if (client == null) return;

            while (TOGGLE_KEY.consumeClick()) {
                enabled = !enabled;
            }

            if (enabled) {
                applySprint(client);
            }

            consecutiveErrors = 0;

        } catch (Throwable t) {
            consecutiveErrors++;
            LOGGER.error("[{}] Runtime error in tick handler ({}/{}): {}",
                    MOD_ID, consecutiveErrors, MAX_CONSECUTIVE_ERRORS, t.getMessage(), t);

            if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                autoDisabled = true;
                enabled = false;
                LOGGER.error("[{}] AUTO-DISABLED after {} consecutive errors to protect game " + "stability. The sprint toggle key is now inert until restart. "
                                + "Please report this with the full log.", MOD_ID, consecutiveErrors);
            }
        }
    }

    private static void applySprint(Minecraft client) {
        LocalPlayer player = client.player;

        boolean wasSetLastTick = sprintSetLastTick;
        sprintSetLastTick = false;

        if (player == null) {
            externallyClearedStreak = 0;
            return;
        }

        if (wasSetLastTick && !player.isSprinting() && !player.horizontalCollision) {
            externallyClearedStreak++;
            if (externallyClearedStreak >= CONFLICT_WARN_TICKS && !conflictWarned) {
                conflictWarned = true;
                LOGGER.warn("[{}] Sprint has been repeatedly cleared by something other than " + "vanilla collision for {} ticks. This usually means another "
                                + "sprint-related mod is also managing sprint state (you may see FOV " + "flicker), or rapid vanilla sprint cancels such as combat. This mod "
                                + "only reports this once and never changes its own behavior.", MOD_ID, externallyClearedStreak);
            }
        } else if (player.isSprinting()) {
            externallyClearedStreak = 0;
        }

        if (!player.isAlive() || player.isRemoved()) return;
        if (player.isSprinting()) return;
        if (client.gui.screen() != null) return;
        if (player.isSpectator()) return;
        if (!client.options.keyUp.isDown()) return;
        if (player.isShiftKeyDown()) return;
        if (player.isUsingItem()) return;
        if (player.isFallFlying()) return;
        if (player.getFoodData().getFoodLevel() <= 6) return;
        if (player.hasEffect(MobEffects.BLINDNESS)) return;

        player.setSprinting(true);
        sprintSetLastTick = true;
    }

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (hudDisabled || !enabled || autoDisabled) return;

        try {
            if (graphics == null) return;
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) return;

            int textWidth = client.font.width(HUD_TEXT);
            if (textWidth <= 0) return;

            int x = graphics.guiWidth() - textWidth - 6;
            int y = 6;

            graphics.fill(x - 3, y - 3, x + textWidth + 3, y + client.font.lineHeight + 3, 0x66000000);
            graphics.text(client.font, HUD_TEXT, x, y, HUD_COLOR, true);

            hudErrors = 0;

        } catch (Throwable t) {
            hudErrors++;
            if (hudErrors >= MAX_HUD_ERRORS) {
                hudDisabled = true;
                LOGGER.error("[{}] HUD rendering failed {} times — indicator disabled. " + "The sprint toggle itself is unaffected.", MOD_ID, hudErrors, t);
            } else {
                LOGGER.debug("[{}] Transient HUD render error ({}/{}).", MOD_ID, hudErrors, MAX_HUD_ERRORS, t);
            }
        }
    }
}
