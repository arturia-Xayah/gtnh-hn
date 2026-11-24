package dev.aeoptimizer.mixin.client;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "codechicken.nei.search.ModNameFilter", remap = false)
public abstract class MachineOwnerNEIMixin {

    @Inject(method = "getModId", at = @At("HEAD"), cancellable = true)
    private static void hn$owner(ItemStack stack, CallbackInfoReturnable<String> cir) {
        if (dev.structureterminal.stocking.MixedInputs.owns(stack))
            cir.setReturnValue(dev.structureterminal.StructureTerminal.ID);
    }
}
