package dev.mitra.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SprintConfig {

    private static final String CONFIG_FILE = "config/mitrasautosprinter.properties";

    private static final int MAX_TEXT_LENGTH = 64;
    private static final int MAX_POSITION = 10000;

    private final Path file;

    public boolean sprintEnabled = false;

    public boolean hudVisible = true;
    public boolean hudBackground = true;
    public int hudX = 200;
    public int hudY = 6;

    public String hudColorOn = "#FF55FF55";
    public String hudColorBlocked = "#FFFFFF55";
    public String hudColorOff = "#FFAAAAAA";
    public String hudBackgroundColor = "#66000000";

    public String textOn = "Sprint ON";
    public String textOff = "Sprint OFF";
    public String textBlockedFormat = "Sprint OFF - %s";

    public String reasonDead = "Dead";
    public String reasonSpectator = "Spectating";
    public String reasonBlind = "Blindness";
    public String reasonElytra = "Flying";
    public String reasonUsingItem = "Using Item";
    public String reasonSneaking = "Sneaking";
    public String reasonSlow = "Crawling";
    public String reasonVehicle = "In Vehicle";
    public String reasonHungry = "Too Hungry";
    public String reasonStanding = "Not Moving";
    public String reasonShallowWater = "Shallow Water";
    public String reasonWall = "Hit Wall";
    public String reasonWaiting = "Starting...";

    public SprintConfig() {
        this.file = Path.of(CONFIG_FILE);
        load();
    }

    public void save() {
        Properties props = new Properties();

        props.setProperty("sprintEnabled", String.valueOf(sprintEnabled));
        props.setProperty("hudVisible", String.valueOf(hudVisible));
        props.setProperty("hudBackground", String.valueOf(hudBackground));
        props.setProperty("hudX", String.valueOf(hudX));
        props.setProperty("hudY", String.valueOf(hudY));
        props.setProperty("hudColorOn", hudColorOn);
        props.setProperty("hudColorBlocked", hudColorBlocked);
        props.setProperty("hudColorOff", hudColorOff);
        props.setProperty("hudBackgroundColor", hudBackgroundColor);
        props.setProperty("textOn", textOn);
        props.setProperty("textOff", textOff);
        props.setProperty("textBlockedFormat", textBlockedFormat);

        props.setProperty("reasonDead", reasonDead);
        props.setProperty("reasonSpectator", reasonSpectator);
        props.setProperty("reasonBlind", reasonBlind);
        props.setProperty("reasonElytra", reasonElytra);
        props.setProperty("reasonUsingItem", reasonUsingItem);
        props.setProperty("reasonSneaking", reasonSneaking);
        props.setProperty("reasonSlow", reasonSlow);
        props.setProperty("reasonVehicle", reasonVehicle);
        props.setProperty("reasonHungry", reasonHungry);
        props.setProperty("reasonStanding", reasonStanding);
        props.setProperty("reasonShallowWater", reasonShallowWater);
        props.setProperty("reasonWall", reasonWall);
        props.setProperty("reasonWaiting", reasonWaiting);

        try {
            Files.createDirectories(this.file.getParent());
            props.store(Files.newOutputStream(this.file), "MitrasAutoSprinter config");
        } catch (IOException e) {
        }
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }

        Properties props = new Properties();
        try {
            props.load(Files.newInputStream(this.file));
        } catch (IOException | RuntimeException e) {
            return;
        }

        sprintEnabled = parseBoolean(props, "sprintEnabled", sprintEnabled);
        hudVisible = parseBoolean(props, "hudVisible", hudVisible);
        hudBackground = parseBoolean(props, "hudBackground", hudBackground);
        hudX = parseClampedInt(props, "hudX", hudX);
        hudY = parseClampedInt(props, "hudY", hudY);

        hudColorOn = parseColor(props, "hudColorOn", hudColorOn);
        hudColorBlocked = parseColor(props, "hudColorBlocked", hudColorBlocked);
        hudColorOff = parseColor(props, "hudColorOff", hudColorOff);
        hudBackgroundColor = parseColor(props, "hudBackgroundColor", hudBackgroundColor);

        textOn = parseText(props, "textOn", textOn);
        textOff = parseText(props, "textOff", textOff);
        textBlockedFormat = parseFormat(props, textBlockedFormat);

        reasonDead = parseText(props, "reasonDead", reasonDead);
        reasonSpectator = parseText(props, "reasonSpectator", reasonSpectator);
        reasonBlind = parseText(props, "reasonBlind", reasonBlind);
        reasonElytra = parseText(props, "reasonElytra", reasonElytra);
        reasonUsingItem = parseText(props, "reasonUsingItem", reasonUsingItem);
        reasonSneaking = parseText(props, "reasonSneaking", reasonSneaking);
        reasonSlow = parseText(props, "reasonSlow", reasonSlow);
        reasonVehicle = parseText(props, "reasonVehicle", reasonVehicle);
        reasonHungry = parseText(props, "reasonHungry", reasonHungry);
        reasonStanding = parseText(props, "reasonStanding", reasonStanding);
        reasonShallowWater = parseText(props, "reasonShallowWater", reasonShallowWater);
        reasonWall = parseText(props, "reasonWall", reasonWall);
        reasonWaiting = parseText(props, "reasonWaiting", reasonWaiting);
    }

    private static boolean parseBoolean(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        return Boolean.parseBoolean(value.trim());
    }

    private static int parseClampedInt(Properties props, String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        try {
            return Math.clamp(Integer.parseInt(value.trim()), 0, SprintConfig.MAX_POSITION);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String parseText(Properties props, String key, String fallback) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return fallback;
        return trimmed.length() > MAX_TEXT_LENGTH ? trimmed.substring(0, MAX_TEXT_LENGTH) : trimmed;
    }

    private static String parseFormat(Properties props, String fallback) {
        String value = parseText(props, "textBlockedFormat", fallback);
        if (!value.contains("%s")) return fallback;
        return value;
    }

    private static String parseColor(Properties props, String key, String fallback) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.matches("#[0-9a-fA-F]{8}") ? trimmed : fallback;
    }
}