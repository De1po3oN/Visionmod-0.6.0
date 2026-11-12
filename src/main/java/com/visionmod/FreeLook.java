package com.visionmod;

import net.minecraft.client.Minecraft;

/** Camera-only orientation while the configured key is held. */
public final class FreeLook {
    public static boolean enabled = true;
    public static boolean active;
    public static float yaw;
    public static float pitch;
    private FreeLook() {}
    public static void tick(boolean held) {
        Minecraft mc=Minecraft.getInstance();
        boolean next=enabled && held && mc.player!=null && mc.screen==null;
        if(next && !active) {yaw=mc.player.getYRot();pitch=mc.player.getXRot();}
        active=next;
    }
    public static void rotate(double dx,double dy) {
        yaw+=(float)(dx * 0.15d);
        pitch=Math.max(-90f,Math.min(90f,pitch+(float)(dy * 0.15d)));
    }
}
