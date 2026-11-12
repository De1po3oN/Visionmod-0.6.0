package com.visionmod;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.*;
import java.io.*;
import java.util.Properties;

/** Independent enable switches per ore family, persisted across launches. */
public final class OreSelection {
    private static final boolean[] ENABLED = new boolean[10];
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("visionmod-ores.properties");
    static { java.util.Arrays.fill(ENABLED, true); load(); }
    private OreSelection() {}

    public static boolean enabled(int i) { return i >= 0 && i < ENABLED.length && ENABLED[i]; }
    public static void toggle(int i) { if(i < 0 || i >= ENABLED.length) return; ENABLED[i] = !ENABLED[i]; save(); }
    public static void setEnabled(int i, boolean on) { if (i < 0 || i >= ENABLED.length) return; ENABLED[i] = on; save(); }
    public static int index(Block b) {
        if(b==Blocks.DIAMOND_ORE || b==Blocks.DEEPSLATE_DIAMOND_ORE) return 0;
        if(b==Blocks.ANCIENT_DEBRIS) return 1;
        if(b==Blocks.IRON_ORE || b==Blocks.DEEPSLATE_IRON_ORE) return 2;
        if(b==Blocks.GOLD_ORE || b==Blocks.DEEPSLATE_GOLD_ORE || b==Blocks.NETHER_GOLD_ORE) return 3;
        if(b==Blocks.REDSTONE_ORE || b==Blocks.DEEPSLATE_REDSTONE_ORE) return 4;
        if(b==Blocks.EMERALD_ORE || b==Blocks.DEEPSLATE_EMERALD_ORE) return 5;
        if(b==Blocks.LAPIS_ORE || b==Blocks.DEEPSLATE_LAPIS_ORE) return 6;
        if(b==Blocks.COPPER_ORE || b==Blocks.DEEPSLATE_COPPER_ORE) return 7;
        if(b==Blocks.COAL_ORE || b==Blocks.DEEPSLATE_COAL_ORE) return 8;
        if(b==Blocks.NETHER_QUARTZ_ORE) return 9;
        return -1;
    }
    public static boolean enabled(Block b) { return enabled(index(b)); }
    private static void load() {
        if(!Files.exists(FILE)) return;
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(FILE)) { p.load(in); for(int i=0;i<ENABLED.length;i++) ENABLED[i]=Boolean.parseBoolean(p.getProperty("enabled."+i,"true")); }
        catch(IOException e){System.err.println("[VisionMod] Ore selection load: "+e);}
    }
    private static void save() {
        Properties p=new Properties();for(int i=0;i<ENABLED.length;i++)p.setProperty("enabled."+i,Boolean.toString(ENABLED[i]));
        try{Files.createDirectories(FILE.getParent());try(OutputStream out=Files.newOutputStream(FILE)){p.store(out,"Vision Mod ore selection");}}
        catch(IOException e){System.err.println("[VisionMod] Ore selection save: "+e);}
    }
}
