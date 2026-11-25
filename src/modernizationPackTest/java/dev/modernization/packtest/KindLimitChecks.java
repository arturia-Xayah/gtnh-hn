package dev.modernization.packtest;

import static net.minecraftforge.common.util.ForgeDirection.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.*;
import appeng.api.util.AEColor;
import appeng.container.ContainerNull;
import appeng.parts.misc.PartStorageBus;
import appeng.tile.misc.TileInterface;
import appeng.tile.storage.TileDrive;
import appeng.util.IterationCounter;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.aeoptimizer.storage.ItemDualStorageBus;
import dev.aeoptimizer.storage.KindLimitCard;
import gregtech.api.util.GTUtility;

public final class KindLimitChecks {

    private final MinecraftServer server = MinecraftServer.getServer();
    private final WorldServer w = server.worldServerForDimension(0);
    private final int x = w.getSpawnPoint().posX, y = 108, z = w.getSpawnPoint().posZ;
    private final FakePlayer player = FakePlayerFactory.getMinecraft(w);
    private final BaseActionSource source = new BaseActionSource();
    private final PartStorageBus[] buses = new PartStorageBus[4];
    private final TileDrive[] drives = new TileDrive[4];
    private final IStorageGrid[] sub = new IStorageGrid[4];
    private IStorageGrid distribution;
    private TileInterface sender;
    private final List<String> tests = new ArrayList<>();
    private final Map<String, Object> report = new LinkedHashMap<>();
    private final Map<String, List<Double>> timings = new TreeMap<>();
    private final IAEItemStack iron = item(Items.iron_ingot), gold = item(Items.gold_ingot), copper = item(Items.brick);
    private final IAEFluidStack water = AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1000));
    private final IAEFluidStack lava = AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1000));
    private ICraftingPatternDetails pattern1, pattern2;
    private int tick;
    private boolean done;

    private static IAEItemStack item(Item i) {
        return AEItemStack.create(new ItemStack(i));
    }

    private void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        tests.add(message);
    }

    private void block(int dx, int dy, int dz, IBlockDefinition b) {
        ItemStack s = b.maybeStack(1)
            .get();
        Block block = Block.getBlockFromItem(s.getItem());
        w.setBlock(x + dx, y + dy, z + dz, block, s.getItemDamage(), 3);
        block.onBlockPlacedBy(w, x + dx, y + dy, z + dz, player, s);
    }

    private IPartHost cable(int dx, int dz) {
        block(
            dx,
            0,
            dz,
            AEApi.instance()
                .definitions()
                .blocks()
                .multiPart());
        IPartHost host = (IPartHost) w.getTileEntity(x + dx, y, z + dz);
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

    private void card(PartStorageBus bus, boolean on) {
        bus.getInventoryByName("upgrades")
            .setInventorySlotContents(0, on ? new ItemStack(KindLimitCard.INSTANCE) : null);
    }

    private void build() throws Exception {
        for (int k = 0; k <= 9; k++) cable(0, k);
        block(
            0,
            0,
            -1,
            AEApi.instance()
                .definitions()
                .blocks()
                .energyCellCreative());
        for (int lane = 0; lane < 4; lane++) {
            int dz = lane * 3;
            // Last slice has three additional network boundaries before the drive.
            int links = lane == 3 ? 4 : 1;
            for (int n = 0; n < links; n++) {
                int dx = n * 3;
                IPartHost parent = n == 0 ? (IPartHost) w.getTileEntity(x, y, z + dz) : cable(dx, dz);
                IPartHost child = cable(dx + 1, dz);
                child.addPart(new ItemStack(ItemAndBlockHolder.FLUID_INTERFACE), WEST, player);
                ItemStack part = n == 0 && lane == 1 ? new ItemStack(ItemAndBlockHolder.FLUID_STORAGE_BUS)
                    : n == 0 && lane == 2 ? AEApi.instance()
                        .definitions()
                        .parts()
                        .storageBus()
                        .maybeStack(1)
                        .get() : new ItemStack(ItemDualStorageBus.INSTANCE);
                parent.addPart(part, EAST, player);
                PartStorageBus bus = (PartStorageBus) parent.getPart(EAST);
                if (n == 0) {
                    buses[lane] = bus;
                    bus.setPriority(lane == 1 ? 110 : 100 - lane);
                    card(bus, true);
                }
                block(
                    dx + 1,
                    1,
                    dz,
                    AEApi.instance()
                        .definitions()
                        .blocks()
                        .energyCellCreative());
                if (n + 1 < links) cable(dx + 2, dz);
                else {
                    block(
                        dx + 2,
                        0,
                        dz,
                        AEApi.instance()
                            .definitions()
                            .blocks()
                            .drive());
                    drives[lane] = (TileDrive) w.getTileEntity(x + dx + 2, y, z + dz);
                    for (int i = 0; i < 9; i++) drives[lane].getInternalInventory()
                        .setInventorySlotContents(
                            i,
                            AEApi.instance()
                                .definitions()
                                .items()
                                .cell64k()
                                .maybeStack(1)
                                .get());
                    drives[lane].getInternalInventory()
                        .setInventorySlotContents(9, new ItemStack(ItemAndBlockHolder.CELL1K));
                }
            }
        }
        block(
            -3,
            0,
            0,
            AEApi.instance()
                .definitions()
                .blocks()
                .energyCellCreative());
        w.setBlock(x - 2, y, z, ItemAndBlockHolder.INTERFACE, 0, 3);
        sender = (TileInterface) w.getTileEntity(x - 2, y, z);
        sender.setOrientation(UP, WEST);
        w.setBlock(x - 1, y, z, ItemAndBlockHolder.INTERFACE, 0, 3);
        Item item = null;
        for (Object o : Item.itemRegistry) if (o.getClass()
            .getSimpleName()
            .equals("ItemFluidEncodedPattern")) item = (Item) o;
        pattern1 = pattern(item, 0, iron, water);
        pattern2 = pattern(item, 1, gold, iron);
    }

    private ICraftingPatternDetails pattern(Item item, int slot, IAEStack<?>... args) throws Exception {
        ItemStack encoded = SmartBlockingChecks.encode(item, args);
        sender.getInterfaceDuality()
            .getPatterns()
            .setInventorySlotContents(slot, encoded);
        return ((ICraftingPatternItem) item).getPatternForItem(encoded, w);
    }

    private boolean push(ICraftingPatternDetails pattern) {
        IAEStack<?>[] slots = pattern.getAEInputs();
        MEInventoryCrafting table = new MEInventoryCrafting(new ContainerNull(), slots.length, 1);
        for (int i = 0; i < slots.length; i++) if (slots[i] != null) table.setInventorySlotContents(i, slots[i].copy());
        return sender.getInterfaceDuality()
            .pushPattern(pattern, table);
    }

    private <T extends IAEStack<T>> long amount(IMEInventory<T> inv, T key) {
        if (inv instanceof appeng.me.cache.NetworkMonitor) inv = ((appeng.me.cache.NetworkMonitor<T>) inv).getHandler();
        T got = inv.getAvailableItem(key, IterationCounter.fetchNewId());
        return got == null ? 0 : got.getStackSize();
    }

    private <T extends IAEStack<T>> void put(IMEInventory<T> inv, T key) {
        if (inv.injectItems(key.copy(), Actionable.MODULATE, source) != null)
            throw new AssertionError("could not insert " + key);
    }

    private <T extends IAEStack<T>> void take(IMEInventory<T> inv, T key, long count) {
        T got = inv.extractItems(
            key.copy()
                .setStackSize(count),
            Actionable.MODULATE,
            source);
        if (got == null || got.getStackSize() != count)
            throw new AssertionError("could not extract " + key + " x" + count);
    }

    private void route() throws Exception {
        distribution = buses[0].getProxy()
            .getStorage();
        for (int i = 0; i < 4; i++) {
            sub[i] = drives[i].getProxy()
                .getStorage();
            check(
                sub[i] != distribution && buses[i].getInstalledUpgrades(KindLimitCard.TYPE) == 1
                    && buses[i].getProxy()
                        .isActive(),
                "powered independent lane " + i);
            put(
                sub[i].getItemInventory(),
                AEItemStack.create(
                    GTUtility.getIntegratedCircuit(i + 1)
                        .copy())
                    .setStackSize(1));
        }
        List<String> deep = new ArrayList<>();
        for (int j = 0; j < 4; j++) {
            PartStorageBus b = (PartStorageBus) ((IPartHost) w.getTileEntity(x + 3 * j, y, z + 9)).getPart(EAST);
            appeng.me.storage.StorageBusInventoryHandler h = (appeng.me.storage.StorageBusInventoryHandler) b
                .getInternalHandler();
            deep.add(
                "active=" + b.getProxy()
                    .isActive()
                    + "; underlying="
                    + (h == null ? "none"
                        : h.getInventory()
                            .getClass()
                            .getName())
                    + "; simulate="
                    + (h != null && h.injectItems(iron.copy(), Actionable.SIMULATE, source) == null));
        }
        report.put("deep_path", deep);
        long start = w.getTotalWorldTime();
        check(
            distribution.getItemInventory()
                .injectItems(copper.copy(), Actionable.SIMULATE, source) == null,
            "simulate empty lane without reserving it");
        check(push(pattern1), "native dual interface pushed first encoded pattern");
        snapshot("after_first");
        check(push(pattern2), "native dual interface pushed second encoded pattern");
        snapshot("after_second");
        check(
            sender.getInterfaceDuality()
                .getWaitingToSend() == null,
            "both batches fully delivered (no remaining interface buffer)");
        check(
            amount(sub[0].getItemInventory(), iron) == 2 && amount(sub[0].getItemInventory(), gold) == 0
                && amount(sub[0].getFluidInventory(), water) == 0,
            "lane 1 holds both iron entries only");
        check(
            amount(sub[1].getFluidInventory(), water) == 1000 && amount(sub[2].getItemInventory(), gold) == 1,
            "water and gold landed in separate slices");
        put(distribution.getFluidInventory(), lava);
        check(
            amount(sub[3].getFluidInventory(), lava) == 1000,
            "four-hop lane accepts overflow without cross-contamination");
        IAEItemStack returned = distribution.getItemInventory()
            .injectItems(
                copper.copy()
                    .setStackSize(17),
                Actionable.MODULATE,
                source);
        check(
            returned != null && returned.getStackSize() == 17,
            "all occupied slices return all 17 units of a fifth type");
        take(distribution.getItemInventory(), iron, 2);
        put(distribution.getFluidInventory(), lava);
        check(
            amount(sub[0].getFluidInventory(), lava) == 1000,
            "drained item lane switches to fluid in same tick despite resident circuit");
        take(distribution.getFluidInventory(), lava, 1000);
        // Extraction can prefer the low-priority remote lane. Drain each target explicitly for the next case.
        for (int i : new int[] { 0, 3 }) {
            long n = amount(sub[i].getFluidInventory(), lava);
            if (n > 0) take(sub[i].getFluidInventory(), lava, n);
        }
        put(sub[0].getItemInventory(), iron);
        put(sub[0].getItemInventory(), gold);
        check(
            buses[0].getInternalHandler()
                .injectItems(iron.copy(), Actionable.MODULATE, source) != null,
            "externally mixed lane refuses existing type");
        take(sub[0].getItemInventory(), gold, 1);
        put(buses[0].getInternalHandler(), iron);
        take(sub[0].getItemInventory(), iron, 2);
        ItemStack named = new ItemStack(Items.iron_ingot);
        named.setStackDisplayName("rename-1");
        IAEItemStack otherIron = AEItemStack.create(named);
        put(sub[3].getItemInventory(), otherIron);
        check(
            buses[3].getInternalHandler()
                .injectItems(iron.copy(), Actionable.MODULATE, source) != null,
            "external write four grids away immediately prevents insertion of different NBT");
        take(sub[3].getItemInventory(), otherIron, 1);
        put(buses[3].getInternalHandler(), iron);
        take(sub[3].getItemInventory(), iron, 1);
        check(w.getTotalWorldTime() == start, "all patterns, inserts and switches occurred in one server tick");
    }

    private void snapshot(String phase) {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < 4; i++) rows.add(
            "iron=" + amount(sub[i].getItemInventory(), iron)
                + ", gold="
                + amount(sub[i].getItemInventory(), gold)
                + ", water="
                + amount(sub[i].getFluidInventory(), water)
                + ", bus="
                + buses[i].getInternalHandler()
                    .getClass()
                    .getSimpleName()
                + ", priority="
                + buses[i].getPriority());
        report.put(phase, rows);
        report.put(
            phase + "_waiting",
            String.valueOf(
                sender.getInterfaceDuality()
                    .getWaitingToSend()));
    }

    private IAEItemStack namedCircuit(int id) {
        ItemStack s = GTUtility.getIntegratedCircuit(id % 25)
            .copy();
        s.stackSize = 1;
        s.setStackDisplayName("fixture-" + id);
        return AEItemStack.create(s);
    }

    private final Map<String, long[]> indexReads = new TreeMap<>();

    private long[] readCounters(IMEInventory<?> handler, boolean requireIndex) throws ReflectiveOperationException {
        java.lang.reflect.Field index = handler.getClass()
            .getDeclaredField("kinds");
        index.setAccessible(true);
        Object state = index.get(handler);
        if (requireIndex && state == null) throw new AssertionError(
            "bus did not cache target monitor: " + ((dev.aeoptimizer.storage.KindLimitedHandler) handler).getInventory()
                .getClass());
        long[] counts = new long[2];
        if (state != null) {
            String[] fields = { "fullReads", "keyReads" };
            for (int i = 0; i < fields.length; i++) {
                java.lang.reflect.Field field = state.getClass()
                    .getDeclaredField(fields[i]);
                field.setAccessible(true);
                counts[i] = field.getLong(state);
            }
        }
        return counts;
    }

    private void measure(int lane, String phase) throws ReflectiveOperationException {
        IMEInventory<IAEItemStack> h = buses[lane].getInternalHandler();
        boolean limited = h instanceof dev.aeoptimizer.storage.KindLimitedHandler;
        checkBenchState(limited == phase.endsWith("_card"), "upgrade state mismatch " + phase);
        String label = phase + "_hops_" + (lane == 3 ? 4 : 1);
        long[] before = limited ? readCounters(h, false) : null;
        long start = System.nanoTime();
        for (int i = 0; i < 48; i++) put(h, iron);
        double us = (System.nanoTime() - start) / 48000.0;
        if (limited) {
            long[] after = readCounters(h, true), sums = indexReads.computeIfAbsent(label, k -> new long[2]);
            for (int i = 0; i < after.length; i++) sums[i] += after[i] - before[i];
        }
        timings.computeIfAbsent(label, k -> new ArrayList<>())
            .add(us);
    }

    private void checkBenchState(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }

    private void step() throws Exception {
        if (tick == 5) build();
        if (tick == 180) route();
        if (tick == 185) {
            put(sub[0].getItemInventory(), iron);
            put(sub[3].getItemInventory(), iron);
        }
        if (tick == 190 || tick == 452) for (int i : new int[] { 0, 3 }) card(buses[i], false);
        if (tick == 260 || tick == 382) for (int i : new int[] { 0, 3 }) card(buses[i], true);
        if (tick >= 220 && tick < 250 || tick >= 295 && tick < 325
            || tick >= 411 && tick < 441
            || tick >= 482 && tick < 512) {
            String phase = tick < 250 ? "small_plain"
                : tick < 325 ? "small_card" : tick < 441 ? "large_card" : "large_plain";
            for (int lane : new int[] { 0, 3 }) measure(lane, phase);
        }
        if (tick >= 334 && tick < 366) {
            for (int lane : new int[] { 0, 3 })
                for (int j = 0; j < 16; j++) put(sub[lane].getItemInventory(), namedCircuit((tick - 334) * 16 + j));
        }
        if (tick == 375) for (int i : new int[] { 0, 3 }) check(
            amount(sub[i].getItemInventory(), namedCircuit(511)) == 1,
            "512 distinct resident NBT stacks on lane " + i);
        if (tick == 515) for (int lane : new int[] { 0, 3 }) card(buses[lane], true);
        if (tick >= 540 && tick < 572) {
            for (int lane : new int[] { 0, 3 }) for (int j = 0; j < 16; j++) {
                int id = (tick - 540) * 16 + j;
                take(sub[lane].getItemInventory(), namedCircuit(id), 1);
                ItemStack component = new ItemStack(Items.paper);
                component.setStackDisplayName("part-" + id);
                put(sub[lane].getItemInventory(), AEItemStack.create(component));
            }
        }
        if (tick >= 580 && tick < 610) for (int lane : new int[] { 0, 3 }) {
            IMEInventory<IAEItemStack> h = buses[lane].getInternalHandler();
            long start = System.nanoTime();
            for (int i = 0; i < 48; i++) if (h.injectItems(iron.copy(), Actionable.MODULATE, source) == null)
                throw new AssertionError("mixed target unexpectedly accepted iron");
            timings.computeIfAbsent("mixed_reject_hops_" + (lane == 3 ? 4 : 1), k -> new ArrayList<>())
                .add((System.nanoTime() - start) / 48000.0);
        }
        if (tick == 612) {
            for (int i : new int[] { 0, 3 })
                check(amount(sub[i].getItemInventory(), iron) == 5761, "conserved benchmark writes on lane " + i);
            for (Map.Entry<String, List<Double>> e : timings.entrySet()) {
                Collections.sort(e.getValue());
                report.put(
                    e.getKey() + "_us_per_insert_median_p95",
                    Arrays.asList(
                        e.getValue()
                            .get(15),
                        e.getValue()
                            .get(28)));
            }
            report.put("index_full_and_single_key_reads_during_1440_inserts_per_condition", indexReads);
            // Replace the interface in front of the item-only bus by a vanilla chest.
            w.setBlock(x + 1, y, z + 6, net.minecraft.init.Blocks.chest, 0, 3);
        }
        if (tick == 710) {
            net.minecraft.tileentity.TileEntityChest chest = (net.minecraft.tileentity.TileEntityChest) w
                .getTileEntity(x + 1, y, z + 6);
            IMEInventory<IAEItemStack> h = buses[2].getInternalHandler();
            put(h, iron);
            check(
                chest.getStackInSlot(0) != null && chest.getStackInSlot(0)
                    .getItem() == Items.iron_ingot,
                "direct vanilla chest received first type");
            chest.setInventorySlotContents(1, new ItemStack(Items.gold_ingot));
            check(
                h.injectItems(iron.copy(), Actionable.MODULATE, source) != null,
                "direct chest mixed by player refuses another insert immediately");
            chest.setInventorySlotContents(0, null);
            put(h, gold);
            int inChest = 0;
            for (int i = 0; i < chest.getSizeInventory(); i++)
                if (chest.getStackInSlot(i) != null && chest.getStackInSlot(i)
                    .getItem() == Items.gold_ingot) inChest += chest.getStackInSlot(i).stackSize;
            check(inChest == 2, "direct chest accepts remaining material immediately");
            finish(true);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent e) throws Exception {
        if (e.phase != TickEvent.Phase.END || done) return;
        tick++;
        try {
            step();
        } catch (Throwable ex) {
            ex.printStackTrace();
            report.put("error", ex.toString());
            finish(false);
        }
    }

    private void finish(boolean ok) throws Exception {
        done = true;
        report.put("passed", ok);
        report.put("tests", tests);
        Files.write(
            server.getFile("kind-limit-tests.json")
                .toPath(),
            new GsonBuilder().setPrettyPrinting()
                .create()
                .toJson(report)
                .getBytes(StandardCharsets.UTF_8));
        server.initiateShutdown();
    }
}
