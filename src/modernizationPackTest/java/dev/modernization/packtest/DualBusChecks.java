package dev.modernization.packtest;

import static net.minecraftforge.common.util.ForgeDirection.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.google.gson.GsonBuilder;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.IBlockDefinition;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartItemStack;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
import appeng.tile.storage.TileDrive;
import appeng.util.IterationCounter;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.aeoptimizer.storage.DualStorageBus;
import dev.aeoptimizer.storage.ItemDualStorageBus;

/** Independent powered grids bridged by a single part facing a flat dual interface. */
public final class DualBusChecks {

    private final MinecraftServer server = MinecraftServer.getServer();
    private final WorldServer world = server.worldServerForDimension(0);
    private final int x = world.getSpawnPoint().posX, y = 100, z = world.getSpawnPoint().posZ;
    private final FakePlayer player = FakePlayerFactory.getMinecraft(world);
    private final BaseActionSource source = new BaseActionSource();
    private final List<String> tests = new ArrayList<>();
    private final Map<String, Object> result = new LinkedHashMap<>();
    private final AEItemStack iron = AEItemStack.create(new ItemStack(Items.iron_ingot, 10));
    private final AEItemStack gold = AEItemStack.create(new ItemStack(Items.gold_ingot, 5));
    private final AEFluidStack water = AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1000));
    private final AEFluidStack lava = AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 250));
    private int ticks;
    private IPartHost parent, child;
    private TileDrive drive;
    private DualStorageBus bus;
    private IStorageGrid main, sub;

    private void place(int dx, IBlockDefinition def) {
        ItemStack stack = def.maybeStack(1)
            .get();
        Block block = Block.getBlockFromItem(stack.getItem());
        world.setBlock(x + dx, y, z, block, stack.getItemDamage(), 3);
        block.onBlockPlacedBy(world, x + dx, y, z, player, stack);
    }

    private IPartHost cable(int dx) {
        place(
            dx,
            AEApi.instance()
                .definitions()
                .blocks()
                .multiPart());
        IPartHost host = (IPartHost) world.getTileEntity(x + dx, y, z);
        host.addPart(
            AEApi.instance()
                .definitions()
                .parts()
                .cableGlass()
                .stack(AEColor.Transparent, 1),
            UNKNOWN,
            player);
        return host;
    }

    private void build() {
        place(
            -1,
            AEApi.instance()
                .definitions()
                .blocks()
                .energyCellCreative());
        parent = cable(0);
        child = cable(1);
        child.addPart(new ItemStack(ItemAndBlockHolder.FLUID_INTERFACE), WEST, player);
        parent.addPart(new ItemStack(ItemDualStorageBus.INSTANCE), EAST, player);
        bus = (DualStorageBus) parent.getPart(EAST);
        place(
            2,
            AEApi.instance()
                .definitions()
                .blocks()
                .drive());
        drive = (TileDrive) world.getTileEntity(x + 2, y, z);
        drive.getInternalInventory()
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .items()
                    .cell1k()
                    .maybeStack(1)
                    .get());
        drive.getInternalInventory()
            .setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL1K));
        drive.getInternalInventory()
            .setInventorySlotContents(2, new ItemStack(ItemAndBlockHolder.CELL1K));
        place(
            3,
            AEApi.instance()
                .definitions()
                .blocks()
                .energyCellCreative());
    }

    private void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        tests.add(label);
    }

    private <T extends IAEStack<T>> void insert(IMEInventory<T> inv, T stack) {
        if (inv.injectItems(stack.copy(), Actionable.MODULATE, source) != null)
            throw new AssertionError("insert " + stack);
    }

    private <T extends IAEStack<T>> long amount(IMEInventory<T> inv, T key) {
        T stack = inv.getAvailableItem(key, IterationCounter.fetchNewId());
        return stack == null ? 0 : stack.getStackSize();
    }

    private String label(IAEStack<?> stack) {
        if (stack instanceof appeng.api.storage.data.IAEFluidStack)
            return ((appeng.api.storage.data.IAEFluidStack) stack).getFluidStack()
                .getFluid()
                .getName();
        return stack.toString();
    }

    private <T extends IAEStack<T>> void blocked(IMEInventory<T> inv, T stack) {
        T left = inv.injectItems(stack.copy(), Actionable.MODULATE, source);
        check(
            left != null && left.getStackSize() == stack.getStackSize()
                && inv.extractItems(stack.copy(), Actionable.MODULATE, source) == null
                && amount(inv, stack) == 0,
            "blocked " + label(stack));
    }

    private void filter(int slot, IAEStack<?> key) {
        bus.getAEInventoryByName(StorageName.CONFIG)
            .putAEStackInSlot(slot, key);
    }

    private appeng.container.implementations.ContainerStorageBus openContainer() {
        return new appeng.container.implementations.ContainerStorageBus(player.inventory, bus) {

            // FakePlayer has no network connection. Slot writes and the partition action still use AE's container.
            @Override
            public void detectAndSendChanges() {}
        };
    }

    private void step() throws Exception {
        switch (ticks) {
            case 5:
                build();
                break;
            case 120:
                check(
                    bus.getProxy()
                        .isActive()
                        && drive.getProxy()
                            .isActive()
                        && bus.getProxy()
                            .getGrid()
                            != drive.getProxy()
                                .getGrid(),
                    "two independent powered grids");
                main = bus.getProxy()
                    .getStorage();
                sub = drive.getProxy()
                    .getStorage();
                insert(main.getItemInventory(), iron);
                insert(main.getFluidInventory(), water);
                insert(sub.getItemInventory(), gold);
                insert(sub.getFluidInventory(), lava);
                break;
            case 135:
                result.put("initial_item_query", amount(sub.getItemInventory(), iron));
                result.put("initial_fluid_query", amount(sub.getFluidInventory(), water));
                check(
                    amount(sub.getItemInventory(), iron) == 10 && amount(sub.getFluidInventory(), water) == 1000,
                    "both writes reach remote cells");
                filter(0, iron);
                filter(1, water);
                break;
            case 160:
                blocked(main.getItemInventory(), gold);
                blocked(main.getFluidInventory(), lava);
                check(
                    amount(main.getItemInventory(), iron) == 10 && amount(main.getFluidInventory(), water) == 1000,
                    "mixed list exposes both selected types");
                check(
                    main.getItemInventory()
                        .extractItems(iron.copy(), Actionable.MODULATE, source)
                        .getStackSize() == 10
                        && main.getFluidInventory()
                            .extractItems(water.copy(), Actionable.MODULATE, source)
                            .getStackSize() == 1000,
                    "remote extraction of both types");
                check(
                    amount(sub.getItemInventory(), iron) == 0 && amount(sub.getFluidInventory(), water) == 0,
                    "remote cells depleted exactly once");
                insert(main.getItemInventory(), iron);
                insert(main.getFluidInventory(), water);
                NBTTagCompound saved = new NBTTagCompound();
                bus.writeToNBT(saved);
                DualStorageBus clone = new DualStorageBus(new ItemStack(ItemDualStorageBus.INSTANCE));
                clone.readFromNBT(saved);
                check(
                    water.equals(
                        clone.getAEInventoryByName(StorageName.CONFIG)
                            .getAEStackInSlot(1)),
                    "fluid filter survives world NBT");
                clone = ItemDualStorageBus.INSTANCE.createPartFromItemStack(bus.getItemStack(PartItemStack.Wrench));
                check(
                    iron.equals(
                        clone.getAEInventoryByName(StorageName.CONFIG)
                            .getAEStackInSlot(0))
                        && water.equals(
                            clone.getAEInventoryByName(StorageName.CONFIG)
                                .getAEStackInSlot(1)),
                    "wrenched part retains mixed filters");
                filter(1, null);
                break;
            case 200:
                blocked(main.getFluidInventory(), water);
                filter(0, water);
                break;
            case 240:
                blocked(main.getItemInventory(), iron);
                filter(0, null);
                break;
            case 280:
                check(
                    amount(main.getItemInventory(), gold) == 5 && amount(main.getFluidInventory(), lava) == 250,
                    "cleared filter restores both channels");
                appeng.container.implementations.ContainerStorageBus gui = openContainer();
                gui.partition(false);
                boolean foundItem = false, foundFluid = false;
                for (int i = 0; i < 18; i++) {
                    IAEStack<?> entry = gui.getConfig()
                        .getAEStackInSlot(i);
                    foundItem |= gold.equals(entry);
                    foundFluid |= lava.equals(entry);
                }
                result.put("partition_found_item", foundItem);
                result.put("partition_found_fluid", foundFluid);
                check(foundItem && foundFluid, "native partition button marks both items and fluids");
                bus.getConfigManager()
                    .putSetting(appeng.api.config.Settings.ACCESS, appeng.api.config.AccessRestriction.READ);
                break;
            case 310:
                check(
                    main.getFluidInventory()
                        .injectItems(water.copy(), Actionable.SIMULATE, source) != null
                        && main.getItemInventory()
                            .injectItems(iron.copy(), Actionable.SIMULATE, source) != null,
                    "read-only access blocks writes on both channels");
                bus.getConfigManager()
                    .putSetting(appeng.api.config.Settings.ACCESS, appeng.api.config.AccessRestriction.READ_WRITE);
                openContainer().clear();
                filter(0, iron);
                bus.getInventoryByName("upgrades")
                    .setInventorySlotContents(
                        0,
                        AEApi.instance()
                            .definitions()
                            .materials()
                            .cardInverter()
                            .maybeStack(1)
                            .get());
                break;
            case 350:
                blocked(main.getItemInventory(), iron);
                check(
                    amount(main.getItemInventory(), gold) == 5 && amount(main.getFluidInventory(), water) == 1000,
                    "item-only blacklist passes unmarked items and all fluids");
                world.setBlockToAir(x + 1, y, z);
                break;
            case 400:
                check(
                    amount(main.getItemInventory(), gold) == 0 && amount(main.getFluidInventory(), lava) == 0,
                    "removing target removes both views");
                world.setBlock(x + 1, y, z, net.minecraft.init.Blocks.chest, 0, 3);
                break;
            case 455:
                insert(main.getItemInventory(), gold);
                net.minecraft.tileentity.TileEntityChest chest = (net.minecraft.tileentity.TileEntityChest) world
                    .getTileEntity(x + 1, y, z);
                check(
                    chest.getStackInSlot(0).stackSize == 5 && chest.getStackInSlot(0)
                        .getItem() == Items.gold_ingot,
                    "ordinary chest receives items directly");
                blocked(main.getFluidInventory(), water);
                chest.setInventorySlotContents(0, new ItemStack(Items.gold_ingot, 12));
                break;
            case 520:
                check(amount(main.getItemInventory(), gold) == 12, "external chest changes reach the grid");
                check(
                    main.getItemInventory()
                        .extractItems(gold.copy(), Actionable.MODULATE, source)
                        .getStackSize() == 5,
                    "extract items from ordinary chest");
                finish(true);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || result.containsKey("passed")) return;
        ticks++;
        try {
            step();
        } catch (Throwable t) {
            t.printStackTrace();
            result.put("error", t.toString());
            finish(false);
        }
    }

    private void finish(boolean passed) throws Exception {
        result.put("passed", passed);
        result.put("tests", tests);
        Files.write(
            server.getFile("dual-bus-tests.json")
                .toPath(),
            new GsonBuilder().setPrettyPrinting()
                .create()
                .toJson(result)
                .getBytes(StandardCharsets.UTF_8));
        server.initiateShutdown();
    }
}
