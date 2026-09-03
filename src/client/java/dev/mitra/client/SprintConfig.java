package dev.mitra.client;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SprintConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("mitrasautosprinter");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mitrasautosprinter.properties");

    private static final int MAX_TEXT_LENGTH = 64;
    private static final int MAX_POSITION = 10_000;
    private static final Pattern HEX_COLOR = Pattern.compile("#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

    boolean sprintEnabled = false;

    boolean hudVisible = true;
    boolean hudBackground = true;
    int hudX = 200;
    int hudY = 6;
    int colorOn = 0xFF55FF55;
    int colorBlocked = 0xFFFFFF55;
    int colorOff = 0xFFAAAAAA;
    int backgroundColor = 0x66000000;

    String textOn = "Sprint ON";
    String textOff = "Sprint OFF";
    String textBlockedFormat = "Sprint OFF - %s";

    private final Map<SprintBlocker, String> reasonText = new EnumMap<>(SprintBlocker.class);

    SprintConfig() {
        for (SprintBlocker reason : SprintBlocker.values()) {
            reasonText.put(reason, reason.defaultText());
        }
        load();
    }

    String reasonText(SprintBlocker reason) {
        return reasonText.get(reason);
    }

    void save() {
        Properties props = new Properties();

        props.setProperty("sprintEnabled", Boolean.toString(sprintEnabled));
        props.setProperty("hudVisible", Boolean.toString(hudVisible));
        props.setProperty("hudBackground", Boolean.toString(hudBackground));
        props.setProperty("hudX", Integer.toString(hudX));
        props.setProperty("hudY", Integer.toString(hudY));
        props.setProperty("hudColorOn", toHex(colorOn));
        props.setProperty("hudColorBlocked", toHex(colorBlocked));
        props.setProperty("hudColorOff", toHex(colorOff));
        props.setProperty("hudBackgroundColor", toHex(backgroundColor));
        props.setProperty("textOn", textOn);
        props.setProperty("textOff", textOff);
        props.setProperty("textBlockedFormat", textBlockedFormat);
        for (SprintBlocker reason : SprintBlocker.values()) {
            props.setProperty(reason.key(), reasonText.get(reason));
        }

        try {
            Files.createDirectories(FILE.getParent());
            try (var out = Files.newOutputStream(FILE)) {
                props.store(out, "MitrasAutoSprinter config");
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save the config to {}", FILE, e);
        }
    }

    private void load() {
        if (!Files.isRegularFile(FILE)) {
            return;
        }

        Properties props = new Properties();
        try (var in = Files.newInputStream(FILE)) {
            props.load(in);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Config file is unreadable; using defaults", e);
            return;
        }

        sprintEnabled = parseBoolean(props, "sprintEnabled", sprintEnabled);

        hudVisible = parseBoolean(props, "hudVisible", hudVisible);
        hudBackground = parseBoolean(props, "hudBackground", hudBackground);
        hudX = parseInt(props, "hudX", hudX);
        hudY = parseInt(props, "hudY", hudY);
        colorOn = parseColor(props, "hudColorOn", colorOn);
        colorBlocked = parseColor(props, "hudColorBlocked", colorBlocked);
        colorOff = parseColor(props, "hudColorOff", colorOff);
        backgroundColor = parseColor(props, "hudBackgroundColor", backgroundColor);

        textOn = parseText(props, "textOn", textOn);
        textOff = parseText(props, "textOff", textOff);
        textBlockedFormat = parseFormat(props, textBlockedFormat);

        for (SprintBlocker reason : SprintBlocker.values()) {
            reasonText.put(reason, parseText(props, reason.key(), reason.defaultText()));
        }
    }

    private static boolean parseBoolean(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        return value != null ? Boolean.parseBoolean(value.trim()) : fallback;
    }

    private static int parseInt(Properties props, String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Math.clamp(Integer.parseInt(value.trim()), 0, MAX_POSITION);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseColor(Properties props, String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        Matcher matcher = HEX_COLOR.matcher(value.trim());
        if (!matcher.matches()) {
            return fallback;
        }
        String hex = matcher.group(1);
        return (int) Long.parseLong(hex.length() == 6 ? "FF" + hex : hex, 16);
    }

    private static String parseText(Properties props, String key, String fallback) {
        String value = props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        return trimmed.length() > MAX_TEXT_LENGTH ? trimmed.substring(0, MAX_TEXT_LENGTH) : trimmed;
    }

    private static String parseFormat(Properties props, String fallback) {
        String format = parseText(props, "textBlockedFormat", fallback);
        if (!format.contains("%s")) {
            return fallback;
        }
        try {
            String ignored = String.format(format, "x");
        } catch (IllegalFormatException e) {
            return fallback;
        }
        return format;
    }

    private static String toHex(int color) {
        return String.format("#%08X", color);
    }
}