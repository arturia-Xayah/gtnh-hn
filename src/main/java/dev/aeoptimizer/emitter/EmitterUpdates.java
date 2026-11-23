package dev.aeoptimizer.emitter;

import java.util.LinkedHashSet;
import java.util.Set;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Publish redstone after both AE storage monitors have delivered the tick's counts. */
public final class EmitterUpdates {

    public static final EmitterUpdates INSTANCE = new EmitterUpdates();

    public interface Output {

        void modernization$publish();
    }

    private final Set<Output> pending = new LinkedHashSet<>();

    public void schedule(Output output) {
        pending.add(output);
    }

    public void remove(Output output) {
        pending.remove(output);
    }

    public void clear() {
        pending.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void end(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending.isEmpty()) return;
        Output[] outputs = pending.toArray(new Output[0]);
        pending.clear();
        for (Output output : outputs) output.modernization$publish();
    }
}
