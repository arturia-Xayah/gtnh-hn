package dev.aeoptimizer.dispatch;

public final class BatchContext {

    public static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> OPTIMIZED = ThreadLocal.withInitial(() -> false);

    private BatchContext() {}
}
