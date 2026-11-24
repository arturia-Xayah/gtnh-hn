package dev.aeoptimizer.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.storage.data.IAEStackType;
import appeng.client.gui.implementations.GuiStorageBus;
import appeng.client.gui.slots.VirtualMEPhantomSlot;
import appeng.container.implementations.ContainerStorageBus;
import appeng.util.item.AEFluidStackType;
import appeng.util.item.AEItemStackType;
import dev.aeoptimizer.storage.DualStorageBus;

@Mixin(value = GuiStorageBus.class, remap = false)
public abstract class DualBusScreenMixin {

    @Shadow
    @Final
    private ContainerStorageBus containerStorageBus;

    @Inject(method = "acceptType", at = @At("HEAD"), cancellable = true)
    private void hn$acceptMixed(VirtualMEPhantomSlot slot, IAEStackType<?> type, int button,
        CallbackInfoReturnable<Boolean> result) {
        if (containerStorageBus.getUpgradeable() instanceof DualStorageBus)
            result.setReturnValue(type == AEItemStackType.ITEM_STACK_TYPE || type == AEFluidStackType.FLUID_STACK_TYPE);
    }
}
