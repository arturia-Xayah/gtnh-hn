package dev.aeoptimizer.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.aeoptimizer.dispatch.OutputOverflow;
import dev.aeoptimizer.dispatch.OverflowRoute;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;
import gregtech.common.tileentities.machines.outputme.util.AECacheCounter;

@Mixin(
    targets = "gregtech.common.tileentities.machines.outputme.MTEHatchOutputME$MEOutputHatchTransaction",
    remap = false)
public abstract class OutputHatchOverflowMixin implements OverflowRoute {

    @Shadow
    @Final
    private MTEHatchOutputME this$0;
    @Shadow
    private boolean isRecipeCheck;

    @Shadow
    @Final
    private long availableSpace;
    @Shadow
    @Final
    private AECacheCounter<GTUtility.FluidId> cache;

    public boolean modernization$overflowEnabled() {
        return !isRecipeCheck && OutputOverflow.enabled(this$0.getProxy());
    }

    public boolean modernization$normalSpace() {
        return cache.getTotal() < availableSpace;
    }

    @Inject(method = "hasAvailableSpace", at = @At("HEAD"), cancellable = true)
    private void modernization$inFlight(CallbackInfoReturnable<Boolean> cir) {
        if (!isRecipeCheck && OutputOverflow.enabled(this$0.getProxy())) cir.setReturnValue(true);
    }

    // Excess can remain after protection is disabled. Strict checks must never "insert" a negative amount.
    @Redirect(method = "storePartial", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(JJ)J"))
    private long modernization$nonNegative(long a, long b) {
        long amount = Math.min(a, b);
        return amount < 0 && isRecipeCheck ? 0 : amount;
    }
}
