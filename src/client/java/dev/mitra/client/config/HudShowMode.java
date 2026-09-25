package dev.mitra.client.config;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jspecify.annotations.NonNull;

public enum HudShowMode implements EnumTranslatable {

    ALWAYS,
    BLOCKED_ONLY,
    ON_CHANGE;

    @NonNull
    @Override
    public String prefix() {
        return "mitrasautosprinter.mitrasautosprinter.hud.showMode";
    }
}
