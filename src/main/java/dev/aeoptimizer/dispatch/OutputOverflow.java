package dev.aeoptimizer.dispatch;

import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import dev.aeoptimizer.network.OptimizerNetworks;
import dev.aeoptimizer.network.TileOptimizer;

public final class OutputOverflow {

    private OutputOverflow() {}

    public static boolean enabled(AENetworkProxy proxy) {
        try {
            TileOptimizer optimizer = proxy == null ? null : OptimizerNetworks.find(proxy.getGrid());
            return optimizer != null && optimizer.outputOverflow;
        } catch (GridAccessException e) {
            return false;
        }
    }
}
