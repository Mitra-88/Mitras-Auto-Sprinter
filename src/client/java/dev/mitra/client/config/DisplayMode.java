package dev.mitra.client.config;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jspecify.annotations.NonNull;

public enum DisplayMode implements EnumTranslatable {

    TEXT,
    ICON;

    @NonNull
    @Override
    public String prefix() {
        return "mitrasautosprinter.mitrasautosprinter.hud.displayMode";
    }
}
