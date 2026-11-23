package dev.aestoragevault.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.World;

import dev.aestoragevault.core.VaultGeometry;
import dev.aestoragevault.world.TileController;

/** Synced footprint for custom shell rendering when Angelica snapshots omit neighboring tile entities. */
public final class VaultCasingSkin {

    private static final java.util.Map<World, java.util.concurrent.ConcurrentMap<net.minecraft.util.ChunkCoordinates, TileController>> OWNERS = java.util.Collections
        .synchronizedMap(new java.util.WeakHashMap<>());
    private final List<net.minecraft.util.ChunkCoordinates> positions = new ArrayList<>();
    private World registeredWorld;
    private int centerX, centerY, centerZ, facing;

    /** Chunk render snapshots may omit neighboring tile entities. Use the synced client footprint. */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static TileController owner(int x, int y, int z) {
        return owner(net.minecraft.client.Minecraft.getMinecraft().theWorld, x, y, z);
    }

    public static TileController owner(World world, int x, int y, int z) {
        java.util.Map<net.minecraft.util.ChunkCoordinates, TileController> map = OWNERS.get(world);
        TileController owner = map == null ? null : map.get(new net.minecraft.util.ChunkCoordinates(x, y, z));
        return owner != null && owner.formed && !owner.isInvalid() ? owner : null;
    }

    public void update(TileController owner, boolean remove) {
        World world = owner.getWorldObj();
        if (world == null || !world.isRemote) return;
        int front = world.getBlockMetadata(owner.xCoord, owner.yCoord, owner.zCoord);
        int cx = owner.xCoord - VaultGeometry.normal(front, 0);
        int cy = owner.yCoord - VaultGeometry.normal(front, 1);
        int cz = owner.zCoord - VaultGeometry.normal(front, 2);
        boolean visible = !remove && owner.formed && !owner.isInvalid();
        if (!visible && positions.isEmpty()) return;
        if (visible && registeredWorld == world && cx == centerX && cy == centerY && cz == centerZ && front == facing)
            return;
        if (registeredWorld != null) {
            java.util.Map<net.minecraft.util.ChunkCoordinates, TileController> old = OWNERS.get(registeredWorld);
            if (old != null) {
                for (net.minecraft.util.ChunkCoordinates pos : positions) old.remove(pos, owner);
                if (old.isEmpty()) OWNERS.remove(registeredWorld);
            }
            registeredWorld.markBlockRangeForRenderUpdate(
                centerX - 1,
                centerY - 1,
                centerZ - 1,
                centerX + 1,
                centerY + 1,
                centerZ + 1);
        }
        positions.clear();
        registeredWorld = visible ? world : null;
        centerX = cx;
        centerY = cy;
        centerZ = cz;
        facing = front;
        if (visible) {
            java.util.concurrent.ConcurrentMap<net.minecraft.util.ChunkCoordinates, TileController> map = OWNERS
                .computeIfAbsent(world, w -> new java.util.concurrent.ConcurrentHashMap<>());
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                if (!VaultGeometry.shell(x, y, z)) continue;
                net.minecraft.util.ChunkCoordinates pos = new net.minecraft.util.ChunkCoordinates(
                    cx + x,
                    cy + y,
                    cz + z);
                positions.add(pos);
                map.put(pos, owner);
            }
        }
        world.markBlockRangeForRenderUpdate(
            owner.xCoord - 2,
            owner.yCoord - 2,
            owner.zCoord - 2,
            owner.xCoord + 2,
            owner.yCoord + 2,
            owner.zCoord + 2);
    }
}
