package dev.aeoptimizer.emitter;

/** Used on the part (persisted state) and its container (AE's bidirectional GUI sync). */
public interface EmitterTargets {

    int EXACT = 0, ITEMS = 1, FLUIDS = 2;

    int target(int slot);

    void setTarget(int slot, int mode);
}
