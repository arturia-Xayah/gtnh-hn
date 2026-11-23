package dev.aeoptimizer.mixin;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.structureterminal.output.MixedOutputBus;
import dev.structureterminal.output.OutputPlacement;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.util.HatchElementBuilder;
import gregtech.api.util.IGTHatchAdder;
import gregtech.common.blocks.ItemMachines;

@Mixin(value = HatchElementBuilder.class, remap = false)
public abstract class MixedOutputBuilderMixin {

    @Shadow
    private IGTHatchAdder<Object> mAdder;
    @Shadow
    private BiFunction<Object, ItemStack, Predicate<ItemStack>> mHatchItemFilter;
    @Unique
    private boolean modernization$dual, modernization$wrapped;

    @Inject(method = "adder", at = @At("HEAD"))
    private void modernization$reset(IGTHatchAdder<?> adder, CallbackInfoReturnable<?> cir) {
        modernization$dual = false;
        modernization$wrapped = false;
    }

    @Inject(method = "anyOf", at = @At("RETURN"))
    private void modernization$any(IHatchElement<?>[] elements, CallbackInfoReturnable<?> cir) {
        modernization$dual = modernization$both(java.util.Arrays.asList(elements));
    }

    @Inject(method = "atLeast(Ljava/util/Map;)Lgregtech/api/util/HatchElementBuilder;", at = @At("RETURN"))
    private void modernization$least(Map<IHatchElement<?>, ?> elements, CallbackInfoReturnable<?> cir) {
        modernization$dual = modernization$both(elements.keySet());
    }

    @Unique
    private static boolean modernization$both(java.util.Collection<?> elements) {
        // Unknown/custom adders fail closed: both native roles must be explicitly present in this one element.
        int roles = 0;
        for (Object element : elements) roles |= dev.structureterminal.output.OutputRoles.of(element);
        return roles == 3;
    }

    @Inject(method = "build", at = @At("HEAD"))
    private void modernization$guard(CallbackInfoReturnable<?> cir) {
        if (!modernization$wrapped && mAdder != null) {
            mAdder = OutputPlacement.guard(mAdder, modernization$dual);
            if (!modernization$dual && mHatchItemFilter != null) {
                BiFunction<Object, ItemStack, Predicate<ItemStack>> original = mHatchItemFilter;
                mHatchItemFilter = (controller, trigger) -> original.apply(controller, trigger)
                    .and(stack -> !(ItemMachines.getMetaTileEntity(stack) instanceof MixedOutputBus));
            }
            modernization$wrapped = true;
        }
    }
}
