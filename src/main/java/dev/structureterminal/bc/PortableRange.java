package dev.structureterminal.bc;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import buildcraft.core.DefaultProps;
import buildcraft.core.lib.block.BlockBuildCraft;
import buildcraft.core.lib.block.TileBuildCraft;

public final class PortableRange {

    public static final String KEY = "modernization.bcDimensions";

    public static final String DRAFT = "modernization.bcDraft";

    private PortableRange() {}

    public static int[] actualDimensions(TileEntity tile) {
        int[] b = RangeJobs.bounds(tile);
        if (b.length != 6) return new int[] { 1, 1, 1 };
        int f = front(tile), fx = f == 5 ? 1 : f == 4 ? -1 : 0, fz = f == 3 ? 1 : f == 2 ? -1 : 0;
        int x = tile.xCoord + fx, y = tile.yCoord, z = tile.zCoord + fz;
        boolean aligned = fx > 0 ? b[0] == x : fx < 0 ? b[3] == x : fz > 0 ? b[2] == z : b[5] == z;
        int low = fx == 0 ? b[0] : b[2], high = fx == 0 ? b[3] : b[5], anchor = fx == 0 ? x : z;
        if (!aligned || (anchor != low && anchor != high) || (y != b[1] && y != b[4]))
            throw new IllegalArgumentException("bc.range.unaligned");
        int width = high - low + 1;
        return new int[] { width == 1 ? 1 : width * (anchor == low ? 1 : -1) * (fx == 0 ? -fz : fx),
            (b[4] - b[1] + 1) * (y == b[1] ? 1 : -1), fx == 0 ? b[5] - b[2] + 1 : b[3] - b[0] + 1 };
    }

    public static int[] draft(TileEntity tile) {
        int[] stored = ((RangeMachine) tile).rangeData()
            .getIntArray(DRAFT);
        return stored.length == 3 ? stored : actualDimensions(tile);
    }

    public static void saveDraft(TileEntity tile, int[] size) {
        bounds(
            tile.xCoord,
            tile.yCoord,
            tile.zCoord,
            front(tile),
            size,
            tile.getWorldObj()
                .getHeight());
        ((RangeMachine) tile).rangeData()
            .setIntArray(DRAFT, size.clone());
        tile.markDirty();
    }

    public static int front(TileEntity tile) {
        return ((BlockBuildCraft) tile.getBlockType()).getFrontSide(tile.getBlockMetadata());
    }

    public static int[] dimensions(TileEntity tile) {
        return ((RangeMachine) tile).rangeData()
            .getIntArray(KEY);
    }

    public static int[] bounds(int x, int y, int z, int facing, int[] size, int height) {
        if (size.length != 3) throw new IllegalArgumentException("bc.range.dimensions");
        return WorkRange.from(x, y, z, facing, size[0], size[1], size[2], DefaultProps.MARKER_RANGE, height)
            .coords();
    }

    public static boolean apply(TileEntity tile, int[] size) {
        int[] box = bounds(
            tile.xCoord,
            tile.yCoord,
            tile.zCoord,
            front(tile),
            size,
            tile.getWorldObj()
                .getHeight());
        NBTTagCompound data = ((RangeMachine) tile).rangeData();
        boolean changed = !java.util.Arrays.equals(size, data.getIntArray(KEY));
        boolean sameBox = java.util.Arrays.equals(box, RangeJobs.bounds(tile));
        data.setIntArray(KEY, size.clone());
        boolean queued = RangeJobs.submit(tile, box);
        // Native BC only sends a new box when it actually starts using it.
        if (changed && (queued || sameBox)) ((TileBuildCraft) tile).sendNetworkUpdate();
        return queued;
    }
}
