package dev.mitra.client.sprint;

import dev.mitra.client.config.SprintConfig;
import dev.mitra.client.hud.SprintHud;
import net.minecraft.client.Minecraft;

public final class AutoSprint {

    private static final int CONFIG_CHECK_INTERVAL = 20;

    private final SprintConfig config;
    private final SprintHud hud;

    private boolean enabled;
    private boolean holdingSprintKey;
    private int configCheckTimer;

    public AutoSprint(SprintConfig config, SprintHud hud) {
        this.config = config;
        this.hud = hud;
        this.enabled = config.sprintEnabled;
    }

    public void toggle() {
        enabled = !enabled;
        config.sprintEnabled = enabled;
        config.save();
    }

    public void startClientTick(Minecraft client) {
        holdSprintKey(client);
    }

    public void endClientTick(Minecraft client) {
        if (++configCheckTimer >= CONFIG_CHECK_INTERVAL) {
            configCheckTimer = 0;
            if (config.reloadIfChanged()) {
                hud.refreshLabels();
                enabled = config.sprintEnabled;
            }
        }
        holdSprintKey(client);
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
