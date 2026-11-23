package dev.structureterminal.output;

// Transaction behavior adapted from GT5-Unofficial 5.09.54.275 MTEHatchOutputME (LGPL-3.0).
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.util.item.AEFluidStack;
import dev.aeoptimizer.dispatch.OutputOverflow;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.interfaces.IOutputTransaction;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.outputme.base.MTEHatchOutputMEBase;
import gregtech.common.tileentities.machines.outputme.util.AECacheCounter;

final class FluidOutputTransaction implements IOutputHatchTransaction, IOutputTransaction.IRecipeCheckAware,
    IOutputTransaction.IProtectOutputAware, dev.aeoptimizer.dispatch.OverflowRoute {

    private final AECacheCounter<GTUtility.FluidId> cache = new AECacheCounter<>();
    private final MixedOutputBus bus;
    private final MTEHatchOutputMEBase<IAEFluidStack> provider;
    private final long availableSpace;
    private boolean active = true;
    private boolean allowAnyInput = false;
    private boolean isRecipeCheck = false;
    private boolean isProtectOutput = true;
    private boolean isDynamicCapacity = false;
    private IMEInventoryHandler<IAEFluidStack> cell = null;

    FluidOutputTransaction(MixedOutputBus bus) {
        this.bus = bus;
        this.provider = bus.fluids;
        availableSpace = provider.getPhysicalSpace();
    }

    public void setRecipeCheck(boolean isRecipeCheck) {
        this.isRecipeCheck = isRecipeCheck;
        if (isRecipeCheck && provider.shouldCheckCell()) {
            provider.flushCachedStack();
            cell = AEApi.instance()
                .registries()
                .cell()
                .getCellInventory(
                    bus.fluidCell()
                        .copy(),
                    bus,
                    StorageChannel.FLUIDS);
        }
        updateFlags();
    }

    public void setProtectOutput(boolean isProtectOutput) {
        this.isProtectOutput = isProtectOutput;
        updateFlags();
    }

    private void updateFlags() {
        // only false when cache mode on and is distribution
        isDynamicCapacity = isRecipeCheck && isProtectOutput
            && provider.getCheckMode()
            && (!provider.getCacheMode() || !provider.isDistribution())
            && !provider.canVoidOverflow();
        allowAnyInput = !provider.getCheckMode() && availableSpace > 0;
        if (!isRecipeCheck) {
            allowAnyInput |= provider.getLastInputTick() == provider.getTickCounter();
        }
    }

    @Override
    public boolean needsTotalParallelData() {
        return isDynamicCapacity;
    }

    @Override
    public IOutputHatch getHatch() {
        return bus.fluidEndpoint;
    }

    public boolean modernization$overflowEnabled() {
        return !isRecipeCheck && OutputOverflow.enabled(bus.getProxy());
    }

    public boolean modernization$normalSpace() {
        return cache.getTotal() < availableSpace;
    }

    @Override
    public boolean hasAvailableSpace() {
        return (!isRecipeCheck && OutputOverflow.enabled(bus.getProxy())) || allowAnyInput
            || cache.getTotal() < availableSpace;
    }

    @Override
    public boolean storePartial(GTUtility.FluidId id, @NotNull FluidStack stack, long totalPerParallel,
        long perParallel) {
        if (!active) throw new IllegalStateException("Cannot add to a transaction after committing it");

        if (isRecipeCheck) {
            if (provider.shouldCheckCell()) {
                IAEFluidStack input = AEFluidStack.create(stack);
                if (isDynamicCapacity) {
                    long cellAvailableSpace = provider.getCellAvailableSpace();
                    int parallels = clamp(cellAvailableSpace / totalPerParallel, 1, Integer.MAX_VALUE);
                    long amount = Math.min(parallels * perParallel, cellAvailableSpace - cache.getTotal());
                    amount = Math.max(0, Math.min(amount, stack.amount));
                    input.setStackSize(amount);
                }
                IAEFluidStack rejected = cell.injectItems(input, Actionable.MODULATE, bus.getActionSource());
                int inserted = (int) (input.getStackSize() - (rejected == null ? 0 : rejected.getStackSize()));
                cache.insert(id, inserted);
                stack.amount -= inserted;
                return inserted > 0;
            } else if (isDynamicCapacity) {
                int parallels = clamp(availableSpace / totalPerParallel, 1, Integer.MAX_VALUE);
                long amount = Math.min(parallels * perParallel, availableSpace - cache.getTotal());
                amount = Math.max(0, Math.min(amount, stack.amount));
                cache.insert(id, amount);
                stack.amount -= amount;
                return amount > 0;
            }
        }
        if (!hasAvailableSpace() || !isFilteredTo(id)) {
            return false;
        }
        cache.insert(id, stack.amount);
        stack.amount = 0;
        return true;
    }

    @Override
    public void complete(GTUtility.FluidId id) {
        // Do nothing
    }

    @Override
    public void commit() {
        if (!active) throw new IllegalStateException("Transaction already committed");
        if (cache.getTotal() > 0) {
            cache.iterateAll(
                (id, amount) -> {
                    provider.addToCache(
                        AEFluidStack.create(id.getFluidStack())
                            .setStackSize(amount));
                });
            provider.updateLastInputTick();
            bus.markDirty();
        }
        active = false;
    }

    private static int clamp(long n, int min, int max) {
        return (int) Math.max(min, Math.min(max, n));
    }
}
