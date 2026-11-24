package dev.aeoptimizer.mixin;

import java.util.Iterator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.config.ActionItems;
import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerStorageBus;
import appeng.tile.inventory.IAEStackInventory;
import dev.aeoptimizer.storage.DualStorageBus;

@Mixin(value = ContainerStorageBus.class, remap = false)
public abstract class DualBusPartitionMixin {

    @Unique
    private Iterator<IAEStack<?>> hn$partition;

    @Inject(method = "partition", at = @At("HEAD"), cancellable = true)
    private void hn$partitionBoth(boolean reset, CallbackInfo ci) {
        ContainerStorageBus screen = (ContainerStorageBus) (Object) this;
        if (!(screen.getUpgradeable() instanceof DualStorageBus)) return;
        ci.cancel();
        if (reset) {
            hn$partition = null;
        } else {
            if (hn$partition == null) hn$partition = ((DualStorageBus) screen.getUpgradeable()).contentsForPartition();
            IAEStackInventory config = screen.getConfig();
            for (int slot = 0; slot < config.getSizeInventory(); slot++) {
                IAEStack<?> entry = null;
                if (screen.isSlotEnabled(slot / 9) && hn$partition.hasNext()) {
                    entry = hn$partition.next()
                        .copy();
                    entry.setStackSize(1);
                }
                config.putAEStackInSlot(slot, entry);
            }
            if (!hn$partition.hasNext()) hn$partition = null;
        }
        screen.setPartitionMode(hn$partition == null ? ActionItems.WRENCH : ActionItems.NEXT_PARTITION);
        screen.detectAndSendChanges();
    }
}
