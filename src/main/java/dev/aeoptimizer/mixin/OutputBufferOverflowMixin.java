package dev.aeoptimizer.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.storage.data.IAEStack;
import appeng.me.helpers.AENetworkProxy;
import dev.aeoptimizer.dispatch.OutputOverflow;
import gregtech.common.tileentities.machines.outputme.base.MTEHatchOutputMEBase;

@Mixin(value = MTEHatchOutputMEBase.class, remap = false)
public abstract class OutputBufferOverflowMixin {

    @Shadow
    @Final
    private MTEHatchOutputMEBase.Environment<?> env;

    @Shadow
    public abstract AENetworkProxy getProxy();

    @Shadow
    public abstract boolean canStore(IAEStack<?> stack);

    @Shadow
    public abstract void addToCache(IAEStack<?> stack);

    @Shadow
    public abstract void updateLastInputTick();

    @Inject(method = "storePartial", at = @At("HEAD"), cancellable = true)
    private void modernization$inFlight(IAEStack<?> input, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (simulate || !OutputOverflow.enabled(getProxy()) || !canStore(input)) return;
        addToCache(input.copy());
        updateLastInputTick();
        env.dispatchMarkDirty();
        input.setStackSize(0);
        cir.setReturnValue(true);
    }
}
