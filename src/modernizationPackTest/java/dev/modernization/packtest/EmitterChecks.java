package dev.modernization.packtest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.google.gson.GsonBuilder;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.BooleanOperation;
import appeng.api.config.Settings;
import appeng.api.definitions.IBlockDefinition;
import appeng.api.definitions.IParts;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AEColor;
import appeng.container.implementations.ContainerAdvancedLevelEmitter;
import appeng.me.cache.NetworkMonitor;
import appeng.me.storage.NetworkInventoryHandler;
import appeng.parts.automation.PartAdvancedLevelEmitter;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import appeng.util.item.AEItemStackType;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.aeoptimizer.emitter.ChannelTotal;
import dev.aeoptimizer.emitter.EmitterTargets;

/** Powered quartz-fiber subnet with a real drive, two emitters, and independent parent storage. */
public final class EmitterChecks {

    private final MinecraftServer server = MinecraftServer.getServer();
    private final WorldServer world = server.worldServerForDimension(0);
    private final int x = world.getSpawnPoint().posX, y = 100, z = world.getSpawnPoint().posZ;
    private final FakePlayer player = FakePlayerFactory.getMinecraft(world);
    private final BaseActionSource source = new BaseActionSource();
    private final List<String> tests = new ArrayList<>();
    private TileDrive mainDrive, subDrive;
    private PartAdvancedLevelEmitter front, back;
    private IStorageGrid subnet;
    private int tick;
    private MachineChecks machines;
    private ItemStack removedCell;
    private final Map<String, Object> report = new LinkedHashMap<>();

    private void require(boolean ok, String description) {
        if (!ok) throw new AssertionError(description);
    }

    private ItemStack place(IBlockDefinition block, int dx) {
        ItemStack stack = block.maybeStack(1)
            .get();
        Block b = Block.getBlockFromItem(stack.getItem());
        require(world.setBlock(x + dx, y, z, b, stack.getItemDamage(), 3), "place " + dx);
        b.onBlockPlacedBy(world, x + dx, y, z, player, stack);
        return stack;
    }

