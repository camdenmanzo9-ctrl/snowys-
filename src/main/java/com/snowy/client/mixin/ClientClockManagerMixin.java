package com.snowy.client.mixin;

import com.snowy.client.TweakModules;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Time & Weather > Time: shows your chosen time of day (the day count stays the same). */
@Mixin(ClientClockManager.class)
abstract class ClientClockManagerMixin {
    @Inject(method = "getTotalTicks", at = @At("RETURN"), cancellable = true)
    private void snowys$fixedTime(Holder<WorldClock> definition, CallbackInfoReturnable<Long> cir) {
        long t = TweakModules.fixedTime();
        if (t < 0) return;
        cir.setReturnValue(Math.floorDiv(cir.getReturnValue(), 24000L) * 24000L + t);
    }
}
