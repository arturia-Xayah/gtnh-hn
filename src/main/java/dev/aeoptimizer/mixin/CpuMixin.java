package dev.aeoptimizer.mixin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.CraftUpdateListener;
import appeng.container.ContainerNull;
import appeng.crafting.MECraftingInventory;
import appeng.helpers.DualityInterface;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.cluster.implementations.CraftingCPUCluster.TaskProgress;
import appeng.util.ScheduledReason;
import appeng.util.inv.MEInventoryCrafting;
import dev.aeoptimizer.api.CpuBatchStats;
import dev.aeoptimizer.api.IBatchMedium;
import dev.aeoptimizer.api.ProgressAccess;
import dev.aeoptimizer.dispatch.BatchContext;
import dev.aeoptimizer.dispatch.BatchLimitSettings;
import dev.aeoptimizer.dispatch.BatchLimits;
import dev.aeoptimizer.dispatch.OrderOrigin;
import dev.aeoptimizer.dispatch.TunnelDispatch;
import dev.aeoptimizer.dispatch.TunnelShare;

/** Bulk exact processing; unsupported crafting/receiver semantics stay in the untouched native loop. */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class CpuMixin implements CpuBatchStats {

    @Shadow
    @Final
    protected Map<ICraftingPatternDetails, TaskProgress> tasks;
    @Shadow
    @Final
    protected Map<ICraftingPatternDetails, TaskProgress> workableTasks;
    @Shadow
    @Final
    protected HashMap<ICraftingPatternDetails, ScheduledReason> reasonProvider;
    @Shadow
    @Final
    protected HashMap<ICraftingPatternDetails, List<ICraftingMedium>> parallelismProvider;
    @Shadow
    protected MECraftingInventory inventory;
    @Shadow
    protected MachineSource machineSrc;
    @Shadow
    protected IItemList<IAEStack<?>> waitingFor;
    @Shadow
    protected int accelerator, remainingOperations;
    @Shadow
    protected boolean suspended, somethingChanged;
    @Shadow
    @Final
    protected List<CraftUpdateListener> craftUpdateListeners;

    @Shadow
    public abstract IGrid getGrid();

    @Shadow
    protected abstract net.minecraft.world.World getWorld();

    @Shadow
    protected abstract void postChange(IAEStack stack, appeng.api.networking.security.BaseActionSource source);

    @Shadow
    protected abstract void postCraftingStatusChange(IAEStack stack);

    @Shadow
    public abstract void markDirty();

    @Shadow
    protected abstract List<IAEStack<?>> getExpandedInputs(ICraftingPatternDetails details, CraftingGridCache cache);

    @Shadow
    protected abstract List<IAEStack<?>> getExpandedCondensedInputs(ICraftingPatternDetails details,
        CraftingGridCache cache);

    @Shadow
    @Final
    protected appeng.me.cluster.implementations.CraftingCpuDiagnostics diagnostics;

    @Shadow
    protected abstract boolean isCraftingDiagnosticsEnabled();

    @Unique
    private final OrderOrigin aeo$origin = new OrderOrigin();
    @Unique
    private boolean aeo$wasBusy;
    @Unique
    private boolean hn$hadRoutes;

    @Inject(method = "submitJob", at = @At("HEAD"))
    private void aeo$beforeSubmit(IGrid grid, appeng.api.networking.crafting.ICraftingJob job,
        appeng.api.networking.security.BaseActionSource source,
        appeng.api.networking.crafting.ICraftingRequester requester,
        CallbackInfoReturnable<appeng.api.networking.crafting.ICraftingLink> cir) {
        aeo$wasBusy = !tasks.isEmpty() || !waitingFor.isEmpty();
    }

    @Inject(method = "submitJob", at = @At("RETURN"))
    private void aeo$afterSubmit(IGrid grid, appeng.api.networking.crafting.ICraftingJob job,
        appeng.api.networking.security.BaseActionSource source,
        appeng.api.networking.crafting.ICraftingRequester requester,
        CallbackInfoReturnable<appeng.api.networking.crafting.ICraftingLink> cir) {
        if (cir.getReturnValue() != null) aeo$origin.accept(OrderOrigin.of(job, grid), getGrid(), aeo$wasBusy);
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void aeo$saveOrigin(NBTTagCompound nbt, CallbackInfo ci) {
        aeo$origin.write(nbt);
        NBTTagList rows = nbt.getTagList("tasks", 10);
        int row = 0;
        for (TaskProgress task : tasks.values()) {
            TunnelShare share = ((ProgressAccess) (Object) task).hn$share(false);
            if (share != null) share.write(rows.getCompoundTagAt(row));
            row++;
        }
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void aeo$loadOrigin(NBTTagCompound nbt, CallbackInfo ci) {
        aeo$origin.read(nbt);
        NBTTagList rows = nbt.getTagList("tasks", 10);
        for (int i = 0; i < rows.tagCount(); i++) {
            NBTTagCompound row = rows.getCompoundTagAt(i);
            if (!row.hasKey("hnShares")) continue;
            hn$hadRoutes = true;
            appeng.api.storage.data.IAEItemStack stack = appeng.util.item.AEItemStack.loadItemStackFromNBT(row);
            if (stack == null || !(stack.getItem() instanceof appeng.api.implementations.ICraftingPatternItem))
                continue;
            TaskProgress task = tasks.get(
                ((appeng.api.implementations.ICraftingPatternItem) stack.getItem())
                    .getPatternForItem(stack.getItemStack(), getWorld()));
            if (task != null) ((ProgressAccess) (Object) task).hn$share(true)
                .read(row);
        }
    }

    @Unique
    private final Map<ICraftingPatternDetails, Integer> aeo$nextMedium = new java.util.WeakHashMap<>();

    @Unique
    private long aeo$tick = Long.MIN_VALUE;
    @Unique
    private int aeo$transactions;
    @Unique
    private long aeo$successfulBatches;

    public long aeo$getBatches() {
        return aeo$successfulBatches;
    }

    @Inject(method = "executeCrafting", at = @At("HEAD"))
    private void aeo$dispatch(IEnergyGrid energy, CraftingGridCache cache, CallbackInfo ci) {
        if (suspended) return;
        if (!aeo$origin.enabled(getGrid())) {
            if (hn$hadRoutes) {
                for (TaskProgress task : tasks.values()) {
                    TunnelShare share = ((ProgressAccess) (Object) task).hn$share(false);
                    if (share != null) share.clear();
                }
                hn$hadRoutes = false;
                markDirty();
            }
            return;
        }
        long tick = getWorld().getTotalWorldTime();
        if (tick != aeo$tick) {
            aeo$tick = tick;
            aeo$transactions = 0;
        }
        Iterator<Map.Entry<ICraftingPatternDetails, TaskProgress>> iterator = workableTasks.entrySet()
            .iterator();
        int sent = 0;
        while (iterator.hasNext()) {
            Map.Entry<ICraftingPatternDetails, TaskProgress> entry = iterator.next();
            ICraftingPatternDetails pattern = entry.getKey();
            List<ICraftingMedium> mediums = cache.getMediums(pattern);
            if (pattern.isCraftable() || pattern.canSubstitute()
                || pattern.isInputOnly()
                || pattern.getCondensedAEOutputs().length != 1
                || mediums.isEmpty()) continue;
            boolean eligible = true;
            for (ICraftingMedium medium : mediums) {
                if (!(medium instanceof DualityInterface) && !(medium instanceof IBatchMedium)) {
                    eligible = false;
                    break;
                }
                if (medium instanceof DualityInterface && ((DualityInterface) medium).isFakeCraftingMode()) {
                    eligible = false;
                    break;
                }
            }
            if (!eligible) continue;
            // Native loop must not revisit this task and fall back to thousands of single pushes this tick.
            iterator.remove();
            ProgressAccess progress = (ProgressAccess) (Object) entry.getValue();
            List<IAEStack<?>> inputs = getExpandedCondensedInputs(pattern, cache);
            List<IAEStack<?>> slots = getExpandedInputs(pattern, cache);
            if (inputs == null || slots == null) throw new IllegalStateException("AEO input-only expansion failed");
            TunnelDispatch route = TunnelDispatch.inspect(mediums, progress);
            hn$hadRoutes |= route != null;
            boolean extended = false;
            while (progress.aeo$get() > 0 && remainingOperations > 0 && aeo$transactions < 64) {
                long max = Math.min(progress.aeo$get(), Math.max(1, Math.min(4096, accelerator)));
                long funded = progress.aeo$get();
                double unitEnergy = 0;
                for (IAEStack<?> input : inputs) {
                    if (input.getStackSize() <= 0) throw new IllegalStateException("AEO invalid input amount");
                    IAEStack<?> available = inventory.extractItems(
                        input.copy()
                            .setStackSize(Long.MAX_VALUE),
                        Actionable.SIMULATE);
                    funded = Math
                        .min(funded, (available == null ? 0 : available.getStackSize()) / input.getStackSize());
                }
                max = Math.min(max, funded);
                for (IAEStack<?> slot : slots) if (slot != null) {
                    unitEnergy += (double) slot.getStackSize() / slot.getAmountPerUnit();
                    max = Math.min(max, Integer.MAX_VALUE / slot.getStackSize());
                }
                for (IAEStack<?> output : pattern.getCondensedAEOutputs())
                    max = Math.min(max, Long.MAX_VALUE / output.getStackSize());
                if (max <= 0) {
                    reasonProvider.put(pattern, ScheduledReason.NOT_ENOUGH_INGREDIENTS);
                    break;
                }
                boolean accepted = false;
                int firstMedium = aeo$nextMedium.getOrDefault(pattern, 0) % mediums.size();
                List<ICraftingMedium> ordered = route == null ? mediums : route.order(firstMedium);
                for (int offset = 0; offset < ordered.size(); offset++) {
                    int mediumIndex = (firstMedium + offset) % mediums.size();
                    ICraftingMedium medium = route == null ? mediums.get(mediumIndex) : ordered.get(offset);
                    long count = route == null ? max : route.allowance(medium, max, tick);
                    if (count <= 0 || medium.isBusy()) continue;
                    BatchLimitSettings limits = medium instanceof BatchLimitSettings ? (BatchLimitSettings) medium
                        : null;
                    boolean limited = limits != null && limits.hasBatchLimitCard();
                    if (medium.getBlockingMode() == ICraftingMedium.BlockingMode.BLOCKING && !limited) count = 1;
                    if (limited) {
                        count = BatchLimits.maximum(inputs, limits, count);
                        limits.setBatchLimitBlocked(count == 0);
                        if (count == 0) {
                            reasonProvider.put(pattern, ScheduledReason.BLOCKING_MODE);
                            continue;
                        }
                    }
                    if (medium instanceof IBatchMedium)
                        count = Math.max(0, Math.min(count, ((IBatchMedium) medium).maxBatch(pattern, count)));
                    double perCraft = unitEnergy;
                    if (medium instanceof DualityInterface) perCraft *= Math
                        .pow(4, ((DualityInterface) medium).getInstalledUpgrades(Upgrades.PATTERN_CAPACITY));
                    if (perCraft > 0) count = Math.min(
                        count,
                        (long) Math.floor(
                            (energy.extractAEPower(perCraft * count, Actionable.SIMULATE, PowerMultiplier.CONFIG)
                                + .000001) / perCraft));
                    if (count <= 0) continue;
                    for (int attempt = 0; attempt < 2; attempt++) {
                        MEInventoryCrafting table = aeo$take(slots, count);
                        if (table == null) break;
                        boolean pushed;
                        boolean oldActive = BatchContext.ACTIVE.get();
                        boolean oldOptimized = BatchContext.OPTIMIZED.get();
                        BatchContext.ACTIVE.set(count > 1);
                        BatchContext.OPTIMIZED.set(true);
                        try {
                            pushed = medium instanceof IBatchMedium
                                ? ((IBatchMedium) medium).pushBatch(pattern, table, count)
                                : medium.pushPattern(pattern, table);
                        } finally {
                            BatchContext.ACTIVE.set(oldActive);
                            BatchContext.OPTIMIZED.set(oldOptimized);
                        }
                        if (pushed) {
                            energy.extractAEPower(perCraft * count, Actionable.MODULATE, PowerMultiplier.CONFIG);
                            progress.aeo$set(progress.aeo$get() - count);
                            Map<appeng.me.diagnostics.CraftingDiagnosticSessionId, Long> sessions = progress
                                .aeo$consume(count);
                            if (isCraftingDiagnosticsEnabled()) {
                                for (Map.Entry<appeng.me.diagnostics.CraftingDiagnosticSessionId, Long> session : sessions
                                    .entrySet())
                                    for (IAEStack<?> output : pattern.getCondensedAEOutputs())
                                        ((CpuDiagnosticsMixin) (Object) diagnostics).aeo$expected(
                                            output.copy()
                                                .setStackSize(
                                                    Math.multiplyExact(output.getStackSize(), session.getValue())),
                                            CraftingCPUCluster.getServerTick(),
                                            session.getKey());
                            }
                            for (IAEStack<?> output : pattern.getCondensedAEOutputs()) {
                                IAEStack<?> expected = output.copy()
                                    .setStackSize(Math.multiplyExact(output.getStackSize(), count));
                                postChange(expected, machineSrc);
                                waitingFor.add(expected.copy());
                                postCraftingStatusChange(expected.copy());
                            }
                            for (IAEStack<?> input : inputs) postChange(
                                input.copy()
                                    .setStackSize(Math.multiplyExact(input.getStackSize(), count)),
                                machineSrc);
                            if (route != null) {
                                route.accepted(medium, count);
                                mediumIndex = mediums.indexOf(medium);
                            }
                            aeo$nextMedium.put(pattern, (mediumIndex + 1) % mediums.size());
                            reasonProvider.remove(pattern);
                            remainingOperations--;
                            aeo$transactions++;
                            aeo$successfulBatches++;
                            sent++;
                            somethingChanged = true;
                            accepted = true;
                            markDirty();
                            break;
                        }
                        aeo$return(table);
                        reasonProvider.put(pattern, medium.getScheduledReason());
                        if (count == 1) break;
                        count = 1; // One bounded retry for a receiver that refuses a large batch.
                    }
                    if (accepted) break;
                }
                if (!accepted) {
                    if (extended || route == null || !route.extend(max, funded)) break;
                    extended = true;
                    markDirty();
                } else extended = false;
            }
            if (progress.aeo$get() == 0) {
                tasks.remove(pattern);
                aeo$nextMedium.remove(pattern);
                parallelismProvider.remove(pattern);
                reasonProvider.remove(pattern);
            }
        }
        if (sent > 0) for (CraftUpdateListener listener : craftUpdateListeners) listener.accept(sent);
    }

    /** Native fallback paths (e.g. substitutions) must not bypass a limit smaller than one recipe. */
    @Redirect(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingMedium;pushPattern(Lappeng/api/networking/crafting/ICraftingPatternDetails;Lnet/minecraft/inventory/InventoryCrafting;)Z"))
    private boolean modernization$nativeLimit(ICraftingMedium medium, ICraftingPatternDetails pattern,
        net.minecraft.inventory.InventoryCrafting table) {
        boolean old = BatchContext.OPTIMIZED.get();
        BatchContext.OPTIMIZED.set(aeo$origin.enabled(getGrid()));
        try {
            return medium.pushPattern(pattern, table);
        } finally {
            BatchContext.OPTIMIZED.set(old);
        }
    }

    @Unique
    private MEInventoryCrafting aeo$take(List<IAEStack<?>> inputs, long count) {
        MEInventoryCrafting table = new MEInventoryCrafting(new ContainerNull(), Math.max(1, inputs.size()), 1);
        for (int i = 0; i < inputs.size(); i++) {
            IAEStack<?> input = inputs.get(i);
            if (input == null) continue;
            long amount = Math.multiplyExact(input.getStackSize(), count);
            IAEStack<?> got = inventory.extractItems(
                input.copy()
                    .setStackSize(amount),
                Actionable.MODULATE);
            if (got != null) table.setInventorySlotContents(i, got);
            if (got == null || got.getStackSize() != amount) {
                aeo$return(table);
                return null;
            }
        }
        return table;
    }

    @Unique
    private void aeo$return(MEInventoryCrafting table) {
        for (int i = 0; i < table.getSizeInventory(); i++) {
            IAEStack<?> stack = table.getAEStackInSlot(i);
            if (stack != null) inventory.injectItems(stack, Actionable.MODULATE);
        }
    }
}
