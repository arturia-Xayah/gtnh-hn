package dev.modernization.packtest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import com.google.gson.GsonBuilder;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ICellProvider;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.me.storage.MEInventoryHandler;
import appeng.tile.misc.TileInterface;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEFluidStackType;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.aeoptimizer.dispatch.BatchContext;
import dev.aeoptimizer.dispatch.BatchLimitSettings;
import dev.aeoptimizer.dispatch.BatchLimits;
import dev.aeoptimizer.dispatch.ItemBatchLimitCard;

/** Native AE2FC interfaces with three single-fluid 16k tanks registered on the receiving grid. */
public final class SmartBlockingChecks {

    private final MinecraftServer server = MinecraftServer.getServer();
    private final WorldServer world = server.worldServerForDimension(0);
    private final int x = world.getSpawnPoint().posX, y = 100, z = world.getSpawnPoint().posZ;
    private TileInterface sender, receiver;
    private final Tanks tanks = new Tanks();
    private final Fluid[] fluids = { FluidRegistry.WATER, FluidRegistry.LAVA, FluidRegistry.getFluid("oxygen") };
    private final BaseActionSource action = new BaseActionSource();
    private ICraftingPatternDetails pattern;
    private ItemStack encoded;
    private Block iface;
    private int tick;
    private boolean done;
    private boolean optimized;
    private final List<Object> cpuRuns = new ArrayList<>();
    private final List<String> tests = new ArrayList<>();

    private static void check(boolean ok, String text) {
        if (!ok) throw new AssertionError(text);
    }

    private TileInterface sender() {
        world.setBlock(x, y, z, iface, 0, 3);
        TileInterface tile = (TileInterface) world.getTileEntity(x, y, z);
        tile.setOrientation(ForgeDirection.UP, ForgeDirection.WEST);
        tile.getInterfaceDuality()
            .getPatterns()
            .setInventorySlotContents(0, encoded.copy());
        tile.getInterfaceDuality()
            .getConfigManager()
            .putSetting(Settings.BLOCK, YesNo.YES);
        tile.getInterfaceDuality()
            .getConfigManager()
            .putSetting(Settings.SMART_BLOCK, YesNo.YES);
        return tile;
    }

