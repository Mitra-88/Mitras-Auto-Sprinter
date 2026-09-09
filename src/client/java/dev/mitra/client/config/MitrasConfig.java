package dev.mitra.client.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigAction;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedColor;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MitrasConfig extends Config {

    private static final String MOD_ID = "mitrasautosprinter";
    public static final double MIN_ICON_SCALE = 0.25;
    public static final double MAX_ICON_SCALE = 8.0;

    public static Runnable hudEditorAction = () -> {};

    public SprintSection sprint = new SprintSection();
    public HudSection hud = new HudSection();
    public TextSection text = new TextSection();
    public ReasonSection reasons = new ReasonSection();

    public MitrasConfig() {
        super(Identifier.fromNamespaceAndPath(MOD_ID, MOD_ID), "", MOD_ID);
    }

    public static MitrasConfig register() {
        return ConfigApiJava.registerAndLoadConfig(MitrasConfig::new, RegisterType.CLIENT);
    }

    public static class SprintSection extends ConfigSection {
        public boolean sprintEnabled = false;
    }

    public static class HudSection extends ConfigSection {

        private static final Component REQUIRES_TEXT_MODE =
                Component.translatable("mitrasautosprinter.config.condition.text_mode");
        private static final Component REQUIRES_ICON_MODE =
                Component.translatable("mitrasautosprinter.config.condition.icon_mode");
        private static final Component REQUIRES_CUSTOM_ANCHOR =
                Component.translatable("mitrasautosprinter.config.condition.custom_anchor");
        private static final Component REQUIRES_SOLID_COLOR =
                Component.translatable("mitrasautosprinter.config.condition.solid_color");

        private final ValidatedColor colorOnField = new ValidatedColor(0x55, 0xFF, 0x55, 0xFF).withFormattingColorPresets();
        private final ValidatedColor colorOffField = new ValidatedColor(0xAA, 0xAA, 0xAA, 0xFF).withFormattingColorPresets();
        private final ValidatedColor colorBlockedField = new ValidatedColor(0xFF, 0xFF, 0x55, 0xFF).withFormattingColorPresets();
        private final ValidatedDouble iconScaleField = new ValidatedDouble(1.0, MAX_ICON_SCALE, MIN_ICON_SCALE, ValidatedNumber.WidgetType.SLIDER);
        private final ValidatedDouble hudXField = new ValidatedDouble(0.01, 1.0, 0.0, ValidatedNumber.WidgetType.TEXTBOX);
        private final ValidatedDouble hudYField = new ValidatedDouble(0.01, 1.0, 0.0, ValidatedNumber.WidgetType.TEXTBOX);
        private final ValidatedEnum<TextColorMode> textColorModeEnum = new ValidatedEnum<>(TextColorMode.SOLID, ValidatedEnum.WidgetType.CYCLING);

        public boolean hudVisible = true;
        public boolean hudBackground = false;
        public ValidatedEnum<DisplayMode> displayMode = new ValidatedEnum<>(DisplayMode.TEXT, ValidatedEnum.WidgetType.CYCLING);
        public ValidatedEnum<HudShowMode> showMode = new ValidatedEnum<>(HudShowMode.ALWAYS, ValidatedEnum.WidgetType.CYCLING);
        public ValidatedEnum<HudAnchor> hudAnchor = new ValidatedEnum<>(HudAnchor.AUTO_CENTER_TOP, ValidatedEnum.WidgetType.SCROLLABLE);
        public boolean hudTextShadow = true;
        public ValidatedCondition<Double> hudIconScale = new ValidatedCondition<>(
                iconScaleField,
                new ValidatedDouble(1.0, MAX_ICON_SCALE, MIN_ICON_SCALE, ValidatedNumber.WidgetType.SLIDER))
                .withCondition(REQUIRES_ICON_MODE, () -> displayMode.get() == DisplayMode.ICON);
        public ValidatedCondition<Double> hudX = new ValidatedCondition<>(
                hudXField,
                new ValidatedDouble(0.01, 1.0, 0.0, ValidatedNumber.WidgetType.TEXTBOX))
                .withCondition(REQUIRES_CUSTOM_ANCHOR, () -> hudAnchor.get() == HudAnchor.CUSTOM);
        public ValidatedCondition<Double> hudY = new ValidatedCondition<>(
                hudYField,
                new ValidatedDouble(0.01, 1.0, 0.0, ValidatedNumber.WidgetType.TEXTBOX))
                .withCondition(REQUIRES_CUSTOM_ANCHOR, () -> hudAnchor.get() == HudAnchor.CUSTOM);
        public ValidatedCondition<TextColorMode> textColorMode = new ValidatedCondition<>(
                textColorModeEnum,
                new ValidatedEnum<>(TextColorMode.SOLID, ValidatedEnum.WidgetType.CYCLING))
                .withCondition(REQUIRES_TEXT_MODE, () -> displayMode.get() == DisplayMode.TEXT);
        public ValidatedCondition<ValidatedColor.ColorHolder> colorOn = new ValidatedCondition<>(
                colorOnField,
                new ValidatedColor(0x55, 0xFF, 0x55, 0xFF).withFormattingColorPresets())
                .withCondition(REQUIRES_TEXT_MODE, () -> displayMode.get() == DisplayMode.TEXT)
                .withCondition(REQUIRES_SOLID_COLOR, () -> textColorMode.get() == TextColorMode.SOLID);
        public ValidatedCondition<ValidatedColor.ColorHolder> colorOff = new ValidatedCondition<>(
                colorOffField,
                new ValidatedColor(0xAA, 0xAA, 0xAA, 0xFF).withFormattingColorPresets())
                .withCondition(REQUIRES_TEXT_MODE, () -> displayMode.get() == DisplayMode.TEXT)
                .withCondition(REQUIRES_SOLID_COLOR, () -> textColorMode.get() == TextColorMode.SOLID);
        public ValidatedCondition<ValidatedColor.ColorHolder> colorBlocked = new ValidatedCondition<>(
                colorBlockedField,
                new ValidatedColor(0xFF, 0xFF, 0x55, 0xFF).withFormattingColorPresets())
                .withCondition(REQUIRES_TEXT_MODE, () -> displayMode.get() == DisplayMode.TEXT)
                .withCondition(REQUIRES_SOLID_COLOR, () -> textColorMode.get() == TextColorMode.SOLID);

        public ConfigAction openHudEditorButton = new ConfigAction.Builder()
                .title(Component.translatable("key.mitrasautosprinter.hud_editor"))
                .desc(Component.translatable("mitrasautosprinter.mitrasautosprinter.hud.openHudEditor.desc"))
                .build(() -> MitrasConfig.hudEditorAction.run());

        public ValidatedColor backgroundColor = new ValidatedColor(0x00, 0x00, 0x00, 0x66).withDyeColorPresets();
    }

    public static class TextSection extends ConfigSection {
        public ValidatedString textOn = new ValidatedString("Sprint ON");
        public ValidatedString textOff = new ValidatedString("Sprint OFF");
        public ValidatedString textJoining = new ValidatedString("Joining...");
        public ValidatedString textTerrain = new ValidatedString("Loading terrain...");
        public ValidatedString textBlockedFormat = new ValidatedString("Sprint OFF - %s");
    }

    public static class ReasonSection extends ConfigSection {
        public ValidatedString reasonStanding = new ValidatedString("Not Moving");
        public ValidatedString reasonRestricted = new ValidatedString("Restricted");
        public ValidatedString reasonVehicle = new ValidatedString("In Vehicle");
        public ValidatedString reasonHungry = new ValidatedString("Too Hungry");
        public ValidatedString reasonShallowWater = new ValidatedString("Shallow Water");
        public ValidatedString reasonUsingItem = new ValidatedString("Using Item");
        public ValidatedString reasonElytra = new ValidatedString("Flying");
        public ValidatedString reasonSneaking = new ValidatedString("Sneaking");
        public ValidatedString reasonSlow = new ValidatedString("Crawling");
        public ValidatedString reasonWall = new ValidatedString("Hit Wall");
    }
}
