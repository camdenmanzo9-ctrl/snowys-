package com.snowy.client.mixin;

import com.snowy.client.CosmeticModules;
import com.snowy.client.TweakModules;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Particle Changer: crit multiplier, hide explosions, hide other particles. Trails are never hidden. */
@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {
    @Unique private static boolean snowys$extra;

    @Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",
            at = @At("HEAD"), cancellable = true)
    private void snowys$crits(Entity entity, ParticleOptions particle, CallbackInfo ci) {
        if (!TweakModules.particlesOn() || snowys$extra || !snowys$isCrit(particle.getType())) return;
        int n = TweakModules.critMult.i();
        if (n == 0) {
            ci.cancel();
            return;
        }
        snowys$extra = true;
        try {
            for (int i = 1; i < n; i++) ((ParticleEngine) (Object) this).createTrackingEmitter(entity, particle);
        } finally {
            snowys$extra = false;
        }
    }

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void snowys$hide(ParticleOptions options, double x, double y, double z, double xa, double ya, double za,
                             CallbackInfoReturnable<Particle> cir) {
        if (!TweakModules.particlesOn() || CosmeticModules.spawningTrail) return;
        ParticleType<?> type = options.getType();
        if (snowys$isCrit(type)) return;   // handled by the crit multiplier
        boolean explosion = type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER;
        if (explosion ? !TweakModules.explosions.bool : !TweakModules.others.bool) cir.setReturnValue(null);
    }

    @Unique
    private static boolean snowys$isCrit(ParticleType<?> type) {
        return type == ParticleTypes.CRIT || type == ParticleTypes.ENCHANTED_HIT;
    }
}
