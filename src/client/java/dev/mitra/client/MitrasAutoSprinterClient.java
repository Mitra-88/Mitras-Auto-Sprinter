package dev.mitra.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MitrasAutoSprinterClient implements ClientModInitializer {

    private static final String MOD_ID = "mitrasautosprinter";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.mitrasautosprinter.toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            KEY_CATEGORY));

    private static final KeyMapping HUD_EDITOR_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.mitrasautosprinter.hud_editor",
            InputConstants.Type.KEYSYM,
            -1,
            KEY_CATEGORY));

    @Override
    public void onInitializeClient() {
        SprintConfig config = new SprintConfig();
        SprintHud hud = new SprintHud(config);
        AutoSprint sprint = new AutoSprint(config, hud);

        ClientTickEvents.START_CLIENT_TICK.register(sprint::startClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.consumeClick()) {
                sprint.toggle();
            }
            while (HUD_EDITOR_KEY.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(new HudEditorScreen(config, hud));
                }
            }
            sprint.endClientTick(client);
        });

        hud.attach();

        LOGGER.info("Initialized. Toggle auto sprint with K (rebindable in Controls).");
    }
}