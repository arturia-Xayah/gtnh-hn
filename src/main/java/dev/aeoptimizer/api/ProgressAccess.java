package dev.aeoptimizer.api;

import java.util.Map;

import appeng.me.diagnostics.CraftingDiagnosticSessionId;
import dev.aeoptimizer.dispatch.TunnelShare;

public interface ProgressAccess {

    TunnelShare hn$share(boolean create);

    long aeo$get();

    void aeo$set(long count);

    Map<CraftingDiagnosticSessionId, Long> aeo$consume(long count);
}
