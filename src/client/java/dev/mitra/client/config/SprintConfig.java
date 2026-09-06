package dev.mitra.client.config;

import dev.mitra.client.sprint.SprintBlocker;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SprintConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("mitrasautosprinter");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mitrasautosprinter.properties");

    private static final int MAX_TEXT_LENGTH = 64;
    private static final int MAX_POSITION = 10_000;
    public static final int AUTO_POSITION = -1;
    public static final int DEFAULT_HUD_Y = 38;
    public static final String DISPLAY_MODE_TEXT = "text";
    public static final String DISPLAY_MODE_ICON = "icon";
    public static final double MIN_ICON_SCALE = 0.25;
    public static final double MAX_ICON_SCALE = 8.0;
    private static final Pattern HEX_COLOR = Pattern.compile("#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

    public boolean sprintEnabled = false;

    public boolean hudVisible = true;
    public boolean hudBackground = false;
    public String displayMode = DISPLAY_MODE_TEXT;
    public double hudIconScale = 1.0;
    public int hudX = AUTO_POSITION;
    public int hudY = DEFAULT_HUD_Y;
    public int colorOn = 0xFF55FF55;
    public int colorBlocked = 0xFFFFFF55;
    public int colorOff = 0xFFAAAAAA;
    public int backgroundColor = 0x66000000;

    public String textOn = "Sprint ON";
    public String textOff = "Sprint OFF";
    public String textJoining = "Joining...";
    public String textTerrain = "Loading terrain...";
    public String textBlockedFormat = "Sprint OFF - %s";

    private final Map<SprintBlocker, String> reasonText = new EnumMap<>(SprintBlocker.class);
    private long lastSeenFileStamp;

    public SprintConfig() {
        for (SprintBlocker reason : SprintBlocker.values()) {
            reasonText.put(reason, reason.defaultText());
        }
        load();
        lastSeenFileStamp = fileStamp();
    }

    public String reasonText(SprintBlocker reason) {
        return reasonText.get(reason);
    }

    public boolean reloadIfChanged() {
        long stamp = fileStamp();
        if (stamp == lastSeenFileStamp || !Files.isRegularFile(FILE)) {
            return false;
        }
        Properties props = new Properties();
        try (var in = Files.newInputStream(FILE)) {
            props.load(in);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Config file is unreadable; keeping the current config", e);
            return false;
        }
        if (fileStamp() != stamp) {
            return false;
        }
        lastSeenFileStamp = stamp;
        applyParsed(props);
        return true;
    }

    public void save() {
        List<String> lines = new ArrayList<>();

        lines.add("#=====================================================");
        lines.add("# MitrasAutoSprinter Configuration");
        lines.add("# Last saved: " + new Date());
        lines.add("#=====================================================");

        addSection(lines, "SPRINT BEHAVIOR");
        addComment(lines, "Master toggle for auto-sprint functionality");
        addEntry(lines, "sprintEnabled", sprintEnabled);

        addSection(lines, "HUD DISPLAY");
        addComment(lines, "Show/hide the sprint status HUD overlay");
        addEntry(lines, "hudVisible", hudVisible);

        addComment(lines, "Show a background box behind the HUD text");
        addEntry(lines, "hudBackground", hudBackground);

        addComment(lines, "What the HUD shows: \"text\" (the label) or \"icon\" (the speed effect icon)");
        addEntry(lines, "displayMode", displayMode);

        addComment(lines, "Icon size multiplier, only used when displayMode is icon (0.25 - 8)");
        addEntry(lines, "hudIconScale", hudIconScale);

        addComment(lines,
                "Position of the HUD on screen, in GUI-scaled pixels.",
                "Set hudX or hudY to -1 to auto-center that axis at any",
                "resolution or GUI scale");
        addEntry(lines, "hudX", hudX);
        addEntry(lines, "hudY", hudY);

        addSection(lines, "HUD COLORS",
                "Format: ARGB hex, escaped as \\#AARRGGBB",
                "(AA = alpha/transparency, RR/GG/BB = red/green/blue)");
        addEntry(lines, "hudBackgroundColor", toHex(backgroundColor));
        addEntry(lines, "hudColorOn", toHex(colorOn));
        addEntry(lines, "hudColorOff", toHex(colorOff));
        addEntry(lines, "hudColorBlocked", toHex(colorBlocked));

        addSection(lines, "HUD TEXT");
        addEntry(lines, "textOn", textOn);
        addEntry(lines, "textOff", textOff);
        addEntry(lines, "textJoining", textJoining);
        addEntry(lines, "textTerrain", textTerrain);

        addComment(lines,
                "Shown when sprint is blocked. %s is replaced with the",
                "blocking reason (see REASON LABELS below)");
        addEntry(lines, "textBlockedFormat", textBlockedFormat);

        addSection(lines, "REASON LABELS",
                "Text shown in place of %s above, depending on why",
                "sprinting is currently blocked");
        Arrays.stream(SprintBlocker.values())
                .sorted(Comparator.comparing(SprintBlocker::key))
                .forEach(reason -> addEntry(lines, reason.key(), reasonText.get(reason)));

        try {
            Files.createDirectories(FILE.getParent());
            Files.write(FILE, lines, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            LOGGER.warn("Could not save the config to {}", FILE, e);
        }
    }

    private long fileStamp() {
        try {
            return Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException e) {
            return -1;
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

        applyParsed(props);
    }

    private void applyParsed(Properties props) {
        sprintEnabled = parseBoolean(props, "sprintEnabled", sprintEnabled);
        hudVisible = parseBoolean(props, "hudVisible", hudVisible);
        hudBackground = parseBoolean(props, "hudBackground", hudBackground);
        displayMode = parseDisplayMode(props, displayMode);
        hudIconScale = parseIconScale(props, hudIconScale);
        hudX = parsePosition(props, "hudX", hudX);
        hudY = parsePosition(props, "hudY", hudY);
        colorOn = parseColor(props, "hudColorOn", colorOn);
        colorBlocked = parseColor(props, "hudColorBlocked", colorBlocked);
        colorOff = parseColor(props, "hudColorOff", colorOff);
        backgroundColor = parseColor(props, "hudBackgroundColor", backgroundColor);
        textOn = parseText(props, "textOn", textOn);
        textOff = parseText(props, "textOff", textOff);
        textJoining = parseText(props, "textJoining", textJoining);
        textTerrain = parseText(props, "textTerrain", textTerrain);
        textBlockedFormat = parseFormat(props, textBlockedFormat);
        for (SprintBlocker reason : SprintBlocker.values()) {
            reasonText.put(reason, parseText(props, reason.key(), reason.defaultText()));
        }
    }

    private static void addSection(List<String> lines, String title, String... comments) {
        lines.add("");
        lines.add("#-----------------------------------------------------");
        lines.add("# " + title);
        for (String comment : comments) {
            lines.add("# " + comment);
        }
        lines.add("#-----------------------------------------------------");
    }

    private static void addComment(List<String> lines, String... commentLines) {
        for (String comment : commentLines) {
            lines.add("# " + comment);
        }
    }

    private static void addEntry(List<String> lines, String key, Object value) {
        lines.add(key + "=" + escapeValue(String.valueOf(value)));
    }

    private static String escapeValue(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            out.append(escaped(value.charAt(i)));
        }
        return out.toString();
    }

    private static String escaped(char c) {
        return switch (c) {
            case '\\' -> "\\\\";
            case '=' -> "\\=";
            case ':' -> "\\:";
            case '#' -> "\\#";
            case '!' -> "\\!";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> c >= 0x20 && c <= 0x7e ? Character.toString(c) : "\\u%04x".formatted((int) c);
        };
    }

    private static boolean parseBoolean(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        return value != null ? Boolean.parseBoolean(value.trim()) : fallback;
    }

    private static String parseDisplayMode(Properties props, String fallback) {
        String value = props.getProperty("displayMode");
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.equals(DISPLAY_MODE_TEXT) || trimmed.equals(DISPLAY_MODE_ICON) ? trimmed : fallback;
    }

    private static double parseIconScale(Properties props, double fallback) {
        String value = props.getProperty("hudIconScale");
        if (value == null) {
            return fallback;
        }
        try {
            return Math.clamp(Double.parseDouble(value.trim()), MIN_ICON_SCALE, MAX_ICON_SCALE);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parsePosition(Properties props, String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed == AUTO_POSITION ? AUTO_POSITION : Math.clamp(parsed, 0, MAX_POSITION);
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
