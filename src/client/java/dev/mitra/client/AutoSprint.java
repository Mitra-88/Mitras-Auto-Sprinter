package dev.mitra.client;

import net.minecraft.client.Minecraft;

final class AutoSprint {

    private final SprintConfig config;
    private final SprintHud hud;

    private boolean enabled;
    private boolean holdingSprintKey;
    private int configCheckTimer;

    private static final int CONFIG_CHECK_INTERVAL = 20;

    AutoSprint(SprintConfig config, SprintHud hud) {
        this.config = config;
        this.hud = hud;
        this.enabled = config.sprintEnabled;
    }

    void toggle() {
        enabled = !enabled;
        config.sprintEnabled = enabled;
        config.save();
    }

    void startClientTick(Minecraft client) {
        holdSprintKey(client);
    }

    void endClientTick(Minecraft client) {
        holdSprintKey(client);
        if (++configCheckTimer >= CONFIG_CHECK_INTERVAL) {
            configCheckTimer = 0;
            if (config.reloadLabelsIfChanged()) {
                hud.refreshLabels();
            }
        }
        hud.update(client, enabled);
    }

    private void holdSprintKey(Minecraft client) {
        if (enabled) {
            client.options.keySprint.setDown(true);
            holdingSprintKey = true;
        } else if (holdingSprintKey) {
            client.options.keySprint.setDown(false);
            holdingSprintKey = false;
        }
    }
}