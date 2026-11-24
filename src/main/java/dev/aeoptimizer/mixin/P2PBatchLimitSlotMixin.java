package dev.aeoptimizer.mixin;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.helpers.DualityInterface;
import appeng.parts.automation.UpgradeInventory;
import appeng.parts.p2p.PartP2PInterface;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.util.Platform;
import dev.aeoptimizer.dispatch.ItemBatchLimitCard;

@Mixin(value = UpgradeInventory.class, remap = false)
public abstract class P2PBatchLimitSlotMixin {

    @Shadow
    @Final
    private IAEAppEngInventory parent;

    @Inject(method = { "func_94041_b", "isItemValidForSlot" }, at = @At("HEAD"), cancellable = true)
    private void modernization$outputOnly(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // The server owns the tunnel role. A client-side part need not know it when the GUI opens.
        if (stack == null || stack.getItem() != ItemBatchLimitCard.INSTANCE
            || !(parent instanceof DualityInterface)
            || !Platform.isServer()) return;
        Object host = ((DualityInterface) parent).getHost();
        if (host instanceof PartP2PInterface && !((PartP2PInterface) host).isOutput()) cir.setReturnValue(false);
    }
}
