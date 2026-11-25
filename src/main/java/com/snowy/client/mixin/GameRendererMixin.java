package com.snowy.client.mixin;

import com.snowy.client.TweakModules;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Camera Tweaks > View bobbing: scales how much the camera bobs while walking. */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    // bobView's float locals: walk distance (0), bob (1)
    @ModifyVariable(method = "bobView", at = @At("STORE"), ordinal = 1)
    private float snowys$scaleBob(float bob) {
        return bob * TweakModules.bobFactor();
    }
}
