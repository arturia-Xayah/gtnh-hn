package dev.structureterminal.stocking;

import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.config.Actionable;
import appeng.api.networking.storage.IStackWatcher;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.tile.inventory.IAEStackInventory;
import appeng.tile.inventory.IIAEStackInventory;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.enums.Textures.BlockIcons;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity.SkipGenerateDescription;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.gui.modularui.hatch.MTEHatchInputBusMEGui;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;

/** Native stocking bus with one shared set of sixteen item/fluid filters. */
@SkipGenerateDescription
public final class MixedInputBus extends MTEHatchInputBusME implements IIAEStackInventory {

    public final IAEStackInventory filters = new IAEStackInventory(this, SLOT_COUNT, StorageName.CONFIG);
    private final FluidStack[] fluids = new FluidStack[SLOT_COUNT];
    private final int[] fluidAmounts = new int[SLOT_COUNT];
    private final long[] autoAmounts = new long[SLOT_COUNT];
    private IStackWatcher mixedWatcher;
    private boolean changingFilters;

    public MixedInputBus(int id, boolean advanced, String name, String title) {
        super(id, advanced, name, title);
    }

    private MixedInputBus(String name, boolean advanced, int tier, String[] description, ITexture[][][] textures) {
        super(name, advanced, tier, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MixedInputBus(mName, autoPullAvailable, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(BlockIcons.OVERLAY_ME_INPUT_FLUID_HATCH_ACTIVE),
            TextureFactory.of(BlockIcons.OVERLAY_PIPE_IN) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(BlockIcons.OVERLAY_ME_INPUT_FLUID_HATCH),
            TextureFactory.of(BlockIcons.OVERLAY_PIPE_IN) };
    }

    @Override
    public String[] getDescription() {
        return new String[] { net.minecraft.util.StatCollector.translateToLocal("mixed.input.description"),
            net.minecraft.util.StatCollector
                .translateToLocal(autoPullAvailable ? "mixed.input.advanced" : "mixed.input.basic"),
            net.minecraft.util.StatCollector.translateToLocal("mixed.input.reset") };
    }

    private long lastGuiPoll = Long.MIN_VALUE;
    private final Slot[] guiSlots = new Slot[SLOT_COUNT];

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        if (getBaseMetaTileEntity().isServerSide()) {
            updateGuiSnapshot();
            sync.onServerTick(this::updateGuiSnapshot);
        }
        return new MTEHatchInputBusMEGui(this, guiSlots).build(data, sync, settings);
    }

    private void updateGuiSnapshot() {
        long tick = getBaseMetaTileEntity().getWorld()
            .getTotalWorldTime();
        if (lastGuiPoll != Long.MIN_VALUE && tick >= lastGuiPoll && tick - lastGuiPoll < 10) return;
        lastGuiPoll = tick;
        if (!processingRecipe) updateAllInformationSlots();
        for (int i = 0; i < SLOT_COUNT; i++) {
            IAEStack<?> filter = filters.getAEStackInSlot(i);
            if (!(filter instanceof IAEFluidStack)) {
                guiSlots[i] = slots[i];
                continue;
            }
            FluidStack fluid = ((IAEFluidStack) filter).getFluidStack();
            ItemStack icon = fluid.tag == null ? GTUtility.getFluidDisplayStack(fluid, false)
                : ItemFluidPacket.newStack(fluid);
            Slot view = new Slot(icon);
            view.extractedAmount = fluidAmounts[i];
            view.extracted = view.extractedAmount == 0 ? null : icon;
            guiSlots[i] = view;
        }
    }

    public void setFilter(int index, IAEStack<?> stack) {
        if (index < 0 || index >= SLOT_COUNT) return;
        if (stack != null && !(stack instanceof IAEItemStack) && !(stack instanceof IAEFluidStack)) return;
        if (stack != null)
            for (int i = 0; i < SLOT_COUNT; i++) if (i != index && stack.equals(filters.getAEStackInSlot(i))) return;
        filters.putAEStackInSlot(
            index,
            stack == null ? null
                : stack.copy()
                    .setStackSize(1));
    }

    @Override
    public IAEStackInventory getAEInventoryByName(StorageName name) {
        return name == StorageName.CONFIG ? filters : null;
    }

