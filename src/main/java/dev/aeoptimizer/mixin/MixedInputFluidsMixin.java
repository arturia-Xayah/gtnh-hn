package dev.aeoptimizer.mixin;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.structureterminal.stocking.MixedInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class MixedInputFluidsMixin {

    @Shadow
    public ArrayList<MTEHatchInputBus> mInputBusses;

    @Unique
    private void modernization$fluids(Map<Fluid, FluidStack> fluids, Optional<Byte> color) {
        for (MTEHatchInputBus bus : mInputBusses) {
            if (!(bus instanceof MixedInputBus) || !bus.isValid()) continue;
            if (color.isPresent() && bus.getColor() != -1 && bus.getColor() != color.get()) continue;
            for (FluidStack fluid : ((MixedInputBus) bus).fluidInputs())
                if (fluid != null && fluid.amount > 0) fluids.put(fluid.getFluid(), fluid);
        }
    }

    @Redirect(method = "getStoredFluidsForColor", at = @At(value = "INVOKE", target = "Ljava/util/Map;isEmpty()Z"))
    private boolean modernization$colored(Map<Fluid, FluidStack> fluids, Optional<Byte> color) {
        modernization$fluids(fluids, color);
        return fluids.isEmpty();
    }

    @Inject(method = "getStoredFluidsFromME", at = @At("RETURN"))
    private void modernization$all(CallbackInfoReturnable<Map<Fluid, FluidStack>> cir) {
        modernization$fluids(cir.getReturnValue(), Optional.empty());
    }

    @Inject(method = "depleteInputQuantity", at = @At("RETURN"), cancellable = true)
    private void modernization$drain(FluidStack request, boolean simulate, CallbackInfoReturnable<Integer> cir) {
        if (request == null) return;
        int drained = cir.getReturnValue();
        for (MTEHatchInputBus bus : mInputBusses) {
            if (drained >= request.amount) break;
            if (!(bus instanceof MixedInputBus) || !bus.isValid()) continue;
            FluidStack remaining = request.copy();
            remaining.amount -= drained;
            drained += ((MixedInputBus) bus).drainFluid(remaining, simulate);
        }
        cir.setReturnValue(drained);
    }
}
