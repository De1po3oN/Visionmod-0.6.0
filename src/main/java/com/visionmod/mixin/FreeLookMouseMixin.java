package com.visionmod.mixin;

import com.visionmod.FreeLook;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class FreeLookMouseMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Inject(method="turnPlayer",at=@At("HEAD"),cancellable=true)
    private void visionmod$freeLook(CallbackInfo ci) {
        if(!FreeLook.active) return;
        FreeLook.rotate(accumulatedDX,accumulatedDY);
        accumulatedDX=0;
        accumulatedDY=0;
        ci.cancel();
    }
}
