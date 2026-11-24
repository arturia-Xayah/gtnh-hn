package dev.aeoptimizer.storage;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;
import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.google.common.collect.Iterators;

import appeng.api.config.ExtractionMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IConfigManager;
import appeng.core.settings.TickRates;
import appeng.me.GridAccessException;
import appeng.me.storage.MEInventoryHandler;
import appeng.parts.misc.PartStorageBus;
import appeng.util.target.TileTarget;
import appeng.util.target.TileTargetCache;

/** One cable part and one powered channel. Each stack type has its own external monitor and handler. */
public final class DualStorageBus extends PartStorageBus {

    final MachineSource source = new MachineSource(this);
    final BusPort<IAEItemStack> items = new BusPort<>(this, ITEM_STACK_TYPE);
    final BusPort<IAEFluidStack> fluids = new BusPort<>(this, FLUID_STACK_TYPE);
    private final TileTargetCache targets = new TileTargetCache();
    private TileTarget bound;
    private IPart boundFace;

    private IPart facingPart(TileTarget target) {
        if (target == null || !(target.tile() instanceof IPartHost)) return null;
        return ((IPartHost) target.tile()).getPart(getSide().getOpposite());
    }

    public Iterator<IAEStack<?>> contentsForPartition() {
        initialize();
        return Iterators.concat(items.contentsForPartition(), fluids.contentsForPartition());
    }

    private boolean initialized;
    private boolean settingsChanged;
    private long nextProbe;

    public DualStorageBus(ItemStack stack) {
        this(
            stack,
            stack.hasTagCompound() && stack.getTagCompound()
                .hasKey("configManager"));
    }

    private DualStorageBus(ItemStack stack, boolean configured) {
        super(stack);
        if (!configured) getConfigManager().putSetting(Settings.EXTRACTION_MODE, ExtractionMode.STRICT);
    }

    @Override
    public ItemStack getPrimaryGuiIcon() {
        return new ItemStack(ItemDualStorageBus.INSTANCE);
    }

    @Override
    public void onNeighborChanged() {
        targets.markStale();
        super.onNeighborChanged();
    }

    @Override
    public void saveAEStackInv() {
        settingsChanged = true;
        super.saveAEStackInv();
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
        settingsChanged = true;
        super.updateSetting(manager, settingName, newValue);
    }

    @Override
    public void upgradesChanged() {
        settingsChanged = true;
        super.upgradesChanged();
    }

    @Override
    public void setPriority(int value) {
        settingsChanged = true;
        super.setPriority(value);
    }

    @Override
    public MEInventoryHandler getInternalHandler() {
        initialize();
        return items.handler;
    }

    private void initialize() {
        if (initialized || getTile() == null || getTile().getWorldObj() == null) return;
        initialized = true;
        bound = targets.get(getTile(), getSide());
        boundFace = facingPart(bound);
        settingsChanged = false;
        items.bind(bound);
        fluids.bind(bound);
        nextProbe = getTile().getWorldObj()
            .getTotalWorldTime() + 80;
    }

    @Override
    public List<IMEInventoryHandler> getCellArray(IAEStackType<?> type) {
        if (!getProxy().isActive()) return Collections.emptyList();
        initialize();
        IMEInventoryHandler handler = type == ITEM_STACK_TYPE ? items.handler
            : type == FLUID_STACK_TYPE ? fluids.handler : null;
        return handler == null ? Collections.emptyList() : Collections.singletonList(handler);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.StorageBus.getMin(), TickRates.StorageBus.getMax(), false, true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int elapsed) {
        initialize();
        TileTarget current = targets.get(getTile(), getSide());
        IPart face = facingPart(current);
        boolean reset = settingsChanged || current != bound || face != boundFace;
        long now = getTile().getWorldObj()
            .getTotalWorldTime();
        // A newly placed GT tile may acquire its capabilities after its first neighbor update.
        boolean probe = current != null && now >= nextProbe;
        if (reset || probe) {
            bound = current;
            boundFace = face;
            settingsChanged = false;
            nextProbe = now + 80;
            boolean itemChange = items.refresh(current, reset);
            boolean fluidChange = fluids.refresh(current, reset);
            if (itemChange || fluidChange) {
                try {
                    getProxy().getGrid()
                        .postEvent(new MENetworkCellArrayUpdate());
                } catch (GridAccessException ignored) {}
                items.publishDifference();
                fluids.publishDifference();
            }
        }
        TickRateModulation i = items.tick();
        TickRateModulation f = fluids.tick();
        if (current == null && !targets.isWaitingForChunk()) return TickRateModulation.SLEEP;
        return i.ordinal() >= f.ordinal() ? i : f;
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        items.detach();
        fluids.detach();
        initialized = false;
        bound = null;
        boundFace = null;
        targets.markStale();
    }
}
