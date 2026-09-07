package dev.mitra.client.sprint;

import dev.mitra.client.config.MitrasConfig;
import dev.mitra.client.hud.SprintHud;
import net.minecraft.client.Minecraft;

public final class AutoSprint {

    private final MitrasConfig config;
    private final SprintHud hud;

    private boolean holdingSprintKey;

    public AutoSprint(MitrasConfig config, SprintHud hud) {
        this.config = config;
        this.hud = hud;
    }

    public void toggle() {
        config.sprint.sprintEnabled = !config.sprint.sprintEnabled;
        config.save();
    }

    public void startClientTick(Minecraft client) {
        holdSprintKey(client);
    }

    public void endClientTick(Minecraft client) {
        holdSprintKey(client);
        hud.update(client, config.sprint.sprintEnabled);
    }

    private void holdSprintKey(Minecraft client) {
        if (config.sprint.sprintEnabled) {
            client.options.keySprint.setDown(true);
            holdingSprintKey = true;
        } else if (holdingSprintKey) {
            client.options.keySprint.setDown(false);
            holdingSprintKey = false;
        }
    }
}
