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
import gregtech.api.enums.OutputBusType;
import gregtech.api.interfaces.IOutputBus;
import gregtech.api.interfaces.IOutputBusTransaction;
import gregtech.api.util.ItemEjectionHelper;

@Mixin(value = ItemEjectionHelper.class, remap = false)
public abstract class ItemOutputRoutingMixin {

    @Shadow
    @Final
    private EnumMap<OutputBusType, List<IOutputBusTransaction>> transactionsByType;
    @Unique
    private Map<IOutputBusTransaction, IOutputBusTransaction> modernization$normal;

    @Inject(method = "<init>(Ljava/util/List;ZZ)V", at = @At("RETURN"))
    private void modernization$prepare(List<IOutputBus> outputs, boolean protect, boolean recipeCheck,
        CallbackInfo ci) {
        if (recipeCheck) return;
        for (List<IOutputBusTransaction> group : transactionsByType.values()) for (IOutputBusTransaction tx : group) {
            if (tx instanceof OverflowRoute && ((OverflowRoute) tx).modernization$overflowEnabled()) {
                if (modernization$normal == null) modernization$normal = new IdentityHashMap<>();
                modernization$normal.put(tx, OutputRouting.normal(tx));
            }
        }
    }

    @Redirect(
        method = "ejectItems",
        at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/Iterators;peekingIterator(Ljava/util/Iterator;)Lcom/google/common/collect/PeekingIterator;"))
    private PeekingIterator<IOutputBusTransaction> modernization$order(Iterator<IOutputBusTransaction> source) {
        return OutputRouting.order(source, modernization$normal);
    }
}
