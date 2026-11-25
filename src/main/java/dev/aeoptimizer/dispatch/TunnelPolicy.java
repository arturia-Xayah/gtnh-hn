package dev.aeoptimizer.dispatch;

import net.minecraft.nbt.NBTTagCompound;

/** Shared by all crafting CPUs using this particular input. Accessed on the server thread. */
public final class TunnelPolicy {

    public static final int DEFAULT = 0, EVEN = 1, ROUND = 2, FIRST_AVAILABLE = 3, DELAY = 10;
    public int mode;
    private int remainder, next;
    private long resume;

    public void setMode(int value) {
        int valid = value >= DEFAULT && value <= FIRST_AVAILABLE ? value : DEFAULT;
        if (valid != mode) {
            mode = valid;
            remainder = next = 0;
            resume = 0;
        }
    }

    public int splitStart(int crafts, int size) {
        int start = Math.floorMod(remainder, size);
        remainder = (start + crafts % size) % size;
        return start;
    }

    public int next(int size) {
        return Math.floorMod(next, size);
    }

    public boolean visit(int index, int size, long tick) {
        if (tick < resume || index != next(size)) return false;
        next = (index + 1) % size;
        if (next == 0) resume = tick + DELAY;
        return true;
    }

    public void write(NBTTagCompound n) {
        n.setInteger("hnRouteMode", mode);
        n.setInteger("hnRouteRemainder", remainder);
        n.setInteger("hnRouteNext", next);
        n.setLong("hnRouteResume", resume);
    }

    public void read(NBTTagCompound n) {
        setMode(n.getInteger("hnRouteMode"));
        remainder = n.getInteger("hnRouteRemainder");
        next = n.getInteger("hnRouteNext");
        resume = n.getLong("hnRouteResume");
    }

    public interface Host {

        TunnelPolicy hn$policy();
    }

    public interface Menu {

        /** -1 for a container other than a P2P input. */
        int hn$mode();

        void hn$cycle();
    }
}
