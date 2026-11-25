package dev.aeoptimizer.storage;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.data.IAEStack;
import appeng.me.cache.NetworkMonitor;
import appeng.util.IterationCounter;

/** Shared by all entrances looking at this exact target monitor. Server thread only. */
final class NetworkKinds<T extends IAEStack<T>> {

    private static final WeakHashMap<NetworkMonitor<?>, WeakReference<NetworkKinds<?>>> indices = new WeakHashMap<>();
    private final Set<T> dirty = new HashSet<>();
    private boolean rebuild = true;
    private long revision;
    // Count actual storage queries, including cold reads, for the dedicated-server regression fixture.
    private long fullReads, keyReads;
    private T first, second;

    @SuppressWarnings("unchecked")
    static <T extends IAEStack<T>> NetworkKinds<T> watch(NetworkMonitor<T> net) {
        WeakReference<NetworkKinds<?>> ref = indices.get(net);
        NetworkKinds<T> state = ref == null ? null : (NetworkKinds<T>) ref.get();
        if (state != null) return state;
        state = new NetworkKinds<>();
        indices.put(net, new WeakReference<>(state));
        final WeakReference<NetworkKinds<T>> owner = new WeakReference<>(state);
        net.addListener(new IMEMonitorHandlerReceiver<T>() {

            public boolean isValid(Object token) {
                return owner.get() != null;
            }

            public void postChange(IBaseMonitor<T> monitor, Iterable<T> changes, BaseActionSource src) {
                NetworkKinds<T> index = owner.get();
                if (index != null) for (T key : changes) if (!KindLimitedHandler.ignored(key)) index.changed(key);
            }

            public void onListUpdate() {
                NetworkKinds<T> index = owner.get();
                if (index != null) index.invalidate();
            }
        }, null);
        return state;
    }

    private void invalidate() {
        revision++;
        rebuild = true;
        dirty.clear();
    }

    void changed(T key) {
        revision++;
        if (rebuild || dirty.contains(key)) return;
        // Two known occupied types already prove the target is mixed. Only their disappearance matters.
        if (second != null && !key.equals(first) && !key.equals(second)) return;
        if (dirty.size() >= 8) invalidate();
        else dirty.add(key.copy());
    }

    boolean allows(NetworkMonitor<T> net, IAEStack<?> candidate) {
        if (!rebuild && dirty.size() == 1 && candidate.equals(first) && second == null && dirty.contains(first))
            return true;
        long before = revision;
        if (!rebuild && !dirty.isEmpty()) {
            // Notifications can repeat or represent topology changes. Query presence, never add deltas.
            for (T key : dirty.toArray((T[]) new IAEStack[0])) {
                keyReads++;
                T found = net.getHandler()
                    .getAvailableItem(key, IterationCounter.fetchNewId());
                boolean present = found != null && found.getStackSize() > 0;
                if (second != null) {
                    if (!present && (key.equals(first) || key.equals(second))) {
                        rebuild = true;
                        break;
                    }
                } else if (present) remember(key);
                else if (key.equals(first)) first = null;
            }
        }
        if (rebuild) {
            fullReads++;
            first = null;
            second = null;
            // Read the live target even after nested AE operations; getStorageList can still be dirty.
            for (T key : net.getHandler()
                .getAvailableItems(
                    net.getStackType()
                        .createList(),
                    IterationCounter.fetchNewId())) {
                if (key.getStackSize() > 0 && !KindLimitedHandler.ignored(key)) remember(key);
                if (second != null) break;
            }
        }
        if (before != revision) {
            invalidate();
            return false;
        }
        rebuild = false;
        dirty.clear();
        return second == null && (first == null || candidate.equals(first));
    }

    private void remember(T key) {
        if (first == null) first = key.copy();
        else if (!first.equals(key) && second == null) second = key.copy();
    }
}
