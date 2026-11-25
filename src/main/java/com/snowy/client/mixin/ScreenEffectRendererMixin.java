package com.snowy.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.snowy.client.TweakModules;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Camera Tweaks > Low fire: draws the on-fire overlay lower on the screen. */
@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
    @Unique private static boolean snowys$lowered;

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void snowys$lowerFire(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite sprite, CallbackInfo ci) {
        snowys$lowered = TweakModules.lowFire();
        if (snowys$lowered) {
            poseStack.pushPose();
            poseStack.translate(0.0F, -0.3F, 0.0F);
        }
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void snowys$restoreFire(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (snowys$lowered) poseStack.popPose();
        snowys$lowered = false;
    }
}
