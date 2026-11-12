package com.visionmod.mixin;

import com.visionmod.FreeLook;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FreeLookCameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Inject(method="setup",at=@At("TAIL"))
    private void visionmod$orient(Level area, Entity entity, boolean thirdPerson, boolean reverse, float partialTick, CallbackInfo ci) {
        if(FreeLook.active) setRotation(FreeLook.yaw,FreeLook.pitch);
    }
}
