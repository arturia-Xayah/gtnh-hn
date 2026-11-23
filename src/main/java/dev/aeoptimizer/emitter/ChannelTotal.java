package dev.aeoptimizer.emitter;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEStack;

/** One accumulator owned by each AE network storage monitor. All accesses are on the server thread. */
public final class ChannelTotal {

    public interface Monitor {

        ChannelTotal modernization$total();
    }

    public interface Listener {

        void modernization$totalChanged(ChannelTotal channel, long value);
    }

    private final Set<Listener> listeners = Collections.newSetFromMap(new WeakHashMap<>());
    private BigInteger sum = BigInteger.ZERO;
    private boolean rebuild = true, changed;

    public void subscribe(Listener listener) {
        if (listeners.isEmpty()) rebuild = true;
        if (listeners.add(listener)) changed = true;
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void invalidate() {
        rebuild = true;
    }

    public void change(boolean added, Iterable<IAEStack<?>> changes) {
        if (rebuild || listeners.isEmpty()) return;
        for (IAEStack<?> stack : changes) {
            if (stack == null || stack.getStackSize() == 0) continue;
            BigInteger delta = BigInteger.valueOf(stack.getStackSize());
            sum = added ? sum.add(delta) : sum.subtract(delta);
            changed = true;
        }
        if (sum.signum() < 0) rebuild = true;
    }

    public void flush(IMEMonitor<?> monitor) {
        if ((!rebuild && !changed) || listeners.isEmpty()) return;
        if (rebuild) {
            BigInteger total = BigInteger.ZERO;
            for (IAEStack<?> stack : monitor.getStorageList()) {
                if (stack.getStackSize() > 0) total = total.add(BigInteger.valueOf(stack.getStackSize()));
            }
            sum = total;
            rebuild = false;
        }
        changed = false;
        long visible = sum.bitLength() > 63 ? Long.MAX_VALUE : sum.longValue();
        // Updating redstone can synchronously reconfigure or detach a neighbour.
        for (Listener listener : listeners.toArray(new Listener[0])) {
            listener.modernization$totalChanged(this, visible);
        }
    }
}
