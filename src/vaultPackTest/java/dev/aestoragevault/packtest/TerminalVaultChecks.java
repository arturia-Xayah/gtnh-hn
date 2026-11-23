package dev.aestoragevault.packtest;

import java.lang.reflect.Proxy;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import cpw.mods.fml.common.gameevent.TickEvent;
import dev.aestoragevault.VaultMod;
import dev.aestoragevault.core.VaultGeometry;
import dev.aestoragevault.world.TileAccess;
import dev.aestoragevault.world.TileController;
import dev.aestoragevault.world.VaultStructure;
import dev.structureterminal.StructureTerminal;
import dev.structureterminal.TerminalSettings;
import dev.structureterminal.bridge.NativeBridge;
import dev.structureterminal.bridge.StructureScanner;
import dev.structureterminal.server.BuildService;

/** Real pack construction and client-view regression tests, no Minecraft client or GL context. */
final class TerminalVaultChecks {

    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }

    static void previews(WorldServer world) {
        check(IMultiblockInfoContainer.contains(TileController.class), "NEI discovery registration absent");
        IMultiblockInfoContainer<TileController> info = IMultiblockInfoContainer.get(TileController.class);
        for (int face = 0; face < 6; face++) for (int variant = 1; variant <= 3; variant++) {
            int x = 24, y = 145, z = 24;
            clear(world, x, y, z);
            world.setBlock(x, y, z, VaultMod.CONTROLLER, face, 2);
            TileController t = (TileController) world.getTileEntity(x, y, z);
            info.construct(
                new ItemStack(StructureTerminal.item, variant),
                false,
                t,
                ExtendedFacing.of(ForgeDirection.getOrientation(face)));
            t.checkStructure();
            check(t.formed, "preview not formed face=" + face + " variant=" + variant);
            syncedSkin(world, t);
            VaultStructure.Shape shape = VaultStructure.inspect(t);
            int casings = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                int wx = shape.centerX + dx, wy = shape.centerY + dy, wz = shape.centerZ + dz;
                if (!VaultMod.isResonanceCasing(world, wx, wy, wz)) continue;
                casings++;
                check(world.getTileEntity(wx, wy, wz) == null, "passive casing created a tile entity");
            }
            check(casings == (variant == 3 ? 22 : 23), "wrong dedicated casing count " + casings);
            check((shape.output != null) == (variant == 3), "preview optional output");
            check((shape.access instanceof dev.aestoragevault.world.TileOutput) == (variant == 2), "preview ME port");
        }
        clear(world, 24, 145, 24);
    }

    private static void syncedSkin(WorldServer world, TileController owner) {
        VaultStructure.Shape shape = VaultStructure.inspect(owner);
        dev.aestoragevault.client.VaultCasingSkin skin = new dev.aestoragevault.client.VaultCasingSkin();
        // Exercise client footprint synchronization without launching a client or initializing GL.
        boolean remote = world.isRemote;
        world.isRemote = true;
        try {
            skin.update(owner, false);
            int cells = 0;
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                int wx = shape.centerX + x, wy = shape.centerY + y, wz = shape.centerZ + z;
                boolean shell = VaultGeometry.shell(x, y, z);
                check(
                    dev.aestoragevault.client.VaultCasingSkin.owner(world, wx, wy, wz) == (shell ? owner : null),
                    "synced casing/controller/port footprint missing or covers center air");
                if (shell) cells++;
                check(
                    gregtech.api.render.RenderOverlay.get(world, wx, wy, wz) == null,
                    "custom renderer must not allocate unused GT casing overlays");
                skin.update(owner, false);
                check(
                    dev.aestoragevault.client.VaultCasingSkin.owner(world, wx, wy, wz) == (shell ? owner : null),
                    "unchanged state lost footprint");
            }
            check(cells == 26, "synced footprint does not include all 26 shell blocks");
            owner.formed = false;
            skin.update(owner, false);
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) check(
                dev.aestoragevault.client.VaultCasingSkin
                    .owner(world, shape.centerX + x, shape.centerY + y, shape.centerZ + z) == null,
                "broken structure retained skin");
            owner.formed = true;
            skin.update(owner, false);
            skin.update(owner, true);
            check(
                dev.aestoragevault.client.VaultCasingSkin
                    .owner(world, shape.centerX + 1, shape.centerY + 1, shape.centerZ + 1) == null,
                "unloaded controller retained skin");
        } finally {
            skin.update(owner, true);
            world.isRemote = remote;
        }
    }

    static void instant(WorldServer world, FakePlayer player, boolean partial) throws Exception {
        int x = 24, y = 145, z = 24;
        clear(world, x, y, z);
        world.setBlock(x, y, z, VaultMod.CONTROLLER, 2, 2);
        TileController t = (TileController) world.getTileEntity(x, y, z);
        if (partial) {
            // Existing parts deliberately differ from the NEI example layout.
            world.setBlock(x + 1, y, z + 2, VaultMod.ACCESS, 3, 2);
            world.setBlock(x - 1, y, z + 2, gregtech.api.GregTechAPI.sBlockMachines, 0, 2);
            ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) world.getTileEntity(x - 1, y, z + 2))
                .setInitialValuesAsNBT(
                    null,
                    (short) gregtech.api.enums.ItemList.Hatch_Input_Bus_LuV.get(1)
                        .getItemDamage());
            world.setBlock(x, y + 1, z, VaultMod.RESONANCE_CASING, 0, 2);
        }
        boolean creative = player.capabilities.isCreativeMode;
        ItemStack held = player.getHeldItem();
        double px = player.posX, py = player.posY, pz = player.posZ;
        try {
            player.setPosition(x + .5, y, z - 2);
            player.capabilities.isCreativeMode = true;
            player.inventory
                .setInventorySlotContents(player.inventory.currentItem, new ItemStack(StructureTerminal.item));
            NativeBridge bridge = NativeBridge.resolve(world, x, y, z, 2);
            check(bridge != null && bridge.supportsSurvivalConstruction(), "vault terminal bridge");
            StructureScanner.Scan scan = StructureScanner.scan(bridge, player, new TerminalSettings());
            check(scan.cells.size() >= 25, "vault scan cells missing " + scan.cells.size());
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("action", 1);
            tag.setInteger("x", x);
            tag.setInteger("y", y);
            tag.setInteger("z", z);
            tag.setInteger("side", 2);
            tag.setTag("settings", new TerminalSettings().write());
            // Each case represents a separate player click; bypass only the click-rate clock.
            java.lang.reflect.Field clock = BuildService.class.getDeclaredField("lastRequest");
            clock.setAccessible(true);
            ((java.util.Map<?, ?>) clock.get(BuildService.INSTANCE)).clear();
            BuildService.INSTANCE.enqueue(player, tag);
            long tick = world.getTotalWorldTime();
            BuildService.INSTANCE.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
            t.checkStructure();
            check(world.getTotalWorldTime() == tick, "world tick advanced inside build");
            check(t.formed, "instant build incomplete partial=" + partial);
            java.lang.reflect.Field jobs = BuildService.class.getDeclaredField("jobs");
            jobs.setAccessible(true);
            check(((java.util.Map<?, ?>) jobs.get(BuildService.INSTANCE)).isEmpty(), "build left queued rounds");
            if (partial) check(world.getTileEntity(x + 1, y, z + 2) instanceof TileAccess, "existing port replaced");
        } finally {
            player.capabilities.isCreativeMode = creative;
            player.inventory.setInventorySlotContents(player.inventory.currentItem, held);
            player.setPosition(px, py, pz);
            clear(world, x, y, z);
        }
    }

    static void clientView(WorldServer world) {
        TileController synced = new TileController();
        synced.xCoord = 20;
        synced.yCoord = 140;
        synced.zCoord = 20;
        synced.formed = true;
        // No World and no server-side bus data exist in a render view. Previous code called
        // inspectExclusive(controller) here and rejected this fully synchronized controller.
        IBlockAccess view = (IBlockAccess) Proxy.newProxyInstance(
            IBlockAccess.class.getClassLoader(),
            new Class<?>[] { IBlockAccess.class },
            (proxy, m, args) -> {
                if (m.getName()
                    .equals("getTileEntity")
                    || m.getName()
                        .equals("func_147438_o"))
                    return (int) args[0] == 20 && (int) args[1] == 140 && (int) args[2] == 20 ? synced : null;
                if (m.getName()
                    .equals("getBlockMetadata")
                    || m.getName()
                        .equals("func_72805_g"))
                    return 2;
                throw new AssertionError("render view requested unrelated state: " + m.getName());
            });
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) if (VaultGeometry.shell(dx, dy, dz))
                check(VaultStructure.formedOwner(view, 20 + dx, 140 + dy, 21 + dz) == synced, "missing shell skin");
        check(VaultStructure.formedOwner(view, 25, 140, 21) == null, "reskinned unrelated block");
        synced.formed = false;
        check(VaultStructure.formedOwner(view, 21, 140, 21) == null, "unformed shell still skinned");
    }

    private static void clear(WorldServer w, int x, int y, int z) {
        for (int dx = -3; dx <= 3; dx++)
            for (int dy = -3; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) w.setBlockToAir(x + dx, y + dy, z + dz);
    }
}
