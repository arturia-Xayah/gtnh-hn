package dev.aeoptimizer.dispatch;

/** Implemented by a sending interface's duality and its synchronized container. */
public interface BatchLimitSettings {

    long batchItemLimit();

    long batchFluidLimit();

    boolean hasBatchLimitCard();

    boolean batchLimitBlocked();

    void setBatchLimits(long items, long fluids);

    void setBatchLimitBlocked(boolean blocked);
}
