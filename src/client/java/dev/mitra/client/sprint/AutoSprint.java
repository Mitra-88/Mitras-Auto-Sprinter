package dev.mitra.client.sprint;

import dev.mitra.client.config.MitrasConfig;
import dev.mitra.client.hud.SprintHud;
import net.minecraft.client.KeyMapping;
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
        KeyMapping sprintKey = client.options.keySprint;
        if (config.sprint.sprintEnabled) {
            if (!sprintKey.isDown()) {
                sprintKey.setDown(true);
                holdingSprintKey = true;
            }
        } else if (holdingSprintKey) {
            if (client.options.toggleSprint().get()) {
                if (sprintKey.isDown()) {
                    sprintKey.setDown(true);
                }
            } else {
                sprintKey.setDown(false);
            }
            holdingSprintKey = false;
        }
    }
}
