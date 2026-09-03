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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.UseEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MitrasAutoSprinterClient implements ClientModInitializer {

    private static final String MOD_ID = "mitrasautosprinter";
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "sprint");

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int DEFAULT_COLOR_ON      = 0xFF55FF55;
    private static final int DEFAULT_COLOR_BLOCKED = 0xFFFFFF55;
    private static final int DEFAULT_COLOR_OFF     = 0xFFAAAAAA;
    private static final int DEFAULT_BACKGROUND    = 0x66000000;

    private static final int DEFAULT_HUD_X = 200;
    private static final int DEFAULT_HUD_Y = 6;

    private static final int HUD_PADDING = 3;

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

    private static final KeyMapping HUD_EDITOR_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.mitrasautosprinter.hud_editor",
                    InputConstants.Type.KEYSYM,
                    -1,
                    CATEGORY
            )
    );

    private static SprintConfig CONFIG;

    private static boolean enabled;
    private static boolean wasEnabled;

    private static Component hudText;
    private static int hudColor;
    private static int hudWidth = -1;

    private static Component textOn;
    private static Component textOff;

    private static int colorOn;
    private static int colorBlocked;
    private static int colorOff;
    private static int backgroundColor;
    private static boolean hudBackgroundOn;
    private static boolean hudVisible;

    private static Component reasonDead;
    private static Component reasonSpectator;
    private static Component reasonBlind;
    private static Component reasonElytra;
    private static Component reasonUsingItem;
    private static Component reasonSneaking;
    private static Component reasonSlow;
    private static Component reasonVehicle;
    private static Component reasonHungry;
    private static Component reasonStanding;
    private static Component reasonShallowWater;
    private static Component reasonWall;
    private static Component reasonWaiting;

    private static Component lastReason;
    private static Component blockedText;

    private static boolean hudRenderBroken;
    private static boolean configBroken;

    private static boolean hudEditorOpen;
    private static int editorPreviewX = DEFAULT_HUD_X;
    private static int editorPreviewY = DEFAULT_HUD_Y;

    @Override
    public void onInitializeClient() {
        loadConfig();

        ClientTickEvents.START_CLIENT_TICK.register(MitrasAutoSprinterClient::onStartClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(MitrasAutoSprinterClient::onEndClientTick);

        attachHudElement();

        LOGGER.info("[{}] Initialized. Toggle key: K (rebindable in Controls).", MOD_ID);
    }

    private static void loadConfig() {
        try {
            CONFIG = new SprintConfig();
            configBroken = false;
        } catch (Throwable t) {
            configBroken = true;
            LOGGER.warn("[{}] Config failed to load, using built-in defaults.", MOD_ID, t);
        }

        enabled = !configBroken && CONFIG.sprintEnabled;

        textOn = Component.literal(configBroken ? "Sprint ON" : CONFIG.textOn);
        textOff = Component.literal(configBroken ? "Sprint OFF" : CONFIG.textOff);

        colorOn = configBroken ? DEFAULT_COLOR_ON : parseColorSafe(CONFIG.hudColorOn, DEFAULT_COLOR_ON);
        colorBlocked = configBroken ? DEFAULT_COLOR_BLOCKED : parseColorSafe(CONFIG.hudColorBlocked, DEFAULT_COLOR_BLOCKED);
        colorOff = configBroken ? DEFAULT_COLOR_OFF : parseColorSafe(CONFIG.hudColorOff, DEFAULT_COLOR_OFF);
        backgroundColor = configBroken ? DEFAULT_BACKGROUND : parseColorSafe(CONFIG.hudBackgroundColor, DEFAULT_BACKGROUND);

        hudBackgroundOn = configBroken || CONFIG.hudBackground;
        hudVisible = configBroken || CONFIG.hudVisible;

        reasonDead = Component.literal(configBroken ? "Dead" : CONFIG.reasonDead);
        reasonSpectator = Component.literal(configBroken ? "Spectating" : CONFIG.reasonSpectator);
        reasonBlind = Component.literal(configBroken ? "Blindness" : CONFIG.reasonBlind);
        reasonElytra = Component.literal(configBroken ? "Flying" : CONFIG.reasonElytra);
        reasonUsingItem = Component.literal(configBroken ? "Using Item" : CONFIG.reasonUsingItem);
        reasonSneaking = Component.literal(configBroken ? "Sneaking" : CONFIG.reasonSneaking);
        reasonSlow = Component.literal(configBroken ? "Crawling" : CONFIG.reasonSlow);
        reasonVehicle = Component.literal(configBroken ? "In Vehicle" : CONFIG.reasonVehicle);
        reasonHungry = Component.literal(configBroken ? "Too Hungry" : CONFIG.reasonHungry);
        reasonStanding = Component.literal(configBroken ? "Not Moving" : CONFIG.reasonStanding);
        reasonShallowWater = Component.literal(configBroken ? "Shallow Water" : CONFIG.reasonShallowWater);
        reasonWall = Component.literal(configBroken ? "Hit Wall" : CONFIG.reasonWall);
        reasonWaiting = Component.literal(configBroken ? "Starting..." : CONFIG.reasonWaiting);

        hudText = textOff;
        hudColor = colorOff;
        hudWidth = -1;
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

    private static void onStartClientTick(Minecraft client) {
        if (client == null) return;

        assertSprintKeyState(client);
    }

    private static void onEndClientTick(Minecraft client) {
        if (client == null) return;

        handleToggleKeyPress(client);
        assertSprintKeyState(client);
        updateHudStatus(client);
    }

    private static void handleToggleKeyPress(Minecraft client) {
        while (TOGGLE_KEY.consumeClick()) {
            enabled = !enabled;
            if (!configBroken) {
                CONFIG.sprintEnabled = enabled;
                CONFIG.save();
            }
        }

        while (HUD_EDITOR_KEY.consumeClick()) {
            if (client.player != null && client.gui.screen() == null) {
                client.gui.setScreen(new HudEditorScreen(null));
            }
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
            text = textOff;
            color = colorOff;
        } else if (player == null) {
            text = textOn;
            color = colorOff;
        } else if (player.isSprinting()) {
            text = textOn;
            color = colorOn;
        } else {
            text = getBlockedText(player);
            color = colorBlocked;
        }

        if (text != hudText) {
            hudText = text;
            hudColor = color;
            hudWidth = -1;
        }
    }

    private static Component getBlockedText(LocalPlayer player) {
        Component reason = getBlockedReason(player);

        if (reason != lastReason) {
            lastReason = reason;
            String format = configBroken ? "Sprint OFF - %s" : CONFIG.textBlockedFormat;
            blockedText = Component.literal(String.format(format, reason.getString()));
        }

        return blockedText;
    }

    private static Component getBlockedReason(LocalPlayer player) {
        if (!player.isAlive() || player.isRemoved())    return reasonDead;
        if (player.isSpectator())                       return reasonSpectator;
        if (!player.input.hasForwardImpulse())          return reasonStanding;
        if (player.isMobilityRestricted())              return reasonBlind;

        if (player.isPassenger()) {
            Entity vehicle = player.getVehicle();
            if (vehicle == null || !vehicleCanSprint(vehicle)) return reasonVehicle;
        } else if (!hasEnoughFoodToDoExhaustiveManoeuvres(player)) {
            return reasonHungry;
        }

        if (isBlockedByShallowWater(player))            return reasonShallowWater;
        if (isSlowDueToUsingItem(player))               return reasonUsingItem;
        if (player.isFallFlying() && !player.isUnderWater()) return reasonElytra;

        if (player.isMovingSlowly() && !player.isUnderWater()) {
            return player.isCrouching() ? reasonSneaking : reasonSlow;
        }

        if (player.horizontalCollision)                 return reasonWall;

        return reasonWaiting;
    }

    private static boolean isSlowDueToUsingItem(LocalPlayer player) {
        return player.isUsingItem()
                && !player.getUseItem().getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).canSprint();
    }

    private static boolean hasEnoughFoodToDoExhaustiveManoeuvres(LocalPlayer player) {
        return player.getFoodData().hasEnoughFood() || player.getAbilities().mayfly;
    }

    private static boolean isBlockedByShallowWater(LocalPlayer player) {
        return !player.getAbilities().flying && player.isInShallowWater();
    }

    private static boolean vehicleCanSprint(Entity vehicle) {
        return vehicle.canSprint() && vehicle.isLocalInstanceAuthoritative();
    }

    private static int parseColorSafe(String hex, int fallback) {
        if (hex == null) return fallback;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            if (clean.length() != 8) return fallback;
            return (int) Long.parseLong(clean, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clampHudX(int rawX, int screenWidth, int hudWidth) {
        int maxX = Math.max(0, screenWidth - hudWidth);
        return Math.clamp(rawX, 0, maxX);
    }

    private static int clampHudY(int rawY, int screenHeight, int hudHeight) {
        int maxY = Math.max(0, screenHeight - hudHeight);
        return Math.clamp(rawY, 0, maxY);
    }

    private static int configHudX() {
        return configBroken ? DEFAULT_HUD_X : CONFIG.hudX;
    }

    private static int configHudY() {
        return configBroken ? DEFAULT_HUD_Y : CONFIG.hudY;
    }

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (hudRenderBroken || graphics == null) return;
        if (!hudVisible && !hudEditorOpen) return;

        try {
            Minecraft client = Minecraft.getInstance();

            Component text = hudText;
            if (text == null) return;

            int width = hudWidth;
            if (width <= 0) {
                width = client.font.width(text);
                hudWidth = width;
                if (width <= 0) return;
            }

            int screenWidth = graphics.guiWidth();
            int screenHeight = graphics.guiHeight();

            int x;
            int y;

            if (hudEditorOpen) {
                x = clampHudX(editorPreviewX, screenWidth, width);
                y = clampHudY(editorPreviewY, screenHeight, client.font.lineHeight);
            } else {
                x = clampHudX(configHudX(), screenWidth, width);
                y = clampHudY(configHudY(), screenHeight, client.font.lineHeight);
            }

            if (hudBackgroundOn) {
                graphics.fill(
                        x - HUD_PADDING,
                        y - HUD_PADDING,
                        x + width + HUD_PADDING,
                        y + client.font.lineHeight + HUD_PADDING,
                        backgroundColor
                );
            }

            graphics.text(client.font, text, x, y, hudColor, true);
        } catch (Throwable t) {
            hudRenderBroken = true;
            LOGGER.debug("[{}] HUD render failed once; disabled for this session.", MOD_ID, t);
        }
    }

    public static void beginHudEdit() {
        editorPreviewX = configHudX();
        editorPreviewY = configHudY();
        hudEditorOpen = true;
    }

    public static void setHudPreviewPosition(int rawX, int rawY, int screenWidth, int screenHeight) {
        Minecraft client = Minecraft.getInstance();

        int textWidth = 60;
        int textHeight = 9;

        if (hudText != null) {
            textWidth = Math.max(1, client.font.width(hudText));
        }

        editorPreviewX = clampHudX(rawX, screenWidth, textWidth);
        editorPreviewY = clampHudY(rawY, screenHeight, textHeight);
    }

    public static void clampHudPreviewToScreen(int screenWidth, int screenHeight) {
        setHudPreviewPosition(editorPreviewX, editorPreviewY, screenWidth, screenHeight);
    }

    public static void endHudEdit(boolean save) {
        if (!hudEditorOpen) return;

        hudEditorOpen = false;

        if (save && !configBroken) {
            CONFIG.hudX = editorPreviewX;
            CONFIG.hudY = editorPreviewY;
            CONFIG.save();
        }
    }

    public static int[] getHudBoundsForEditor() {
        return getHudBoundsAt(editorPreviewX, editorPreviewY);
    }

    public static int[] getHudBoundsAt(int x, int y) {
        Minecraft client = Minecraft.getInstance();

        Component text = hudText;
        if (text == null) return null;

        int width = client.font.width(text);
        if (width <= 0) return null;

        return new int[]{x, y, x + width, y + client.font.lineHeight};
    }

    public static void renderHudPreviewForEditor(GuiGraphicsExtractor graphics) {
        try {
            Minecraft client = Minecraft.getInstance();
            Component text = hudText;
            if (text == null) return;

            int width = client.font.width(text);
            if (width <= 0) return;

            int x = clampHudX(editorPreviewX, graphics.guiWidth(), width);
            int y = clampHudY(editorPreviewY, graphics.guiHeight(), client.font.lineHeight);

            if (hudBackgroundOn) {
                graphics.fill(
                        x - HUD_PADDING,
                        y - HUD_PADDING,
                        x + width + HUD_PADDING,
                        y + client.font.lineHeight + HUD_PADDING,
                        backgroundColor
                );
            }

            graphics.text(client.font, text, x, y, hudColor, true);
        } catch (Throwable ignored) {
        }
    }
}