    @Override
    public void saveAEStackInv() {
        if (changingFilters || getBaseMetaTileEntity() == null) return;
        for (int i = 0; i < SLOT_COUNT; i++) {
            IAEStack<?> stack = filters.getAEStackInSlot(i);
            super.setSlotConfig(i, stack instanceof IAEItemStack ? ((IAEItemStack) stack).getItemStack() : null);
        }
        lastGuiPoll = Long.MIN_VALUE;
        bindWatcher();
        markDirty();
        notifyWatchers();
        getBaseMetaTileEntity().enableTicking();
    }

    private void bindWatcher() {
        if (mixedWatcher == null) return;
        mixedWatcher.clear();
        if (!gregtech.common.config.MachineStats.machines.useStackWatcher) return;
        for (int i = 0; i < SLOT_COUNT; i++) {
            IAEStack<?> stack = filters.getAEStackInSlot(i);
            if (stack != null) mixedWatcher.add(stack);
        }
    }

    @Override
    public void updateWatcher(IStackWatcher watcher) {
        mixedWatcher = watcher;
        bindWatcher();
    }

    @Override
    public void setSlotConfig(int index, ItemStack config) {
        FluidStack fluid = config != null && config.getItem() instanceof ItemFluidPacket
            ? ItemFluidPacket.getFluidStack(config)
            : GTUtility.getFluidFromDisplayStack(config);
        if (fluid != null && fluid.amount <= 0) fluid.amount = 1;
        setFilter(index, fluid == null ? AEItemStack.create(config) : AEFluidStack.create(fluid));
    }

    private void replaceFilters(IAEStack<?>[] next) {
        changingFilters = true;
        try {
            for (int i = 0; i < SLOT_COUNT; i++) filters.putAEStackInSlot(
                i,
                next[i] == null ? null
                    : next[i].copy()
                        .setStackSize(1));
        } finally {
            changingFilters = false;
        }
        saveAEStackInv();
    }

    @Override
    protected void clearSlotConfigs() {
        super.clearSlotConfigs();
        if (filters != null) replaceFilters(new IAEStack<?>[SLOT_COUNT]);
    }

