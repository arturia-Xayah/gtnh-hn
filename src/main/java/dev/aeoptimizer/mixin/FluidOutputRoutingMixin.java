package dev.aeoptimizer.mixin;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.PeekingIterator;

import dev.aeoptimizer.dispatch.OutputRouting;
import dev.aeoptimizer.dispatch.OverflowRoute;
import gregtech.api.enums.OutputHatchType;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.util.FluidEjectionHelper;

@Mixin(value = FluidEjectionHelper.class, remap = false)
public abstract class FluidOutputRoutingMixin {

    @Shadow
    @Final
    private EnumMap<OutputHatchType, List<IOutputHatchTransaction>> transactionsByType;
    @Unique
    private Map<IOutputHatchTransaction, IOutputHatchTransaction> modernization$normal;

    @Inject(method = "<init>(Ljava/util/List;ZZ)V", at = @At("RETURN"))
    private void modernization$prepare(List<IOutputHatch> outputs, boolean protect, boolean recipeCheck,
        CallbackInfo ci) {
        if (recipeCheck) return;
        for (List<IOutputHatchTransaction> group : transactionsByType.values())
            for (IOutputHatchTransaction tx : group) {
                if (tx instanceof OverflowRoute && ((OverflowRoute) tx).modernization$overflowEnabled()) {
                    if (modernization$normal == null) modernization$normal = new IdentityHashMap<>();
                    modernization$normal.put(tx, OutputRouting.normal(tx));
                }
            }
    }

    @Redirect(
        method = "ejectFluids",
        at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/Iterators;peekingIterator(Ljava/util/Iterator;)Lcom/google/common/collect/PeekingIterator;"))
    private PeekingIterator<IOutputHatchTransaction> modernization$order(Iterator<IOutputHatchTransaction> source) {
        return OutputRouting.order(source, modernization$normal);
    }
}
