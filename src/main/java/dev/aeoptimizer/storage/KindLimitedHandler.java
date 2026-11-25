package dev.aeoptimizer.storage;

import java.util.function.Supplier;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IStorageBusMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.helpers.IInterfaceHost;
import appeng.me.cache.NetworkMonitor;
import appeng.me.storage.MEMonitorPassThrough;
import appeng.me.storage.StorageBusInventoryHandler;
import appeng.parts.misc.PartStorageBus;
import appeng.util.IterationCounter;
import appeng.util.target.TileTarget;
import gregtech.common.items.ItemIntegratedCircuit;

/** Restrict writes at the external inventory boundary, below AE priority routing. */
public final class KindLimitedHandler<T extends IAEStack<T>> extends StorageBusInventoryHandler<T> {

    private final PartStorageBus bus;
    private final IAEStackType<T> type;
    private final IInterfaceHost bufferInterface;
    private NetworkMonitor<T> network;
    private NetworkKinds<T> kinds;

    private KindLimitedHandler(PartStorageBus bus, IMEInventory<T> inv, IAEStackType<T> type,
        Supplier<AccessRestriction> reshuffle, TileTarget target) {
        super(inv, type, reshuffle, target);
        this.bus = bus;
        this.type = type;
        Object endpoint = target.tile();
        if (endpoint instanceof IPartHost) endpoint = ((IPartHost) endpoint).getPart(
            bus.getSide()
                .getOpposite());
        bufferInterface = inv instanceof IStorageBusMonitor && endpoint instanceof IInterfaceHost
            ? (IInterfaceHost) endpoint
            : null;
    }

    public static <T extends IAEStack<T>> StorageBusInventoryHandler<T> create(PartStorageBus bus, IMEInventory<T> inv,
        IAEStackType<T> type, Supplier<AccessRestriction> reshuffle, TileTarget target) {
        if (bus.getInstalledUpgrades(KindLimitCard.TYPE) == 0)
            return new StorageBusInventoryHandler<>(inv, type, reshuffle, target);
        return new KindLimitedHandler<>(bus, inv, type, reshuffle, target);
    }

    static boolean ignored(IAEStack<?> stack) {
        return stack instanceof IAEItemStack && ((IAEItemStack) stack).getItem() instanceof ItemIntegratedCircuit;
    }

    @Override
    public boolean canAccept(T input) {
        if (!isValid() || !super.canAccept(input)) return false;
        // An unconfigured interface exposes only its local slots before its network comes online.
        // Accepting a stack into that transient inventory would bypass the slice's actual contents.
        if (unboundInterface()) return false;
        if (ignored(input)) return true;
        if (bus instanceof DualStorageBus) {
            DualStorageBus dual = (DualStorageBus) bus;
            StorageBusInventoryHandler<?> other = dual.items.handler == this ? dual.fluids.handler : dual.items.handler;
            if (other != null
                && (!(other instanceof KindLimitedHandler) || !((KindLimitedHandler<?>) other).allows(input)))
                return false;
        }
        return allows(input);
    }

    private boolean unboundInterface() {
        return bufferInterface != null && !bufferInterface.getInterfaceDuality()
            .hasConfig();
    }

    private boolean allows(IAEStack<?> input) {
        if (!isValid() || unboundInterface()) return false;
        IMEInventory<T> inv = getInventory();
        if (inv instanceof MEMonitorPassThrough) inv = ((MEMonitorPassThrough<T>) inv).getInternal();
        if (inv == null) return false;
        if (inv instanceof NetworkMonitor) {
            if (network != inv) {
                network = (NetworkMonitor<T>) inv;
                kinds = NetworkKinds.watch(network);
            }
            return kinds.allows(network, input);
        }
        // Unmonitored containers and sided inventories may change directly between two writes in one tick.
        if (inv instanceof IStorageBusMonitor) ((IStorageBusMonitor<?>) inv).onTick();
        for (T key : inv.getAvailableItems(type.createList(), IterationCounter.fetchNewId()))
            if (key.getStackSize() > 0 && !ignored(key) && !key.equals(input)) return false;
        return true;
    }

    @Override
    public T injectItems(T input, Actionable action, BaseActionSource src) {
        // MEInventoryHandler.injectItems calls our canAccept again at commit time.
        long offered = input.getStackSize();
        T left = super.injectItems(input, action, src);
        if (action == Actionable.MODULATE && kinds != null
            && !ignored(input)
            && (left == null || left.getStackSize() < offered)) kinds.changed(input);
        return left;
    }
}
