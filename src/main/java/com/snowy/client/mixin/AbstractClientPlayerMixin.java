package com.snowy.client.mixin;

import com.snowy.client.CosmeticModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cape: puts the Snowy's cape on your own player (only you see it). */
@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void snowys$cape(CallbackInfoReturnable<PlayerSkin> cir) {
        if ((Object) this != Minecraft.getInstance().player) return;
        ClientAsset.Texture cape = CosmeticModules.capeAsset();
        if (cape == null) return;
        PlayerSkin s = cir.getReturnValue();
        cir.setReturnValue(new PlayerSkin(s.body(), cape, s.elytra(), s.model(), s.secure()));
    }
}
