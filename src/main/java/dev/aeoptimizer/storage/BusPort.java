package dev.aeoptimizer.storage;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.ExtractionMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.Upgrades;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.IExternalStorageHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.IStorageBusMonitor;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import appeng.me.storage.MEMonitorPassThrough;
import appeng.me.storage.StorageBusInventoryHandler;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.IterationCounter;
import appeng.util.Platform;
import appeng.util.prioitylist.FuzzyPriorityList;
import appeng.util.prioitylist.IPartitionList;
import appeng.util.prioitylist.PrecisePriorityList;
import appeng.util.target.TileTarget;

final class BusPort<T extends IAEStack<T>> implements IMEMonitorHandlerReceiver<T> {

    private final DualStorageBus bus;
    private final IAEStackType<T> type;
    StorageBusInventoryHandler<T> handler;
    private IMEMonitor<T> monitor;
    private IStorageBusMonitor<T> poll;
    private IItemList<T> previous;

    BusPort(DualStorageBus bus, IAEStackType<T> type) {
        this.bus = bus;
        this.type = type;
    }

    void detach() {
        if (monitor != null) monitor.removeListener(this);
        if (handler != null) handler.invalidate();
        handler = null;
        monitor = null;
        poll = null;
    }

    boolean refresh(TileTarget target, boolean reset) {
        if (!reset && handler != null) return false;
        boolean existed = handler != null;
        previous = existed ? handler.getItemsForRemoval(type.createList(), IterationCounter.fetchNewId()) : null;
        detach();
        bind(target);
        if (!existed && handler == null) return false;
        if (previous == null) previous = type.createList();
        return true;
    }

    void publishDifference() {
        if (previous == null) return;
        IItemList<T> before = previous;
        previous = null;
        IItemList<T> after = type.createList();
        if (handler != null) after = handler.getAvailableItems(after, IterationCounter.fetchNewId());
        Platform.postListChanges(before, after, this, bus.source);
    }

    void bind(TileTarget target) {
        if (target == null) return;
        ForgeDirection face = bus.getSide()
            .getOpposite();
        IExternalStorageHandler provider = AEApi.instance()
            .registries()
            .externalStorage()
            .getHandler(target.tile(), face, type, bus.source);
        if (provider == null) return;
        IMEInventory<T> inv = provider.getInventory(target.tile(), face, type, bus.source);
        if (inv == null) return;
        StorageFilter visibility = (StorageFilter) bus.getConfigManager()
            .getSetting(Settings.STORAGE_FILTER);
        if (inv instanceof IStorageBusMonitor) {
            poll = (IStorageBusMonitor<T>) inv;
            poll.setMode(visibility);
            poll.setActionSource(bus.source);
            poll.onTick(); // Establish the initial inventory before subscribing to deltas.
        } else if (inv instanceof MEMonitorPassThrough) {
            ((MEMonitorPassThrough<?>) inv).setMode(visibility);
        }
        handler = new StorageBusInventoryHandler<>(inv, type, bus::getReshuffleAccess, target);
        AccessRestriction access = (AccessRestriction) bus.getConfigManager()
            .getSetting(Settings.ACCESS);
        handler.setBaseAccess(access);
        handler.setPriority(bus.getPriority());
        handler.setSticky(bus.getInstalledUpgrades(Upgrades.STICKY) > 0);
        handler.setWhitelist(
            bus.getInstalledUpgrades(Upgrades.INVERTER) > 0 ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST);
        handler.setIsExtractFilterActive(
            access == AccessRestriction.READ || access == AccessRestriction.READ_WRITE && bus.getConfigManager()
                .getSetting(Settings.EXTRACTION_MODE) == ExtractionMode.STRICT);
        IPartitionList<T> partition = partition();
        handler.setPartitionList(partition);
        handler.setExtractPartitionList(partition);
        if (inv instanceof IMEMonitor) {
            monitor = (IMEMonitor<T>) inv;
            monitor.addListener(this, handler);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private IPartitionList<T> partition() {
        IAEStackInventory config = bus.getAEInventoryByName(StorageName.CONFIG);
        int slots = 18 + 9 * bus.getInstalledUpgrades(Upgrades.CAPACITY);
        IItemList<T> entries = type.createList();
        boolean any = false;
        for (int i = 0; i < slots; i++) {
            IAEStack<?> entry = config.getAEStackInSlot(i);
            if (entry == null) continue;
            any = true;
            if (entry.getStackType() == type) entries.add((T) entry);
        }
        // An item-only whitelist must reject every fluid, and vice versa.
        if (any && entries.isEmpty()) return new PrecisePriorityList<T>(entries) {

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
        if (type == ITEM_STACK_TYPE && bus.getInstalledUpgrades(Upgrades.FUZZY) > 0) {
            return new FuzzyPriorityList(
                entries,
                (FuzzyMode) bus.getConfigManager()
                    .getSetting(Settings.FUZZY_MODE));
        }
        return new PrecisePriorityList<>(entries);
    }

    Iterator<T> contentsForPartition() {
        if (handler == null || !handler.isValid()
            || !handler.getAccess()
                .hasPermission(AccessRestriction.READ))
            return Collections.emptyIterator();
        IItemList<T> stored = monitor == null ? handler.getInventory()
            .getAvailableItems(type.createList(), IterationCounter.fetchNewId()) : monitor.getStorageList();
        List<T> snapshot = new ArrayList<>();
        for (T entry : stored) if (entry.getStackSize() > 0) snapshot.add(entry.copy());
        return snapshot.iterator();
    }

    TickRateModulation tick() {
        return poll == null ? TickRateModulation.SLOWER : poll.onTick();
    }

    @Override
    public boolean isValid(Object token) {
        return handler != null && token == handler;
    }

    @Override
    public void postChange(IBaseMonitor<T> sender, Iterable<T> changes, BaseActionSource source) {
        if (!bus.getProxy()
            .isActive()) return;
        // A before/after diff is already filtered using the old AND the new settings.
        if (sender != null) {
            if (handler == null || !handler.getAccess()
                .hasPermission(AccessRestriction.READ) && !handler.isVisible()) return;
            if (handler.isExtractFilterActive() && !handler.getExtractPartitionList()
                .isEmpty()) {
                List<T> accepted = new ArrayList<>();
                for (T change : changes) if (handler.getExtractFilterCondition()
                    .test(change)) accepted.add(change);
                if (accepted.isEmpty()) return;
                changes = accepted;
            }
        }
        try {
            bus.getProxy()
                .getStorage()
                .postAlterationOfStoredItems(type, changes, bus.source);
        } catch (GridAccessException ignored) {}
    }

    @Override
    public void onListUpdate() {}
}
