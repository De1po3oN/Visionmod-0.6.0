package com.visionmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.Desktop;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Local, shareable JSON profiles. Never executes imported data. */
public final class VisionProfiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9_-]{1,40}");
    private static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("visionmod/profiles");

    private VisionProfiles() {}

    public static Path directory() { return DIRECTORY; }

    private static Path path(String name) {
        if (name == null || !VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Use 1-40 English letters, digits, _ or -");
        }
        return DIRECTORY.resolve(name + ".json");
    }

    public static List<String> list() {
        List<String> names = new ArrayList<>();
        try {
            Files.createDirectories(DIRECTORY);
            try (DirectoryStream<Path> files = Files.newDirectoryStream(DIRECTORY, "*.json")) {
                for (Path file : files) {
                    if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)) continue;
                    String name = file.getFileName().toString();
                    name = name.substring(0, name.length() - 5);
                    if (VALID_NAME.matcher(name).matches()) names.add(name);
                }
            }
        } catch (Exception ignored) { }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static void save(String name) throws Exception {
        Files.createDirectories(DIRECTORY);
        Path target = path(name);
        if (Files.isSymbolicLink(target)) throw new IllegalArgumentException("Symlinks are not supported");
        JsonObject data = new JsonObject();
        data.addProperty("format", 1);
        data.addProperty("fullBright", VisionModClient.fullBright);
        data.addProperty("oreHighlight", VisionModClient.oreHighlight);
        data.addProperty("clearWater", VisionModClient.clearWater);
        data.addProperty("clearLava", VisionModClient.clearLava);
        data.addProperty("playerHearts", VisionModClient.playerHearts);
        data.addProperty("particles", VisionModClient.particles);
        data.addProperty("hideBlockParticles", VisionModClient.hideBlockParticles);
        data.addProperty("hideSprintParticles", VisionModClient.hideSprintParticles);
        data.addProperty("hideEnderChestParticles", VisionModClient.hideEnderChestParticles);
        data.addProperty("hideAllParticles", VisionModClient.hideAllParticles);
        data.addProperty("freeLook", FreeLook.enabled);
        // Optional v0.7.0 settings; legacy v0.6.0 profile files stay loadable.
        data.addProperty("performanceRadius", VisionPerformance.oreRadius);
        data.addProperty("performanceBudget", VisionPerformance.checksPerTick);
        data.addProperty("performanceOutlines", VisionPerformance.outlineLimit);
        data.addProperty("performanceCosmeticRange", VisionPerformance.cosmeticsRange);
        data.addProperty("cosmetics", VisionModClient.cosmetics);
        data.addProperty("showOtherCosmetics", VisionModClient.showOtherCosmetics);
        com.google.gson.JsonArray colors = new com.google.gson.JsonArray();
        com.google.gson.JsonArray ores = new com.google.gson.JsonArray();
        for (int i = 0; i < OreColors.NAMES.length; i++) {
            colors.add(String.format("#%06X", OreColors.getColor(i) & 0xFFFFFF));
            ores.add(OreSelection.enabled(i));
        }
        data.add("oreColors", colors);
        data.add("oreEnabled", ores);
        String output = GSON.toJson(data);
        Path temporary = Files.createTempFile(DIRECTORY, "profile-", ".tmp");
        try {
            Files.writeString(temporary, output, StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    public static void load(String name) throws Exception {
        Path file = path(name);
        if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)) throw new IllegalArgumentException("Profile not found");
        if (Files.size(file) > 16384L) throw new IllegalArgumentException("Profile is too large");
        JsonObject data = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        if (!data.has("format") || data.get("format").getAsInt() != 1) throw new IllegalArgumentException("Unsupported profile format");
        // Validate every field before changing any mod settings.
        boolean[] switches = new boolean[] {
            flag(data, "fullBright"), flag(data, "oreHighlight"), flag(data, "clearWater"),
            flag(data, "clearLava"), flag(data, "playerHearts"), flag(data, "particles"),
            flag(data, "hideBlockParticles"), flag(data, "hideSprintParticles"),
            flag(data, "hideEnderChestParticles"), flag(data, "hideAllParticles"), flag(data, "freeLook")
        };
        var colors = data.getAsJsonArray("oreColors");
        var ores = data.getAsJsonArray("oreEnabled");
        if (colors == null || ores == null || colors.size() != OreColors.NAMES.length || ores.size() != OreColors.NAMES.length)
            throw new IllegalArgumentException("Ore settings are missing");
        int[] validatedColors = new int[colors.size()];
        boolean[] validatedOres = new boolean[ores.size()];
        for (int i = 0; i < colors.size(); i++) {
            String hex = colors.get(i).getAsString();
            if (!hex.matches("#[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Invalid ore color");
            validatedColors[i] = 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
            if (!ores.get(i).isJsonPrimitive() || !ores.get(i).getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("Invalid ore toggle");
            validatedOres[i] = ores.get(i).getAsBoolean();
        }
        // Accept optional 0.7.0 numeric fields only if they match UI-supported values.
        int radius = optionalChoice(data, "performanceRadius", VisionPerformance.oreRadius, 4, 6, 8, 12, 16);
        int budget = optionalChoice(data, "performanceBudget", VisionPerformance.checksPerTick, 40, 80, 160, 320);
        int outlines = optionalChoice(data, "performanceOutlines", VisionPerformance.outlineLimit, 32, 48, 96, 160, 240);
        int cosmeticRange = optionalChoice(data, "performanceCosmeticRange", VisionPerformance.cosmeticsRange, 12, 24, 48);
        boolean cosmeticEnabled = optionalFlag(data, "cosmetics", VisionModClient.cosmetics);
        boolean remoteEnabled = optionalFlag(data, "showOtherCosmetics", VisionModClient.showOtherCosmetics);
        VisionModClient.fullBright = switches[0];
        VisionModClient.oreHighlight = switches[1];
        VisionModClient.clearWater = switches[2];
        VisionModClient.clearLava = switches[3];
        VisionModClient.playerHearts = switches[4];
        VisionModClient.particles = switches[5];
        VisionModClient.hideBlockParticles = switches[6];
        VisionModClient.hideSprintParticles = switches[7];
        VisionModClient.hideEnderChestParticles = switches[8];
        VisionModClient.hideAllParticles = switches[9];
        FreeLook.enabled = switches[10];
        VisionPerformance.oreRadius = radius;
        VisionPerformance.checksPerTick = budget;
        VisionPerformance.outlineLimit = outlines;
        VisionPerformance.cosmeticsRange = cosmeticRange;
        VisionModClient.cosmetics = cosmeticEnabled;
        VisionModClient.showOtherCosmetics = remoteEnabled;
        VisionPerformance.custom();
        for (int i = 0; i < validatedColors.length; i++) {
            OreColors.setColor(i, validatedColors[i]);
            OreSelection.setEnabled(i, validatedOres[i]);
        }
    }

    private static boolean flag(JsonObject data, String key) {
        if (!data.has(key) || !data.get(key).isJsonPrimitive() || !data.getAsJsonPrimitive(key).isBoolean())
            throw new IllegalArgumentException("Invalid setting: " + key);
        return data.get(key).getAsBoolean();
    }

    private static int optionalChoice(JsonObject data, String key, int defaultValue, int... allowed) {
        if (!data.has(key)) return defaultValue;
        if (!data.get(key).isJsonPrimitive() || !data.get(key).getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Invalid performance setting: " + key);
        int actual = data.get(key).getAsInt();
        for (int value : allowed) if (actual == value) return actual;
        throw new IllegalArgumentException("Unsupported performance setting: " + key);
    }

    private static boolean optionalFlag(JsonObject data, String key, boolean defaultValue) {
        return data.has(key) ? flag(data, key) : defaultValue;
    }

    public static void delete(String name) throws Exception {
        Path target = path(name);
        if (Files.isSymbolicLink(target)) throw new IllegalArgumentException("Symlinks are not supported");
        Files.deleteIfExists(target);
    }

    public static boolean openFolder() {
        try {
            Files.createDirectories(DIRECTORY);
            if (!Desktop.isDesktopSupported()) return false;
            Desktop.getDesktop().open(DIRECTORY.toFile());
            return true;
        } catch (Exception ignored) { return false; }
    }
}
