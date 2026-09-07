package dev.mitra.client.config;

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable;
import org.jspecify.annotations.NonNull;

public enum HudAnchor implements EnumTranslatable {

    AUTO_CENTER_TOP,
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    CUSTOM;

    @NonNull
    @Override
    public String prefix() {
        return "mitrasautosprinter.mitrasautosprinter.hud.hudAnchor";
    }
}
