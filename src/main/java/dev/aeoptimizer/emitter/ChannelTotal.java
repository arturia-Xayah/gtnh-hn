package dev.aeoptimizer.emitter;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import appeng.api.storage.data.IAEStack;
import appeng.me.cache.NetworkMonitor;
import appeng.util.IterationCounter;

/** Shared, server-thread snapshot of one AE storage channel. Notifications identify dirty keys, not quantities. */
public final class ChannelTotal {

    private static final int SAMPLE_INTERVAL = 5;
    private static final int MAX_POINT_READS = 8;

    public interface Monitor {

        ChannelTotal modernization$total();
    }

    public interface Listener {

        void modernization$totalChanged(ChannelTotal channel, long value);
    }

    private final Set<Listener> listeners = Collections.newSetFromMap(new WeakHashMap<>());
    private final Map<IAEStack<?>, Long> quantities = new HashMap<>();
    private final Set<IAEStack<?>> dirtyKeys = new HashSet<>();
    private BigInteger sum = BigInteger.ZERO;
    private boolean rebuild = true, notify;
    private int cooldown;
    private long revision;

    public void subscribe(Listener listener) {
        if (listeners.isEmpty()) invalidate();
        if (listeners.add(listener)) notify = true;
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) discard();
    }

    private void discard() {
        invalidate();
        quantities.clear();
        sum = BigInteger.ZERO;
        notify = false;
    }

    public void invalidate() {
        revision++;
        rebuild = true;
        dirtyKeys.clear();
    }

    public void changedKey(IAEStack<?> key) {
        revision++;
        if (rebuild || listeners.isEmpty() || dirtyKeys.contains(key)) return;
        if (dirtyKeys.size() == MAX_POINT_READS) {
            invalidate();
        } else {
            dirtyKeys.add(key.copy());
        }
    }

    public void flush(NetworkMonitor<?> monitor) {
        if (cooldown > 0) cooldown--;
        if (listeners.isEmpty()) {
            if (!rebuild || !quantities.isEmpty()) discard();
            return;
        }

        if (rebuild || !dirtyKeys.isEmpty()) {
            if (cooldown > 0) return;
            cooldown = SAMPLE_INTERVAL;
            long before = revision;
            if (rebuild) {
                quantities.clear();
                sum = BigInteger.ZERO;
                for (IAEStack<?> item : monitor.getStorageList()) {
                    long amount = item.getStackSize();
                    if (amount > 0) {
                        quantities.put(item.copy(), amount);
                        sum = sum.add(BigInteger.valueOf(amount));
                    }
                }
            } else {
                // A provider may synchronously invalidate the monitor during a lookup.
                for (IAEStack<?> key : dirtyKeys.toArray(new IAEStack<?>[0])) {
                    long amount = readQuantity(monitor, key);
                    Long previous = amount == 0 ? quantities.remove(key) : quantities.put(key, amount);
                    sum = sum.add(BigInteger.valueOf(amount - (previous == null ? 0L : previous)));
                }
            }
            if (revision != before) {
                invalidate();
                return;
            }
            dirtyKeys.clear();
            rebuild = false;
            notify = true;
        }

        if (notify) {
            notify = false;
            long visible = sum.bitLength() > 63 ? Long.MAX_VALUE : sum.longValue();
            for (Listener listener : listeners.toArray(new Listener[0])) {
                listener.modernization$totalChanged(this, visible);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static long readQuantity(NetworkMonitor monitor, IAEStack<?> key) {
        // Keys are supplied by this very monitor and belong to its stack type.
        IAEStack<?> current = monitor.getHandler()
            .getAvailableItem(key, IterationCounter.fetchNewId());
        return current == null ? 0 : Math.max(0, current.getStackSize());
    }
}
