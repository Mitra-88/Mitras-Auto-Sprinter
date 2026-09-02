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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MitrasAutoSprinterClient implements ClientModInitializer {

    private static final String MOD_ID = "mitrasautosprinter";
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "sprint");
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int COLOR_ON = 0xFF55FF55;
    private static final int COLOR_BLOCKED = 0xFFFFFF55;
    private static final int COLOR_OFF = 0xFFAAAAAA;
    private static final int HUD_BACKGROUND = 0x66000000;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.mitrasautosprinter.toggle", InputConstants.Type.KEYSYM, InputConstants.KEY_K, CATEGORY));

    private static boolean enabled = false;
    private static boolean wasEnabled = false;

    private static Component hudText = Component.translatable("hud.mitrasautosprinter.off");
    private static int hudColor = COLOR_OFF;
    private static int hudWidth = -1;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(MitrasAutoSprinterClient::onEndClientTick);

        try {
            HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, HUD_ID, MitrasAutoSprinterClient::renderHud);
        } catch (Throwable t) {
            LOGGER.warn("[{}] HUD layer could not be attached. Auto-sprint still works; " + "the status indicator is disabled.", MOD_ID, t);
        }

        LOGGER.info("[{}] Initialized. Toggle key: K (rebindable in Controls).", MOD_ID);
    }

    private static void onEndClientTick(Minecraft client) {
        if (client == null) return;

        while (TOGGLE_KEY.consumeClick()) {
            enabled = !enabled;
        }

        if (enabled) {
            client.options.keySprint.setDown(true);
        } else if (wasEnabled) {
            client.options.keySprint.setDown(false);
        }

        wasEnabled = enabled;
        updateHudStatus(client);
    }

    private static void updateHudStatus(Minecraft client) {
        LocalPlayer player = client.player;

        if (!enabled) {
            hudText = Component.translatable("hud.mitrasautosprinter.off");
            hudColor = COLOR_OFF;
        } else if (player == null) {
            hudText = Component.translatable("hud.mitrasautosprinter.on");
            hudColor = COLOR_ON;
        } else if (player.isSprinting()) {
            hudText = Component.translatable("hud.mitrasautosprinter.on");
            hudColor = COLOR_ON;
        } else {
            hudText = Component.translatable("hud.mitrasautosprinter.blocked",
                    getBlockedReason(client, player));
            hudColor = COLOR_BLOCKED;
        }

        hudWidth = client.font.width(hudText);
    }

    private static Component getBlockedReason(Minecraft client, LocalPlayer player) {
        if (!player.isAlive() || player.isRemoved()) {
            return Component.translatable("reason.mitrasautosprinter.dead");
        }
        if (player.isSpectator()) {
            return Component.translatable("reason.mitrasautosprinter.spectator");
        }
        if (player.isFallFlying() && !player.isUnderWater()) {
            return Component.translatable("reason.mitrasautosprinter.elytra");
        }
        if (player.isUsingItem() && !player.isUnderWater()) {
            return Component.translatable("reason.mitrasautosprinter.using_item");
        }
        if (player.isShiftKeyDown() && !player.isUnderWater()) {
            return Component.translatable("reason.mitrasautosprinter.sneaking");
        }
        if (player.isMovingSlowly() && !player.isUnderWater()) {
            return Component.translatable("reason.mitrasautosprinter.slow");
        }
        if (player.isPassenger()) {
            return Component.translatable("reason.mitrasautosprinter.vehicle");
        }
        if (player.getFoodData().getFoodLevel() <= 6) {
            return Component.translatable("reason.mitrasautosprinter.hungry");
        }
        if (!client.options.keyUp.isDown()) {
            return Component.translatable("reason.mitrasautosprinter.standing");
        }
        if (player.horizontalCollision) {
            return Component.translatable("reason.mitrasautosprinter.wall");
        }
        return Component.translatable("reason.mitrasautosprinter.waiting");
    }

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        try {
            if (graphics == null) return;
            Minecraft client = Minecraft.getInstance();

            Component text = hudText;
            if (text == null) return;

            int width = hudWidth;
            if (width <= 0) {
                width = client.font.width(text);
                hudWidth = width;
                if (width <= 0) return;
            }

            int x = graphics.guiWidth() - width - 6;
            int y = 6;
            graphics.fill(x - 3, y - 3, x + width + 3, y + client.font.lineHeight + 3, HUD_BACKGROUND);
            graphics.text(client.font, text, x, y, hudColor, true);
        } catch (Throwable t) {
            LOGGER.debug("[{}] HUD render error.", MOD_ID, t);
        }
    }
}