    private void setup() throws Exception {
        iface = (Block) Class.forName("com.glodblock.github.loader.ItemAndBlockHolder")
            .getField("INTERFACE")
            .get(null);
        Item item = null;
        for (Object obj : Item.itemRegistry) if (obj.getClass()
            .getName()
            .equals("com.glodblock.github.common.item.ItemFluidEncodedPattern")) item = (Item) obj;
        check(item != null && fluids[2] != null, "fixture materials");
        Class<?> cls = Class.forName("com.glodblock.github.util.FluidPatternDetails");
        Object builder = cls.getConstructor(ItemStack.class)
            .newInstance(new ItemStack(item));
        IAEStack<?>[] in = new IAEStack<?>[3];
        for (int i = 0; i < 3; i++) in[i] = AEFluidStack.create(new FluidStack(fluids[i], 1000));
        IAEStack<?>[] out = { AEItemStack.create(new ItemStack(Items.diamond)) };
        cls.getMethod("setInputs", IAEStack[].class)
            .invoke(builder, (Object) in);
        cls.getMethod("setOutputs", IAEStack[].class)
            .invoke(builder, (Object) out);
        encoded = (ItemStack) cls.getMethod("writeToStack")
            .invoke(builder);
        encoded.getTagCompound()
            .setTag(
                "in",
                (NBTTagList) cls.getMethod("writeStackArray", IAEStack[].class)
                    .invoke(null, (Object) in));
        encoded.getTagCompound()
            .setTag(
                "out",
                (NBTTagList) cls.getMethod("writeStackArray", IAEStack[].class)
                    .invoke(null, (Object) out));
        pattern = ((appeng.api.implementations.ICraftingPatternItem) item).getPatternForItem(encoded, world);
        Block power = AEApi.instance()
            .definitions()
            .blocks()
            .energyCellCreative()
            .maybeBlock()
            .get();
        world.setBlock(x - 1, y, z, power, 0, 3);
        world.setBlock(x + 2, y, z, power, 0, 3);
        sender = sender();
        ItemStack cpuBlock = AEApi.instance()
            .definitions()
            .blocks()
            .craftingStorage64k()
            .maybeStack(1)
            .get();
        world.setBlock(x - 1, y + 1, z, Block.getBlockFromItem(cpuBlock.getItem()), cpuBlock.getItemDamage(), 3);
        world.setBlock(x + 1, y, z, iface, 0, 3);
        receiver = (TileInterface) world.getTileEntity(x + 1, y, z);
        receiver.getInterfaceDuality()
            .getUpgrades()
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .materials()
                    .cardAdvancedBlocking()
                    .maybeStack(1)
                    .get());
    }

    private boolean push(int crafts) {
        Container menu = new Container() {

            public boolean canInteractWith(EntityPlayer p) {
                return true;
            }
        };
        MEInventoryCrafting table = new MEInventoryCrafting(menu, 3, 1);
        for (int i = 0; i < 3; i++)
            table.setInventorySlotContents(i, AEFluidStack.create(new FluidStack(fluids[i], 1000 * crafts)));
        BatchContext.OPTIMIZED.set(optimized);
        try {
            return sender.getInterfaceDuality()
                .pushPattern(pattern, table);
        } finally {
            BatchContext.OPTIMIZED.remove();
        }
    }

    private void drain(int amount) throws Exception {
        for (Fluid fluid : fluids) receiver.getProxy()
            .getStorage()
            .getFluidInventory()
            .extractItems(AEFluidStack.create(new FluidStack(fluid, amount)), Actionable.MODULATE, action);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || done) return;
        try {
            switch (++tick) {
                case 1:
                    setup();
                    break;
                case 100:
                    check(
                        sender.getProxy()
                            .isActive()
                            && receiver.getProxy()
                                .isActive(),
                        "interfaces powered");
                    check(
                        sender.getProxy()
                            .getGrid()
                            != receiver.getProxy()
                                .getGrid(),
                        "separate grids");
                    receiver.getProxy()
                        .getStorage()
                        .registerCellProvider(tanks);
                    receiver.getProxy()
                        .getGrid()
                        .postEvent(new appeng.api.networking.events.MENetworkCellArrayUpdate());
                    break;
                case 110:
                    for (int craft = 0; craft < 16; craft++) check(push(1), "native single set " + craft);
                    for (int i = 0; i < 3; i++) check(
                        tanks.slots[i].getFluidAmount() == 16000 && tanks.slots[i].getFluid()
                            .getFluid() == fluids[i],
                        "separate fluids with native single sets");
                    check(!push(1), "full native tanks refuse next single set");
                    drain(16000);
                    tests.add(
                        "native per-recipe smart dispatch: 16 complete sets filled three tanks with separate fluids");
                    boolean accepted = push(100);
                    check(
                        accepted,
                        "native bulk accepted partly: " + sender.getInterfaceDuality()
                            .getScheduledReason()
                            + ", targets="
                            + sender.getTargets()
                            + ", pattern="
                            + sender.getInterfaceDuality().craftingList.contains(pattern)
                            + ", tanks="
                            + tanks.slots[0].getFluidAmount());
                    for (FluidTank t : tanks.slots) check(
                        t.getFluidAmount() == 16000 && t.getFluid()
                            .getFluid() == fluids[0],
                        "first fluid monopolizes tanks");
                    tests.add(
                        "unprotected 100-craft bulk call into native interface: fluid A filled all three 16k tanks, B/C could not enter");
                    world.setBlockToAir(x, y, z);
                    drain(48000);
                    sender = sender();
                    world.setBlock(x, y, z - 1, dev.aeoptimizer.OrderOptimizer.BLOCK, 0, 3);
                    break;
                case 180:
                    optimized = true;
                    check(dev.aeoptimizer.dispatch.OutputOverflow.enabled(sender.getProxy()), "optimizer active");
                    limitsChecks();
                    cpuChecks();
                    finish(null);
                    break;
                default:
                    break;
            }
        } catch (Throwable error) {
            error.printStackTrace();
            finish(error.toString());
        }
    }

    private BatchLimitSettings settings() {
        return (BatchLimitSettings) sender.getInterfaceDuality();
    }

    private void clearTanks() throws Exception {
        drain(48000);
        java.lang.reflect.Field busy = appeng.helpers.DualityInterface.class.getDeclaredField("busyCache");
        busy.setAccessible(true);
        busy.setLong(sender.getInterfaceDuality(), 0);
    }

    private void limitsChecks() throws Exception {
        appeng.helpers.DualityInterface d = sender.getInterfaceDuality();
        ItemStack card = new ItemStack(ItemBatchLimitCard.INSTANCE);
        check(
            d.getUpgrades()
                .isItemValidForSlot(0, card),
            "AE2FC block accepts upgrade");
        d.getUpgrades()
            .setInventorySlotContents(0, card);
        check(settings().hasBatchLimitCard(), "cached upgrade count recognizes custom card");
        check(
            !d.getUpgrades()
                .isItemValidForSlot(1, card),
            "second limit card rejected");
        tests.add("AE2FC upgrade slot accepts one limit card, rejects a duplicate");
        settings().setBatchLimits(2000000, 1000000);
        List<IAEStack<?>> example = new ArrayList<>();
        example.add(
            AEItemStack.create(new ItemStack(Items.iron_ingot))
                .setStackSize(250000));
        for (int i = 0; i < 3; i++)
            example.add(AEFluidStack.create(new FluidStack(fluids[i], new int[] { 100000, 50000, 20000 }[i])));
        check(BatchLimits.maximum(example, settings(), 4096) == 8, "exact user example produces eight sets");
        tests.add("1M mB per fluid / 2M per item, 100k+50k+20k fluids / 250k items => eight sets");
        settings().setBatchLimits(10, 0);
        example.clear();
        example.add(AEItemStack.create(new ItemStack(Items.iron_ingot, 3)));
        example.add(AEItemStack.create(new ItemStack(Items.iron_ingot, 4)));
        check(BatchLimits.maximum(example, settings(), 4096) == 1, "repeated slots merged");
        ItemStack named = new ItemStack(Items.iron_ingot, 4);
        named.setStackDisplayName("different NBT");
        example.set(1, AEItemStack.create(named));
        check(BatchLimits.maximum(example, settings(), 4096) == 2, "NBT variants separately limited");
        tests.add("Repeated slots merge by exact AE identity; named item remains a different type");
        settings().setBatchLimits(0, 8000);
        check(push(8), "smart first eight");
        check(push(8), "smart second eight without waiting for empty");
        for (FluidTank tank : tanks.slots) check(tank.getFluidAmount() == 16000, "smart two batches fill each tank");
        clearTanks();
        tests.add("Smart Blocking + card: two consecutive batches of eight, native smart behavior retained");
        d.getConfigManager()
            .putSetting(Settings.SMART_BLOCK, YesNo.NO);
        check(push(8), "strict first eight");
        check(!push(8), "strict second batch blocked while occupied");
        clearTanks();
        check(push(8), "strict resumes when empty");
        clearTanks();
        tests.add("Strict Blocking + card: eight sets once, wait empty, then resume");
        check(!push(9), "oversize actual table rejected before partial injection");
        for (FluidTank tank : tanks.slots) check(tank.getFluidAmount() == 0, "over-limit input left untouched");
        tests.add("Oversized actual table rejected before any item/fluid insertion");
        settings().setBatchLimits(0, 999);
        check(!push(1) && settings().batchLimitBlocked(), "one set below limit paused and flagged");
        optimized = false;
        check(push(1), "no optimized transaction ignores installed card");
        clearTanks();
        optimized = true;
        tests.add("Limit below one recipe blocks optimized dispatch; unoptimized transaction ignores it");
        d.getUpgrades()
            .setInventorySlotContents(0, null);
        check(push(1), "removing card takes effect immediately");
        clearTanks();
        check(!settings().hasBatchLimitCard(), "card removal invalidates upgrade cache");
        d.getUpgrades()
            .setInventorySlotContents(0, card.copy());
        settings().setBatchLimits(Long.MAX_VALUE, Long.MAX_VALUE - 1);
        net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
        d.writeToNBT(nbt);
        settings().setBatchLimits(0, 0);
        d.readFromNBT(nbt);
        check(
            settings().batchItemLimit() == Long.MAX_VALUE && settings().batchFluidLimit() == Long.MAX_VALUE - 1,
            "long limits NBT round trip");
        tests.add("Card removal immediately disables limits; long limits and upgrade survive NBT round trip");
        settings().setBatchLimits(0, 0);
        check(push(16), "zero limit unlimited");
        clearTanks();
        tests.add("Zero limits allow full native payload");
        nativeHosts(card);
        containerChecks();
    }

    private void nativeHosts(ItemStack card) throws Exception {
        check(
            ItemBatchLimitCard.TYPE.getSupported()
                .size()
                == appeng.api.config.Upgrades.ADVANCED_BLOCKING.getSupported()
                    .size(),
            "same supported hosts as native advanced blocking card");
        for (ItemStack host : appeng.api.config.Upgrades.ADVANCED_BLOCKING.getSupported()
            .keySet())
            check(
                Integer.valueOf(1)
                    .equals(
                        ItemBatchLimitCard.TYPE.getSupported()
                            .get(host)),
                "native upgrade registration limits each supported host to one card");
        // Detached hosts exercise native upgrade validation without changing either live network.
        TileInterface nativeBlock = new TileInterface();
        check(
            nativeBlock.getInterfaceDuality()
                .getUpgrades()
                .isItemValidForSlot(0, card),
            "native block slot");
        ItemStack part = AEApi.instance()
            .definitions()
            .parts()
            .iface()
            .maybeStack(1)
            .get();
        appeng.parts.misc.PartInterface nativePart = new appeng.parts.misc.PartInterface(part);
        check(
            nativePart.getInterfaceDuality()
                .getUpgrades()
                .isItemValidForSlot(0, card),
            "native part slot");
        boolean dualPart = false;
        for (ItemStack host : ItemBatchLimitCard.TYPE.getSupported()
            .keySet())
            if (host.getItem()
                .getClass()
                .getName()
                .startsWith("com.glodblock.") && !(host.getItem() instanceof net.minecraft.item.ItemBlock))
                dualPart = true;
        check(dualPart, "AE2FC part registered");
        tests.add("Native block / part interface and AE2FC part declare limit card support");
    }

    private void containerChecks() throws Exception {
        net.minecraftforge.common.util.FakePlayer player = new net.minecraftforge.common.util.FakePlayer(
            world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "BatchLimitsTest"));
        player.setPosition(x + 0.5, y, z + 0.5);
        appeng.container.implementations.ContainerInterface menu = new appeng.container.implementations.ContainerInterface(
            player.inventory,
            sender);
        player.openContainer = menu;
        BatchLimitSettings cfg = (BatchLimitSettings) menu;
        settings().setBatchLimits(100, 200);
        cfg.setBatchLimits(123, 456);
        check(settings().batchItemLimit() == 123 && settings().batchFluidLimit() == 456, "GUI edits both limits");
        cfg.setBatchLimits(-1, 8000);
        check(
            settings().batchItemLimit() == 123 && settings().batchFluidLimit() == 456,
            "negative packet atomic rejection");
        player.setPosition(x + 100, y, z);
        cfg.setBatchLimits(9, 9);
        check(settings().batchItemLimit() == 123, "remote edit rejected");
        player.setPosition(x + 0.5, y, z + 0.5);
        sender.getInterfaceDuality()
            .getUpgrades()
            .setInventorySlotContents(0, null);
        cfg.setBatchLimits(10, 10);
        check(settings().batchItemLimit() == 123, "card removal rejects stale GUI edit");
        sender.getInterfaceDuality()
            .getUpgrades()
            .setInventorySlotContents(0, new ItemStack(ItemBatchLimitCard.INSTANCE));
        player.openContainer = player.inventoryContainer;
        cfg.setBatchLimits(20, 20);
        check(settings().batchItemLimit() == 123, "closed GUI rejects edit");
        tests.add("Native container: atomic edit, negative limit, distance, card-removal and closed-screen checks");
    }

    private static java.lang.reflect.Field cpuField(String name) throws Exception {
        java.lang.reflect.Field field = appeng.me.cluster.implementations.CraftingCPUCluster.class
            .getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private long cpuDispatch(String name, int sets, int operations, long expectedSets, long expectedBatches)
        throws Exception {
        appeng.api.networking.IGrid grid = sender.getProxy()
            .getGrid();
        appeng.me.cache.CraftingGridCache cache = grid.getCache(appeng.api.networking.crafting.ICraftingGrid.class);
        check(
            !cache.getCpus()
                .isEmpty(),
            "real AE CPU formed");
        appeng.me.cluster.implementations.CraftingCPUCluster cpu = (appeng.me.cluster.implementations.CraftingCPUCluster) cache
            .getCpus()
            .iterator()
            .next();
        check(
            cache.getMediums(pattern)
                .contains(sender.getInterfaceDuality()),
            "real sender registered as crafting medium");
        appeng.me.cluster.implementations.CraftingCPUCluster.TaskProgress progress = new appeng.me.cluster.implementations.CraftingCPUCluster.TaskProgress();
        ((dev.aeoptimizer.api.ProgressAccess) progress).aeo$set(sets);
        java.util.Map tasks = (java.util.Map) cpuField("tasks").get(cpu),
            workable = (java.util.Map) cpuField("workableTasks").get(cpu);
        tasks.clear();
        workable.clear();
        tasks.put(pattern, progress);
        workable.put(pattern, progress);
        appeng.crafting.MECraftingInventory inventory = new appeng.crafting.MECraftingInventory();
        for (IAEStack<?> input : pattern.getCondensedAEInputs()) inventory.injectItems(
            input.copy()
                .setStackSize(input.getStackSize() * sets),
            Actionable.MODULATE);
        cpuField("inventory").set(cpu, inventory);
        cpuField("remainingOperations").setInt(cpu, operations);
        cpuField("accelerator").setInt(cpu, 4096);
        long before = ((dev.aeoptimizer.api.CpuBatchStats) cpu).aeo$getBatches();
        java.lang.reflect.Method execute = cpu.getClass()
            .getDeclaredMethod(
                "executeCrafting",
                appeng.api.networking.energy.IEnergyGrid.class,
                appeng.me.cache.CraftingGridCache.class);
        execute.setAccessible(true);
        long start = System.nanoTime();
        execute.invoke(cpu, grid.getCache(appeng.api.networking.energy.IEnergyGrid.class), cache);
        long elapsed = System.nanoTime() - start;
        long got = sets - ((dev.aeoptimizer.api.ProgressAccess) progress).aeo$get();
        long batches = ((dev.aeoptimizer.api.CpuBatchStats) cpu).aeo$getBatches() - before;
        check(got == expectedSets, name + " sets " + got + " expected " + expectedSets);
        check(batches == expectedBatches, name + " batches " + batches);
        check(
            cpuField("remainingOperations").getInt(cpu) == operations - expectedBatches,
            name + " one step per batch");
        for (IAEStack<?> input : pattern.getCondensedAEInputs()) {
            IAEStack<?> remaining = inventory.extractItems(
                input.copy()
                    .setStackSize(Long.MAX_VALUE),
                Actionable.SIMULATE);
            check(
                (remaining == null ? 0 : remaining.getStackSize()) == input.getStackSize() * (sets - got),
                "CPU inputs conserved");
        }
        java.util.Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("sets", got);
        row.put("batches", batches);
        row.put("nanoseconds", elapsed);
        cpuRuns.add(row);
        tasks.clear();
        workable.clear();
        ((IItemList<?>) cpuField("waitingFor").get(cpu)).resetStatus();
        cpuField("inventory").set(cpu, new appeng.crafting.MECraftingInventory());
        return got;
    }

    private void cpuChecks() throws Exception {
        appeng.helpers.DualityInterface d = sender.getInterfaceDuality();
        settings().setBatchLimits(0, 8000);
        d.getConfigManager()
            .putSetting(Settings.SMART_BLOCK, YesNo.NO);
        cpuDispatch("strict eight then wait empty", 100, 64, 8, 1);
        clearTanks();
        d.getConfigManager()
            .putSetting(Settings.SMART_BLOCK, YesNo.YES);
        cpuDispatch("smart two consecutive batches of eight", 16, 64, 16, 2);
        clearTanks();
        settings().setBatchLimits(0, 999);
        cpuDispatch("limit below one set", 100, 64, 0, 0);
        settings().setBatchLimits(0, 8000);
        cpuDispatch("one remaining operation sends eight sets", 100, 1, 8, 1);
        clearTanks();
        d.getUpgrades()
            .setInventorySlotContents(0, null);
        d.getConfigManager()
            .putSetting(Settings.SMART_BLOCK, YesNo.NO);
        cpuDispatch("no card strict keeps one-set native boundary", 100, 64, 1, 1);
        clearTanks();
        d.getUpgrades()
            .setInventorySlotContents(0, new ItemStack(ItemBatchLimitCard.INSTANCE));
        d.getConfigManager()
            .putSetting(Settings.SMART_BLOCK, YesNo.YES);
        settings().setBatchLimits(0, 0);
        cpuDispatch("zero limit does not cap optimized batch", 16, 1, 16, 1);
        clearTanks();
        check(!BatchContext.OPTIMIZED.get() && !BatchContext.ACTIVE.get(), "dispatch contexts restored");
        tests.add(
            "Real native CPU: strict 8x1, smart 8x2, sub-recipe cap, one operation for eight sets, no-card/zero cases; inputs conserved");
        settings().setBatchLimits(0, 8000);
        d.getConfigManager()
            .putSetting(Settings.BLOCK, YesNo.NO);
        receiver.getInterfaceDuality()
            .getUpgrades()
            .setInventorySlotContents(0, null);
        cpuDispatch("unblocked subnet without advanced card", 16, 64, 16, 2);
        clearTanks();
        tests.add("No Advanced Blocking Card / no blocking: native receiving semantics, two batches of eight");
        appeng.me.cache.CraftingGridCache cache = sender.getProxy()
            .getGrid()
            .getCache(appeng.api.networking.crafting.ICraftingGrid.class);
        appeng.api.networking.crafting.ICraftingMedium legacy = new appeng.api.networking.crafting.ICraftingMedium() {

            public boolean isBusy() {
                return true;
            }

            public boolean pushPattern(ICraftingPatternDetails p, net.minecraft.inventory.InventoryCrafting table) {
                throw new AssertionError("busy alternative must not receive");
            }
        };
        // One unsupported alternative makes the CPU use its native loop for this recipe.
        cache.addCraftingOption(legacy, pattern);
        settings().setBatchLimits(0, 999);
        try {
            cpuDispatch("native fallback cannot bypass too-small cap", 100, 64, 0, 0);
        } finally {
            cache.getMediums(pattern)
                .remove(legacy);
        }
        check(!BatchContext.OPTIMIZED.get() && !BatchContext.ACTIVE.get(), "native fallback restores context");
        tests.add("Native CPU fallback also enforces the cap and returns all rejected inputs");
    }

    private void finish(String error) throws Exception {
        done = true;
        java.util.Map<String, Object> report = new LinkedHashMap<>();
        report.put("passed", error == null);
        report.put("tests", tests);
        report.put("cpu_dispatch", cpuRuns);
        if (error != null) report.put("error", error);
        Files.write(
            server.getFile("smart-blocking-tests.json")
                .toPath(),
            new GsonBuilder().setPrettyPrinting()
                .create()
                .toJson(report)
                .getBytes(StandardCharsets.UTF_8));
        server.initiateShutdown();
    }

    static final class Tanks implements ICellProvider, IMEInventory<IAEFluidStack> {

        final FluidTank[] slots = { new FluidTank(16000), new FluidTank(16000), new FluidTank(16000) };
        final IMEInventoryHandler<IAEFluidStack> handler = new MEInventoryHandler<>(
            this,
            AEFluidStackType.FLUID_STACK_TYPE);

        public int getPriority() {
            return 0;
        }

        public List<IMEInventoryHandler> getCellArray(IAEStackType<?> type) {
            return type == AEFluidStackType.FLUID_STACK_TYPE ? Collections.singletonList(handler)
                : Collections.emptyList();
        }

        public IAEStackType<?> getStackType() {
            return AEFluidStackType.FLUID_STACK_TYPE;
        }

        public IAEFluidStack injectItems(IAEFluidStack input, Actionable mode, BaseActionSource src) {
            int left = (int) input.getStackSize();
            for (FluidTank slot : slots) {
                FluidStack f = input.getFluidStack();
                f.amount = left;
                left -= slot.fill(f, mode == Actionable.MODULATE);
                if (left == 0) return null;
            }
            return input.copy()
                .setStackSize(left);
        }

        public IAEFluidStack extractItems(IAEFluidStack input, Actionable mode, BaseActionSource src) {
            int left = (int) input.getStackSize();
            for (FluidTank slot : slots) if (slot.getFluid() != null && slot.getFluid()
                .isFluidEqual(input.getFluidStack())) {
                    FluidStack drained = slot.drain(left, mode == Actionable.MODULATE);
                    if (drained != null) left -= drained.amount;
                }
            long got = input.getStackSize() - left;
            return got == 0 ? null
                : input.copy()
                    .setStackSize(got);
        }

        public IItemList<IAEFluidStack> getAvailableItems(IItemList<IAEFluidStack> result) {
            for (FluidTank slot : slots)
                if (slot.getFluidAmount() > 0) result.add(AEFluidStack.create(slot.getFluid()));
            return result;
        }
    }
}
