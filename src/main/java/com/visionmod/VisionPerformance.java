package com.visionmod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Tunables for Vision Mod's own renderers. Does not change server TPS or the
 * vanilla entity/chunk renderer. All values are local, reversible and persisted.
 */
public final class VisionPerformance {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
        .resolve("visionmod/performance.properties");

    public static int oreRadius = 6;
    public static int checksPerTick = 80;
    public static int outlineLimit = 48;
    public static int cosmeticsRange = 12;
    public static String preset = "MAX_FPS";

    private VisionPerformance() {}

    public static void load() {
        if (!Files.isRegularFile(FILE)) {
            maxFps();
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            oreRadius = allowed(intValue(p, "oreRadius", 6), new int[] {4, 6, 8, 12, 16}, 6);
            checksPerTick = allowed(intValue(p, "checksPerTick", 80), new int[] {40, 80, 160, 320}, 80);
            outlineLimit = allowed(intValue(p, "outlineLimit", 48), new int[] {32, 48, 96, 160, 240}, 48);
            cosmeticsRange = allowed(intValue(p, "cosmeticsRange", 12), new int[] {12, 24, 48}, 12);
            String selected = p.getProperty("preset", "CUSTOM");
            preset = selected.matches("MAX_FPS|BALANCED|VISUAL|CUSTOM") ? selected : "CUSTOM";
            VisionModClient.oreHighlight = bool(p, "oreHighlight", false);
            VisionModClient.playerHearts = bool(p, "playerHearts", false);
            VisionModClient.cosmetics = bool(p, "cosmetics", false);
            VisionModClient.showOtherCosmetics = bool(p, "otherCosmetics", false);
            VisionModClient.particles = bool(p, "particleFilter", true);
            VisionModClient.hideAllParticles = bool(p, "hideAllParticles", true);
        } catch (Exception ex) {
            System.err.println("[VisionMod] Performance settings could not be loaded: " + ex);
            maxFps();
        }
    }

    private static int intValue(Properties p, String key, int fallback) {
        try { return Integer.parseInt(p.getProperty(key, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static int allowed(int v, int[] values, int fallback) {
        for (int value : values) if (value == v) return v;
        return fallback;
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        String value = p.getProperty(key);
        return "true".equalsIgnoreCase(value) ? true
            : "false".equalsIgnoreCase(value) ? false : fallback;
    }

    public static void maxFps() {
        preset = "MAX_FPS";
        oreRadius = 6;
        checksPerTick = 80;
        outlineLimit = 48;
        cosmeticsRange = 12;
        VisionModClient.oreHighlight = false;
        VisionModClient.playerHearts = false;
        VisionModClient.cosmetics = false;
        VisionModClient.showOtherCosmetics = false;
        VisionModClient.particles = true;
        VisionModClient.hideAllParticles = true;
        save();
    }

    public static void balanced() {
        preset = "BALANCED";
        oreRadius = 8;
        checksPerTick = 160;
        outlineLimit = 96;
        cosmeticsRange = 24;
        VisionModClient.oreHighlight = true;
        VisionModClient.playerHearts = false;
        VisionModClient.cosmetics = true;
        VisionModClient.showOtherCosmetics = false;
        VisionModClient.particles = true;
        VisionModClient.hideAllParticles = false;
        save();
    }

    public static void visual() {
        preset = "VISUAL";
        oreRadius = 16;
        checksPerTick = 320;
        outlineLimit = 240;
        cosmeticsRange = 48;
        VisionModClient.oreHighlight = true;
        VisionModClient.playerHearts = true;
        VisionModClient.cosmetics = true;
        VisionModClient.showOtherCosmetics = true;
        VisionModClient.particles = false;
        VisionModClient.hideAllParticles = false;
        save();
    }

    public static void custom() {
        preset = "CUSTOM";
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Properties p = new Properties();
            p.setProperty("preset", preset);
            p.setProperty("oreRadius", Integer.toString(oreRadius));
            p.setProperty("checksPerTick", Integer.toString(checksPerTick));
            p.setProperty("outlineLimit", Integer.toString(outlineLimit));
            p.setProperty("cosmeticsRange", Integer.toString(cosmeticsRange));
            p.setProperty("oreHighlight", Boolean.toString(VisionModClient.oreHighlight));
            p.setProperty("playerHearts", Boolean.toString(VisionModClient.playerHearts));
            p.setProperty("cosmetics", Boolean.toString(VisionModClient.cosmetics));
            p.setProperty("otherCosmetics", Boolean.toString(VisionModClient.showOtherCosmetics));
            p.setProperty("particleFilter", Boolean.toString(VisionModClient.particles));
            p.setProperty("hideAllParticles", Boolean.toString(VisionModClient.hideAllParticles));
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Vision Mod performance settings - safe to edit while Minecraft is closed");
            }
        } catch (Exception ex) {
            System.err.println("[VisionMod] Performance settings could not be saved: " + ex);
        }
    }
}
