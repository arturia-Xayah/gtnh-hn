package dev.aeoptimizer.mixin.client;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.config.Upgrades;
import appeng.client.gui.implementations.GuiUpgradeable;
import dev.aeoptimizer.dispatch.ItemBatchLimitCard;

@Mixin(value = GuiUpgradeable.class, remap = false)
public abstract class BatchLimitUpgradeTooltipMixin {

    @Inject(method = "getUpgradeCardStack", at = @At("HEAD"), cancellable = true)
    private void modernization$tooltip(Upgrades type, CallbackInfoReturnable<ItemStack> cir) {
        if (type == dev.aeoptimizer.storage.KindLimitCard.TYPE)
            cir.setReturnValue(new ItemStack(dev.aeoptimizer.storage.KindLimitCard.INSTANCE));
        if (type == ItemBatchLimitCard.TYPE) cir.setReturnValue(new ItemStack(ItemBatchLimitCard.INSTANCE));
    }
}