    private TileDrive drive(int dx, boolean fluids) throws Exception {
        place(
            AEApi.instance()
                .definitions()
                .blocks()
                .drive(),
            dx);
        TileDrive d = (TileDrive) world.getTileEntity(x + dx, y, z);
        d.getInternalInventory()
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .items()
                    .cell1k()
                    .maybeStack(1)
                    .get());
        if (fluids) {
            Item cell = (Item) Class.forName("com.glodblock.github.loader.ItemAndBlockHolder")
                .getField("CELL1K")
                .get(null);
            d.getInternalInventory()
                .setInventorySlotContents(1, new ItemStack(cell));
            d.getInternalInventory()
                .setInventorySlotContents(2, new ItemStack(cell));
        }
        return d;
    }

    private IPartHost cable(int dx) {
        place(
            AEApi.instance()
                .definitions()
                .blocks()
                .multiPart(),
            dx);
        IPartHost host = (IPartHost) world.getTileEntity(x + dx, y, z);
        host.addPart(
            AEApi.instance()
                .definitions()
                .parts()
                .cableGlass()
                .stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN,
            player);
        return host;
    }

    private void setup() throws Exception {
        IParts parts = AEApi.instance()
            .definitions()
            .parts();
        mainDrive = drive(0, false);
        place(
            AEApi.instance()
                .definitions()
                .blocks()
                .energyCellCreative(),
            -1);
        IPartHost first = cable(1), second = cable(2);
        first.addPart(
            parts.quartzFiber()
                .maybeStack(1)
                .get(),
            ForgeDirection.EAST,
            player);
        second.addPart(
            parts.advancedLevelEmitter()
                .maybeStack(1)
                .get(),
            ForgeDirection.NORTH,
            player);
        second.addPart(
            parts.advancedLevelEmitter()
                .maybeStack(1)
                .get(),
            ForgeDirection.SOUTH,
            player);
        front = (PartAdvancedLevelEmitter) second.getPart(ForgeDirection.NORTH);
        back = (PartAdvancedLevelEmitter) second.getPart(ForgeDirection.SOUTH);
        subDrive = drive(3, true);
    }

    private void row(PartAdvancedLevelEmitter e, int slot, int mode, boolean invert, long threshold) {
        ((EmitterTargets) e).setTarget(slot, mode);
        e.setSlotInverted(slot, invert);
        e.setReportingValue(slot, threshold);
        e.setSlotActive(slot, true);
    }

    private void state(String label, int expected, long items, long fluids) {
        int actual = world.getBlock(x + 2, y, z)
            .isProvidingWeakPower(world, x + 2, y, z, ForgeDirection.SOUTH.ordinal());
        require(actual == expected && front.isProvidingStrongPower() == expected, label + " signal=" + actual);
        require(
            front.getStoredAmount(0) == items && front.getStoredAmount(5) == fluids,
            label + " counts=" + front.getStoredAmount(0) + "/" + front.getStoredAmount(5));
        tests.add(label + " " + items + " items, " + fluids + " mB, signal " + actual);
    }

    private void add(Item item, int n) {
        require(
            subnet.getItemInventory()
                .injectItems(AEItemStack.create(new ItemStack(item, n)), Actionable.MODULATE, source) == null,
            "insert");
    }

    private void remove(Item item, int n) {
        require(
            subnet.getItemInventory()
                .extractItems(AEItemStack.create(new ItemStack(item, n)), Actionable.MODULATE, source)
                .getStackSize() == n,
            "extract");
    }

    private void liquid(Fluid fluid, int n, boolean insert) {
        AEFluidStack stack = AEFluidStack.create(new FluidStack(fluid, n));
        if (insert) {
            appeng.api.storage.data.IAEFluidStack left = subnet.getFluidInventory()
                .injectItems(stack, Actionable.MODULATE, source);
            require(
                left == null || left.getStackSize() == 0,
                "fluid insert " + fluid.getName()
                    + " returned "
                    + (left == null ? 0 : left.getStackSize())
                    + " cells="
                    + subDrive.getCellArray(appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE)
                        .size());
        } else require(
            subnet.getFluidInventory()
                .extractItems(stack, Actionable.MODULATE, source)
                .getStackSize() == n,
            "fluid extract");
    }

    private void checkSharedScans() {
        int[] reads = { 0, 0 };
        long[] displayed = { -1, -1 };
        IItemList<IAEItemStack> stock = AEItemStackType.ITEM_STACK_TYPE.createList();
        stock.add(
            AEItemStack.create(new ItemStack(Items.iron_ingot))
                .setStackSize(Long.MAX_VALUE));
        stock.add(AEItemStack.create(new ItemStack(Items.apple, 5)));
        NetworkInventoryHandler<IAEItemStack> handler = new NetworkInventoryHandler<IAEItemStack>(
            AEItemStackType.ITEM_STACK_TYPE,
            null) {

            @Override
            public IAEItemStack getAvailableItem(IAEItemStack key, int iteration) {
                reads[1]++;
                return stock.findPrecise(key);
            }
        };
        NetworkMonitor<IAEItemStack> monitor = new NetworkMonitor<IAEItemStack>(null, AEItemStackType.ITEM_STACK_TYPE) {

            @Override
            public IMEInventoryHandler<IAEItemStack> getHandler() {
                return handler;
            }

            @Override
            public IItemList<IAEItemStack> getStorageList() {
                reads[0]++;
                return stock;
            }
        };
        ChannelTotal total = new ChannelTotal();
        ChannelTotal.Listener first = (channel, value) -> displayed[0] = value;
        ChannelTotal.Listener second = (channel, value) -> displayed[1] = value;
        total.subscribe(first);
        total.flush(monitor);
        total.subscribe(second);
        for (int i = 0; i < 1000; i++) total.flush(monitor);
        require(reads[0] == 1 && displayed[0] == Long.MAX_VALUE && displayed[1] == Long.MAX_VALUE, "shared baseline");
        IAEItemStack iron = AEItemStack.create(new ItemStack(Items.iron_ingot));
        stock.findPrecise(iron)
            .setStackSize(Long.MAX_VALUE - 8);
        for (int i = 0; i < 100; i++) total.changedKey(iron);
        total.flush(monitor);
        require(
            reads[0] == 1 && reads[1] == 1 && displayed[0] == Long.MAX_VALUE - 3 && displayed[1] == displayed[0],
            "duplicate notifications use one absolute quantity after saturation");
        stock.findPrecise(iron)
            .setStackSize(0);
        total.changedKey(iron);
        for (int i = 0; i < 5; i++) total.flush(monitor);
        require(reads[0] == 1 && reads[1] == 2 && displayed[0] == 5, "last item of a type removed");
        for (int i = 0; i < 50; i++) {
            total.invalidate();
            total.flush(monitor);
        }
        require(reads[0] == 11 && displayed[0] == 5, "50 consecutive topology updates coalesced");
        total.unsubscribe(first);
        total.unsubscribe(second);
        total.invalidate();
        for (int i = 0; i < 10; i++) total.flush(monitor);
        require(reads[0] == 11 && reads[1] == 2, "unobserved channel");
        tests.add("shared totals: saturation, repeated keys, removal, idle ticks and bounded topology reads");
    }

    private void runStep() throws Exception {
        if (tick > 220) {
            if (machines.step(tick - 220)) finish(true);
            return;
        }
        switch (tick) {
            case 1:
                setup();
                break;
            case 100:
                require(
                    front.getProxy()
                        .isActive()
                        && back.getProxy()
                            .isActive(),
                    "unpowered subnet");
                require(
                    mainDrive.getProxy()
                        .getGrid()
                        != front.getProxy()
                            .getGrid(),
                    "quartz isolation");
                require(
                    mainDrive.getProxy()
                        .getStorage()
                        .getItemInventory()
                        .injectItems(
                            AEItemStack.create(new ItemStack(Items.gold_ingot, 10)),
                            Actionable.MODULATE,
                            source)
                        == null,
                    "parent storage");
                subnet = subDrive.getProxy()
                    .getStorage();
                front.getConfigManager()
                    .putSetting(Settings.ADVANCED_LEVEL_EMITTER_LOGIC, BooleanOperation.AND);
                row(front, 0, EmitterTargets.ITEMS, true, 1);
                row(front, 5, EmitterTargets.FLUIDS, true, 1);
                row(back, 2, EmitterTargets.ITEMS, false, 64);
                break;
            case 110:
                state("empty subnet, nonempty parent", 15, 0, 0);
                add(Items.iron_ingot, 40);
                add(Items.apple, 24);
                break;
            case 120:
                state("two item types", 0, 64, 0);
                require(back.isProvidingStrongPower() == 15 && back.getStoredAmount(2) == 64, "second emitter");
                liquid(FluidRegistry.WATER, 1000, true);
                liquid(FluidRegistry.LAVA, 250, true);
                break;
            case 130:
                state("mixed subnet", 0, 64, 1250);
                remove(Items.iron_ingot, 40);
                remove(Items.apple, 24);
                break;
            case 140:
                state("only fluid remains", 0, 0, 1250);
                require(back.isProvidingStrongPower() == 0, "shared total dropped");
                liquid(FluidRegistry.WATER, 1000, false);
                liquid(FluidRegistry.LAVA, 250, false);
                break;
            case 150:
                state("last fluid drained", 15, 0, 0);
                row(front, 1, EmitterTargets.EXACT, false, 5);
                front.getAEInventoryByName(StorageName.CONFIG)
                    .putAEStackInSlot(1, AEItemStack.create(new ItemStack(Items.apple)));
                front.setSlotActive(0, false);
                front.setSlotActive(5, false);
                add(Items.apple, 5);
                break;
            case 160:
                require(front.getStoredAmount(1) == 5 && front.isProvidingStrongPower() == 15, "specific item");
                for (int i = 0; i < 6; i++) ((EmitterTargets) front).setTarget(i, i % 3);
                NBTTagCompound n = new NBTTagCompound();
                front.writeToNBT(n);
                PartAdvancedLevelEmitter copy = new PartAdvancedLevelEmitter(
                    AEApi.instance()
                        .definitions()
                        .parts()
                        .advancedLevelEmitter()
                        .maybeStack(1)
                        .get());
                copy.readFromNBT(n);
                for (int i = 0; i < 6; i++) require(((EmitterTargets) copy).target(i) == i % 3, "persist slot " + i);
                n.removeTag("modernizationEmitterTargets");
                copy.readFromNBT(n);
                for (int i = 0; i < 6; i++)
                    require(((EmitterTargets) copy).target(i) == EmitterTargets.EXACT, "legacy default");
                tests.add("specific item, six independent persisted targets, legacy defaults");
                ContainerAdvancedLevelEmitter menu = new ContainerAdvancedLevelEmitter(player.inventory, front);
                menu.detectAndSendChanges();
                for (int i = 0; i < 6; i++) require(((EmitterTargets) menu).target(i) == i % 3, "container mode " + i);
                row(front, 1, EmitterTargets.EXACT, false, 500);
                front.getAEInventoryByName(StorageName.CONFIG)
                    .putAEStackInSlot(1, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)));
                liquid(FluidRegistry.LAVA, 1000, true);
                break;
            case 170:
                require(
                    front.getStoredAmount(1) == 0 && front.isProvidingStrongPower() == 0,
                    "specific water ignores lava");
                liquid(FluidRegistry.WATER, 500, true);
                break;
            case 180:
                require(
                    front.getStoredAmount(1) == 500 && front.isProvidingStrongPower() == 15,
                    "specific water threshold");
                tests.add(
                    "specific fluid ignores lava and reaches threshold at exactly 500 mB water; six container modes synchronized");
                row(back, 2, EmitterTargets.ITEMS, false, 5);
                removedCell = subDrive.getInternalInventory()
                    .getStackInSlot(0);
                subDrive.getInternalInventory()
                    .setInventorySlotContents(0, null);
                break;
            case 200:
                require(back.getStoredAmount(2) == 0 && back.isProvidingStrongPower() == 0, "removed filled cell");
                subDrive.getInternalInventory()
                    .setInventorySlotContents(0, removedCell);
                break;
            case 220:
                require(back.getStoredAmount(2) == 5 && back.isProvidingStrongPower() == 15, "returned filled cell");
                tests.add("removing and reinstalling a filled storage cell updates totals and redstone");
                checkSharedScans();
                machines = new MachineChecks(world, player, subDrive, x, y, z, tests);
                break;
            default:
                break;
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || report.containsKey("passed")) return;
        tick++;
        try {
            runStep();
        } catch (Throwable t) {
            t.printStackTrace();
            report.put("error", t.toString());
            finish(false);
        }
    }

    private void finish(boolean success) throws Exception {
        report.put("passed", success);
        report.put("tests", tests);
        Files.write(
            server.getFile("emitter-tests.json")
                .toPath(),
            new GsonBuilder().setPrettyPrinting()
                .create()
                .toJson(report)
                .getBytes(StandardCharsets.UTF_8));
        server.initiateShutdown();
    }
}
