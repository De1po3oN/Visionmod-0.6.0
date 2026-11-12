package com.visionmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class VisionModClient implements ClientModInitializer {
    public static boolean enabled=true;
    public static boolean fullBright=true,oreHighlight=true,clearWater=true,clearLava=true,playerHearts=true;
    public static boolean particles=false,cosmetics=true,showOtherCosmetics=true;
    public static boolean hideBlockParticles=true,hideSprintParticles=true,hideEnderChestParticles=true,hideAllParticles=false;
    public static boolean isFullBrightEnabled(){return enabled&&fullBright;}
    public static boolean isOreEnabled(){return enabled&&oreHighlight;}
    public static boolean isWaterEnabled(){return enabled&&clearWater;}
    public static boolean isLavaEnabled(){return enabled&&clearLava;}
    public static boolean isHeartsEnabled(){return enabled&&playerHearts;}
    public static boolean isParticlesEnabled(){return enabled&&particles;}
    @Override public void onInitializeClient(){
        VisionPerformance.load();
        OreHighlight.init();
        VisionCosmetics.init(); // Sync stays functional; the cosmetics menu entry is removed.
        KeyMapping.Category category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("visionmod","general"));
        KeyMapping toggleAll=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.visionmod.toggle",InputConstants.Type.KEYSYM,InputConstants.KEY_END,category));
        KeyMapping openMenu=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.visionmod.menu",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,category));
        KeyMapping freeLook=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.visionmod.freelook",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_LEFT_ALT,category));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            FreeLook.tick(enabled&&freeLook.isDown());
            while(toggleAll.consumeClick()){
                enabled=!enabled;
                if(client.player!=null)client.player.displayClientMessage(Component.literal(enabled?"Vision Mod: ON":"Vision Mod: OFF"),true);
            }
            while(openMenu.consumeClick())if(client.screen==null)client.setScreen(new VisionScreen());
        });
    }
}
