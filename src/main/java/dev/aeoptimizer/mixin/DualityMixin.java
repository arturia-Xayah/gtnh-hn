package dev.aeoptimizer.mixin;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.DualityInterface;
import appeng.parts.p2p.PartP2PInterface;
import dev.aeoptimizer.dispatch.BatchContext;
import dev.aeoptimizer.dispatch.BatchLimitSettings;
import dev.aeoptimizer.dispatch.BatchLimits;
import dev.aeoptimizer.dispatch.ItemBatchLimitCard;

/** Dispatch restrictions scoped to the submitting CPU's optimized transaction. */
@Mixin(value = DualityInterface.class, remap = false)
public abstract class DualityMixin implements BatchLimitSettings {

    @Shadow
    private appeng.util.ScheduledReason scheduledReason;
    @Unique
    private long modernization$itemLimit;
    @Unique
    private long modernization$fluidLimit;
    @Unique
    private boolean modernization$limitBlocked;
    @Unique
    private long modernization$lastLimitWarning = Long.MIN_VALUE;

    public long batchItemLimit() {
        return modernization$itemLimit;
    }

    public long batchFluidLimit() {
        return modernization$fluidLimit;
    }

    public boolean batchLimitBlocked() {
        return modernization$limitBlocked;
    }

    public boolean hasBatchLimitCard() {
        DualityInterface duality = (DualityInterface) (Object) this;
        if (ItemBatchLimitCard.TYPE == null || duality.getInstalledUpgrades(ItemBatchLimitCard.TYPE) == 0) return false;
        Object host = duality.getHost();
        return !(host instanceof PartP2PInterface) || ((PartP2PInterface) host).isOutput();
    }

    public void setBatchLimits(long items, long fluids) {
        modernization$itemLimit = Math.max(0, items);
        modernization$fluidLimit = Math.max(0, fluids);
        modernization$limitBlocked = false;
        ((DualityInterface) (Object) this).saveChanges();
    }

    public void setBatchLimitBlocked(boolean blocked) {
        if (blocked && !modernization$limitBlocked) {
            net.minecraft.tileentity.TileEntity tile = ((DualityInterface) (Object) this).getHost()
                .getTile();
            long tick = tile.getWorldObj()
                .getTotalWorldTime();
            // Several patterns may alternate on one interface; never log once per failed push/tick.
            if (modernization$lastLimitWarning == Long.MIN_VALUE || tick < modernization$lastLimitWarning
                || tick - modernization$lastLimitWarning >= 200) {
                modernization$lastLimitWarning = tick;
                dev.aeoptimizer.OrderOptimizer.LOG.warn(
                    "Batch dispatch paused: input exceeds card limit at {}, {}, {} (dimension {}). Item limit {}, fluid limit {} mB.",
                    tile.xCoord,
                    tile.yCoord,
                    tile.zCoord,
                    tile.getWorldObj().provider.dimensionId,
                    modernization$itemLimit,
                    modernization$fluidLimit);
            }
        }
        modernization$limitBlocked = blocked;
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void modernization$saveLimits(NBTTagCompound nbt, CallbackInfo ci) {
        nbt.setLong("ModernizationBatchItems", modernization$itemLimit);
        nbt.setLong("ModernizationBatchFluids", modernization$fluidLimit);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void modernization$loadLimits(NBTTagCompound nbt, CallbackInfo ci) {
        modernization$itemLimit = Math.max(0, nbt.getLong("ModernizationBatchItems"));
        modernization$fluidLimit = Math.max(0, nbt.getLong("ModernizationBatchFluids"));
        modernization$limitBlocked = false;
    }

    @Inject(method = "pushPattern", at = @At("HEAD"), cancellable = true)
    private void modernization$checkLimit(ICraftingPatternDetails pattern, InventoryCrafting table,
        CallbackInfoReturnable<Boolean> cir) {
        if (!BatchLimits.accepts((DualityInterface) (Object) this, table)) {
            scheduledReason = appeng.util.ScheduledReason.BLOCKING_MODE;
            cir.setReturnValue(false);
        }
    }

    @Redirect(
        method = "pushPattern",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/implementations/tiles/ICraftingMachine;pushPattern(Lappeng/api/networking/crafting/ICraftingPatternDetails;Lnet/minecraft/inventory/InventoryCrafting;Lnet/minecraftforge/common/util/ForgeDirection;)Z"))
    private boolean aeo$legacyPlans(ICraftingMachine machine, ICraftingPatternDetails pattern, InventoryCrafting table,
        ForgeDirection side) {
        return !BatchContext.ACTIVE.get() && machine.pushPattern(pattern, table, side);
    }
}
