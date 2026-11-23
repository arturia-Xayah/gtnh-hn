package dev.aeoptimizer.dispatch;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.inventory.InventoryCrafting;

import appeng.api.storage.data.IAEStack;
import appeng.helpers.DualityInterface;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEItemStack;

public final class BatchLimits {

    private BatchLimits() {}

    /** Merge repeated ingredients by AE identity (including item NBT), not by slot or display name. */
    public static long maximum(Iterable<IAEStack<?>> inputs, BatchLimitSettings settings, long requested) {
        if (!settings.hasBatchLimitCard()) return requested;
        long itemLimit = settings.batchItemLimit(), fluidLimit = settings.batchFluidLimit();
        if (itemLimit == 0 && fluidLimit == 0) return requested;
        Map<IAEStack<?>, Long> quantities = new HashMap<>();
        for (IAEStack<?> input : inputs) {
            if (input == null) continue;
            long limit = input.isFluid() ? fluidLimit : input.isItem() ? itemLimit : 0;
            if (limit == 0) continue;
            long amount = input.getStackSize();
            if (amount <= 0) return 0;
            Long previous = quantities.get(input);
            if (previous != null) {
                if (amount > Long.MAX_VALUE - previous) return 0;
                amount += previous;
            }
            quantities.put(input, amount);
        }
        long result = requested;
        for (Map.Entry<IAEStack<?>, Long> entry : quantities.entrySet()) result = BatchLimitMath.restrict(
            result,
            entry.getKey()
                .isFluid() ? fluidLimit : itemLimit,
            entry.getValue());
        return result;
    }

    /** Check actual substituted inputs too, before the native interface can partially accept a table. */
    public static boolean accepts(DualityInterface duality, InventoryCrafting table) {
        BatchLimitSettings settings = (BatchLimitSettings) duality;
        if (!BatchContext.OPTIMIZED.get() || !settings.hasBatchLimitCard()) return true;
        if (settings.batchItemLimit() == 0 && settings.batchFluidLimit() == 0) return true;
        java.util.List<IAEStack<?>> inputs = new java.util.ArrayList<>(table.getSizeInventory());
        for (int slot = 0; slot < table.getSizeInventory(); slot++) inputs.add(
            table instanceof MEInventoryCrafting ? ((MEInventoryCrafting) table).getAEStackInSlot(slot)
                : AEItemStack.create(table.getStackInSlot(slot)));
        boolean accepted = maximum(inputs, settings, 1) == 1;
        settings.setBatchLimitBlocked(!accepted);
        return accepted;
    }
}
