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

    private static final int COLOR_ON       = 0xFF55FF55;
    private static final int COLOR_BLOCKED  = 0xFFFFFF55;
    private static final int COLOR_OFF      = 0xFFAAAAAA;
    private static final int HUD_BACKGROUND = 0x66000000;

    private static final int HUD_MARGIN_RIGHT = 6;
    private static final int HUD_MARGIN_TOP   = 6;
    private static final int HUD_PADDING      = 3;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.mitrasautosprinter.toggle",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_K,
                    CATEGORY
            )
    );

    private static final Component TEXT_ON  = Component.translatable("hud.mitrasautosprinter.on");
    private static final Component TEXT_OFF = Component.translatable("hud.mitrasautosprinter.off");

    private static final Component REASON_DEAD       = Component.translatable("reason.mitrasautosprinter.dead");
    private static final Component REASON_SPECTATOR  = Component.translatable("reason.mitrasautosprinter.spectator");
    private static final Component REASON_ELYTRA     = Component.translatable("reason.mitrasautosprinter.elytra");
    private static final Component REASON_USING_ITEM = Component.translatable("reason.mitrasautosprinter.using_item");
    private static final Component REASON_SNEAKING   = Component.translatable("reason.mitrasautosprinter.sneaking");
    private static final Component REASON_SLOW       = Component.translatable("reason.mitrasautosprinter.slow");
    private static final Component REASON_VEHICLE    = Component.translatable("reason.mitrasautosprinter.vehicle");
    private static final Component REASON_HUNGRY     = Component.translatable("reason.mitrasautosprinter.hungry");
    private static final Component REASON_STANDING   = Component.translatable("reason.mitrasautosprinter.standing");
    private static final Component REASON_WALL       = Component.translatable("reason.mitrasautosprinter.wall");
    private static final Component REASON_WAITING    = Component.translatable("reason.mitrasautosprinter.waiting");

    private static boolean enabled;

    private static boolean wasEnabled;

    private static Component hudText = TEXT_OFF;
    private static int hudColor = COLOR_OFF;
    private static int hudWidth = -1;

    private static Component lastReason;
    private static Component blockedText;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(MitrasAutoSprinterClient::onEndClientTick);

        attachHudElement();

        LOGGER.info("[{}] Initialized. Toggle key: K (rebindable in Controls).", MOD_ID);
    }

    private static void attachHudElement() {
        try {
            HudElementRegistry.attachElementAfter(
                    VanillaHudElements.MISC_OVERLAYS,
                    HUD_ID,
                    MitrasAutoSprinterClient::renderHud
            );
        } catch (Throwable t) {
            LOGGER.warn(
                    "[{}] HUD layer could not be attached (another mod may have modified the vanilla HUD). "
                            + "Auto-sprint still works; the status indicator is disabled.",
                    MOD_ID, t
            );
        }
    }

    private static void onEndClientTick(Minecraft client) {
        if (client == null) {
            return;
        }

        handleToggleKeyPress();

        assertSprintKeyState(client);

        updateHudStatus(client);
    }

    private static void handleToggleKeyPress() {
        while (TOGGLE_KEY.consumeClick()) {
            enabled = !enabled;
        }
    }

    private static void assertSprintKeyState(Minecraft client) {
        if (enabled) {
            client.options.keySprint.setDown(true);
        } else if (wasEnabled) {
            client.options.keySprint.setDown(false);
        }
        wasEnabled = enabled;
    }

    private static void updateHudStatus(Minecraft client) {
        LocalPlayer player = client.player;

        Component text;
        int color;

        if (!enabled) {
            text = TEXT_OFF;
            color = COLOR_OFF;
        } else if (player == null || player.isSprinting()) {
            text = TEXT_ON;
            color = COLOR_ON;
        } else {
            text = getBlockedText(client, player);
            color = COLOR_BLOCKED;
        }

        if (text != hudText) {
            hudText = text;
            hudColor = color;
            hudWidth = client.font.width(text);
        }
    }

    private static Component getBlockedText(Minecraft client, LocalPlayer player) {
        Component reason = getBlockedReason(client, player);

        if (reason != lastReason) {
            lastReason = reason;
            blockedText = Component.translatable("hud.mitrasautosprinter.blocked", reason);
        }

        return blockedText;
    }

    private static Component getBlockedReason(Minecraft client, LocalPlayer player) {
        if (!player.isAlive() || player.isRemoved())    return REASON_DEAD;
        if (player.isSpectator())                       return REASON_SPECTATOR;
        if (player.isFallFlying() && !player.isUnderWater())   return REASON_ELYTRA;

        if (player.isUsingItem() && !player.isUnderWater())    return REASON_USING_ITEM;
        if (player.isShiftKeyDown() && !player.isUnderWater()) return REASON_SNEAKING;
        if (player.isMovingSlowly() && !player.isUnderWater()) return REASON_SLOW;

        if (player.isPassenger())                       return REASON_VEHICLE;
        if (player.getFoodData().getFoodLevel() <= 6)   return REASON_HUNGRY;

        if (!client.options.keyUp.isDown())             return REASON_STANDING;
        if (player.horizontalCollision)                 return REASON_WALL;

        return REASON_WAITING;
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

            int x = graphics.guiWidth() - width - HUD_MARGIN_RIGHT;
            int y = HUD_MARGIN_TOP;

            graphics.fill(
                    x - HUD_PADDING,
                    y - HUD_PADDING,
                    x + width + HUD_PADDING,
                    y + client.font.lineHeight + HUD_PADDING,
                    HUD_BACKGROUND
            );

            graphics.text(
                    client.font,
                    text,
                    x,
                    y,
                    hudColor,
                    true
            );
        } catch (Throwable t) {
            LOGGER.debug("[{}] Transient HUD render error.", MOD_ID, t);
        }
    }
}