    @Override
    protected void refreshItemList() {
        if (!isAllowedToWork()) return;
        try {
            // AE's meaningful iterators advance in hasNext(). Never probe one twice before next().
            IAEStack<?>[] items = new IAEStack<?>[SLOT_COUNT], liquids = new IAEStack<?>[SLOT_COUNT];
            int itemCount = 0, fluidCount = 0;
            for (IAEItemStack item : getProxy().getStorage()
                .getItemInventory()
                .getStorageList()) {
                if (item.getStackSize() >= minAutoPullStackSize) items[itemCount++] = item;
                if (itemCount == SLOT_COUNT) break;
            }
            for (IAEFluidStack fluid : getProxy().getStorage()
                .getFluidInventory()
                .getStorageList()) {
                if (fluid.getStackSize() >= minAutoPullStackSize) liquids[fluidCount++] = fluid;
                if (fluidCount == SLOT_COUNT) break;
            }
            IAEStack<?>[] next = new IAEStack<?>[SLOT_COUNT];
            int i = 0, item = 0, fluid = 0;
            while (i < SLOT_COUNT && (item < itemCount || fluid < fluidCount)) {
                if (item < itemCount) next[i++] = items[item++];
                if (i < SLOT_COUNT && fluid < fluidCount) next[i++] = liquids[fluid++];
            }
            boolean changed = false;
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                IAEStack<?> old = filters.getAEStackInSlot(slot), current = next[slot];
                boolean same = old == null ? current == null : old.equals(current);
                changed |= !same;
                long amount = current == null ? 0 : current.getStackSize();
                if (amount > 0 && (!same || amount > autoAmounts[slot])) justHadNewItems = true;
                autoAmounts[slot] = amount;
            }
            if (changed) replaceFilters(next);
        } catch (GridAccessException ignored) {}
    }

    @Override
    protected void updateAllInformationSlots() {
        super.updateAllInformationSlots();
        Arrays.fill(fluids, null);
        Arrays.fill(fluidAmounts, 0);
        if (!isAllowedToWork()) {
            return;
        }
        try {
            for (int i = 0; i < SLOT_COUNT; i++) {
                IAEStack<?> stack = filters.getAEStackInSlot(i);
                if (!(stack instanceof IAEFluidStack)) continue;
                IAEFluidStack available = getProxy().getStorage()
                    .getFluidInventory()
                    .extractItems(
                        ((IAEFluidStack) stack).copy()
                            .setStackSize(Integer.MAX_VALUE),
                        Actionable.SIMULATE,
                        getRequestSource());
                if (available != null) {
                    fluids[i] = available.getFluidStack();
                    fluidAmounts[i] = fluids[i].amount;
                }
            }
        } catch (GridAccessException ignored) {}
    }

    public FluidStack[] fluidInputs() {
        return isAllowedToWork() ? fluids : new FluidStack[0];
    }

    @Override
    public CheckRecipeResult endRecipeProcessing(MTEMultiBlockBase controller) {
        CheckRecipeResult result = super.endRecipeProcessing(controller);
        try {
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (fluids[i] == null) continue;
                int used = fluidAmounts[i] - fluids[i].amount;
                if (used <= 0) continue;
                IAEFluidStack extracted = Platform.poweredExtraction(
                    getProxy().getEnergy(),
                    getProxy().getStorage()
                        .getFluidInventory(),
                    ((IAEFluidStack) filters.getAEStackInSlot(i)).copy()
                        .setStackSize(used),
                    getRequestSource());
                if (extracted == null || extracted.getStackSize() != used)
                    throw new IllegalStateException("Fluid extraction changed during recipe check");
            }
        } catch (GridAccessException | IllegalStateException e) {
            controller.stopMachine(ShutDownReasonRegistry.CRITICAL_NONE);
            result = SimpleCheckRecipeResult.ofFailurePersistOnShutdown("stocking_hatch_fail_extraction");
        } finally {
            Arrays.fill(fluids, null);
            Arrays.fill(fluidAmounts, 0);
        }
        return result;
    }

    public int drainFluid(FluidStack request, boolean simulate) {
        if (!isAllowedToWork()) return 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            IAEStack<?> filter = filters.getAEStackInSlot(i);
            if (!(filter instanceof IAEFluidStack) || !((IAEFluidStack) filter).getFluidStack()
                .isFluidEqual(request)) continue;
            if (processingRecipe) {
                int amount = fluids[i] == null ? 0 : Math.min(request.amount, fluids[i].amount);
                if (!simulate && fluids[i] != null) fluids[i].amount -= amount;
                return amount;
            }
            try {
                IAEFluidStack result = Platform.poweredExtraction(
                    getProxy().getEnergy(),
                    getProxy().getStorage()
                        .getFluidInventory(),
                    AEFluidStack.create(request),
                    getRequestSource(),
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE);
                return result == null ? 0 : (int) result.getStackSize();
            } catch (GridAccessException e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public void saveNBTData(NBTTagCompound n) {
        super.saveNBTData(n);
        writeFilters(n);
    }

    @Override
    public void loadNBTData(NBTTagCompound n) {
        super.loadNBTData(n);
        restoreFilters(n);
    }

    @Override
    public NBTTagCompound getCopiedData(EntityPlayer p) {
        NBTTagCompound n = super.getCopiedData(p);
        writeFilters(n);
        return n;
    }

    @Override
    public boolean pasteCopiedData(EntityPlayer p, NBTTagCompound n) {
        if (!super.pasteCopiedData(p, n)) return false;
        restoreFilters(n);
        return true;
    }

    private void writeFilters(NBTTagCompound n) {
        filters.writeToNBT(n, "mixedFilters");
        if (!n.hasKey("mixedFilters")) n.setTag("mixedFilters", new NBTTagCompound());
    }

    private void restoreFilters(NBTTagCompound n) {
        IAEStack<?>[] next = new IAEStack<?>[SLOT_COUNT];
        if (n.hasKey("mixedFilters", 10)) {
            IAEStackInventory stored = new IAEStackInventory(null, SLOT_COUNT);
            stored.readFromNBT(n, "mixedFilters");
            for (int i = 0; i < SLOT_COUNT; i++) {
                IAEStack<?> candidate = stored.getAEStackInSlot(i);
                if (!(candidate instanceof IAEItemStack) && !(candidate instanceof IAEFluidStack)) continue;
                boolean duplicate = false;
                for (int j = 0; j < i; j++) duplicate |= candidate.equals(next[j]);
                if (!duplicate) next[i] = candidate;
            }
        } else {
            for (int i = 0; i < SLOT_COUNT; i++) next[i] = AEItemStack.create(super.getSlotConfig(i));
        }
        minAutoPullStackSize = Math.max(1, minAutoPullStackSize);
        autoPullRefreshTime = Math.max(1, autoPullRefreshTime);
        replaceFilters(next);
    }
}
