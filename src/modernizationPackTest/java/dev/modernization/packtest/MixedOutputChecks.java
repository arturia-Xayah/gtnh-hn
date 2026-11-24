package dev.modernization.packtest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.google.gson.GsonBuilder;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.aeoptimizer.OrderOptimizer;
import dev.aeoptimizer.dispatch.OutputOverflow;
import dev.structureterminal.output.MixedOutputBus;
import dev.structureterminal.output.MixedOutputHost;
import dev.structureterminal.output.MixedOutputs;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.IOutputBusTransaction;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.interfaces.IOutputTransaction;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.HatchElementBuilder;
import gregtech.common.tileentities.machines.multi.MTELargeChemicalReactor;

/** Real GT output transactions and StructureLib position checks on the pinned dedicated pack. */
public final class MixedOutputChecks {

    private final MinecraftServer server = MinecraftServer.getServer();
    private final WorldServer world = server.worldServerForDimension(0);
    private final FakePlayer player = FakePlayerFactory.getMinecraft(world);
    private final int x = world.getSpawnPoint().posX, y = 100, z = world.getSpawnPoint().posZ;
    private final List<String> tests = new ArrayList<>();
    private final Map<String, Object> report = new LinkedHashMap<>();
    private MixedOutputBus bus;
    private TileDrive drive;
    private MTELargeChemicalReactor reactor;
    private int tick;
    private boolean done;
    private static volatile long sink;

    private void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private MetaTileEntity place(ItemStack stack, int px, int py, int pz) {
        check(
            ((ItemBlock) stack.getItem())
                .placeBlockAt(stack, player, world, px, py, pz, 1, 0, 0, 0, stack.getItemDamage()),
            "place " + px);
        return (MetaTileEntity) ((IGregTechTileEntity) world.getTileEntity(px, py, pz)).getMetaTileEntity();
    }

    private void setup() throws Exception {
        player.capabilities.isCreativeMode = true;
        bus = (MixedOutputBus) place(MixedOutputs.bus.getStackForm(1), x + 2, y, z);
        bus.setConnectsToAllSides(true);
        Block power = AEApi.instance()
            .definitions()
            .blocks()
            .energyCellCreative()
            .maybeBlock()
            .get();
        world.setBlock(x, y, z, power, 0, 3);
        ItemStack driveStack = AEApi.instance()
            .definitions()
            .blocks()
            .drive()
            .maybeStack(1)
            .get();
        Block driveBlock = Block.getBlockFromItem(driveStack.getItem());
        world.setBlock(x + 1, y, z, driveBlock, driveStack.getItemDamage(), 3);
        driveBlock.onBlockPlacedBy(world, x + 1, y, z, player, driveStack);
        drive = (TileDrive) world.getTileEntity(x + 1, y, z);
        drive.getInternalInventory()
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .items()
                    .cell64k()
                    .maybeStack(1)
                    .get());
        Item fluidCell = (Item) Class.forName("com.glodblock.github.loader.ItemAndBlockHolder")
            .getField("CELL1K")
            .get(null);
        drive.getInternalInventory()
            .setInventorySlotContents(1, new ItemStack(fluidCell));
        configure(bus);
        reactor = (MTELargeChemicalReactor) place(ItemList.Machine_Multi_LargeChemicalReactor.get(1), x + 12, y, z);
    }

    private void configure(MixedOutputBus target) {
        NBTTagCompound settings = new NBTTagCompound();
        settings.setLong("baseCapacity", 16000);
        settings.setInteger("refreshTime", 10000);
        settings.setBoolean("additionalConnection", true);
        target.getProvider()
            .loadNBTData(settings);
        target.fluids.loadNBTData(settings);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean accepts(IHatchElement... roles) {
        reactor.mOutputBusses.clear();
        ((MixedOutputHost) reactor).modernization$approvedOutputs()
            .clear();
        return GTStructureUtility.<MTEMultiBlockBase>buildHatchAdder()
            .anyOf(roles)
            .casingIndex(176)
            .hint(1)
            .build()
            .check(reactor, world, x + 2, y, z);
    }

    private void structures() throws Exception {
        HatchElementBuilder<MTEMultiBlockBase> busPosition = GTStructureUtility.<MTEMultiBlockBase>buildHatchAdder()
            .anyOf(HatchElement.OutputBus)
            .casingIndex(176)
            .hint(1);
        busPosition.build();
        check(
            !busPosition.getHatchItemFilter()
                .apply(reactor, new ItemStack(Items.stick))
                .test(MixedOutputs.bus.getStackForm(1)),
            "auto-build excludes mixed output from bus-only position");
        HatchElementBuilder<MTEMultiBlockBase> dualPosition = GTStructureUtility.<MTEMultiBlockBase>buildHatchAdder()
            .anyOf(HatchElement.OutputBus, HatchElement.OutputHatch)
            .casingIndex(176)
            .hint(1);
        dualPosition.build();
        check(
            dualPosition.getHatchItemFilter()
                .apply(reactor, new ItemStack(Items.stick))
                .test(MixedOutputs.bus.getStackForm(1)),
            "auto-build includes mixed output for dual position");
        check(!accepts(HatchElement.OutputBus), "reject bus-only position");
        check(!accepts(HatchElement.OutputHatch), "reject hatch-only position");
        check(accepts(HatchElement.OutputBus, HatchElement.OutputHatch), "accept same-position dual roles");
        check(
            reactor.getOutputHatches()
                .contains(bus.fluidEndpoint)
                && reactor.getOutputBusses()
                    .contains(bus),
            "expose both output endpoints");
        check(!accepts(HatchElement.OutputBus), "previous dual position does not authorize bus-only position");
        check(accepts(HatchElement.OutputBus.or(HatchElement.OutputHatch)), "accept native OR element");
        check(
            accepts(HatchElement.OutputBus.withName("alias"), HatchElement.OutputHatch.withCount(m -> 0)),
            "safe aliases");
        check(
            !accepts(HatchElement.OutputBus.withAdder((m, t, c) -> true), HatchElement.OutputHatch),
            "custom bus adder fails closed");
        check(
            !reactor.addOutputBusToMachineList(bus.getBaseMetaTileEntity(), 176),
            "direct legacy bus-only adder rejected");
        check(
            !GTStructureUtility.<MTEMultiBlockBase>buildHatchAdder()
                .anyOf(HatchElement.OutputHatch, HatchElement.OutputBus)
                .adder(MTEMultiBlockBase::addOutputBusToMachineList)
                .casingIndex(176)
                .hint(1)
                .build()
                .check(reactor, world, x + 2, y, z),
            "custom builder override rejected");
        tests.add(
            "position and auto-build rules: bus only/hatch only/dual/native OR/alias/custom override/direct legacy/prior approval isolation");
        reactor.construct(new ItemStack(Items.stick), false);
        List<int[]> casings = new ArrayList<>();
        for (int px = x + 9; px <= x + 15; px++)
            for (int py = y - 3; py <= y + 3; py++) for (int pz = z - 3; pz <= z + 3; pz++)
                if (world.getBlock(px, py, pz) == GregTechAPI.sBlockCasings8 && world.getBlockMetadata(px, py, pz) == 0)
                    casings.add(new int[] { px, py, pz });
        check(casings.size() >= 12, "constructed LCR shell: " + casings.size());
        ItemStack[] parts = { MixedOutputs.bus.getStackForm(1), ItemList.Hatch_Energy_LV.get(1),
            ItemList.Hatch_Maintenance.get(1), ItemList.Hatch_Input_Bus_LV.get(1) };
        MixedOutputBus formed = null;
        for (int i = 0; i < parts.length; i++) {
            int[] pos = casings.get(i);
            MetaTileEntity m = place(parts[i], pos[0], pos[1], pos[2]);
            if (i == 0) formed = (MixedOutputBus) m;
        }
        check(reactor.checkStructure(true, reactor.getBaseMetaTileEntity()), "real LCR forms with mixed output");
        check(
            reactor.getOutputBusses()
                .contains(formed)
                && reactor.getOutputHatches()
                    .contains(formed.fluidEndpoint),
            "formed native endpoint discovery");
        check(
            !formed.getProxy()
                .isActive(),
            "LCR output offline");
        formed.inventoryHandler.setStackInSlot(
            MixedOutputBus.FLUID_SLOT,
            new ItemStack(com.glodblock.github.loader.ItemAndBlockHolder.CELL16384KM));
        formed.fluids.updateCell();
        FluidStack acid = gregtech.api.enums.Materials.HydrochloricAcid.getFluid(1000);
        java.lang.reflect.Method finishFluids = MTEMultiBlockBase.class
            .getDeclaredMethod("addFluidOutputs", FluidStack[].class);
        finishFluids.setAccessible(true);
        check(
            (boolean) finishFluids.invoke(reactor, (Object) new FluidStack[] { acid.copy() }),
            "offline LCR acid ejection");
        formed.fluids.flushCachedStack();
        NBTTagCompound hud = new NBTTagCompound();
        formed.getWailaNBTData(
            player,
            (net.minecraft.tileentity.TileEntity) formed.getBaseMetaTileEntity(),
            hud,
            world,
            x,
            y,
            z);
        NBTTagCompound fluidHud = hud.getCompoundTag("hnFluidView");
        NBTTagCompound acidRow = fluidHud.getTagList("stacks", 10)
            .getCompoundTagAt(0);
        check(
            hud.getInteger("stackCount") == 0 && fluidHud.getLong("buffered") == 1000
                && acidRow.getLong("Amount") == 1000
                && AEFluidStack.loadFluidStackFromNBT(acidRow)
                    .getFluid() == acid.getFluid(),
            "Waila distinguishes empty items and 1000 mB acid offline");
        tests.add(
            "Offline formed LCR, 16384k multi-fluid cell: 1000 mB hydrochloric acid retained after flush and listed in Waila");
        check(
            reactor.addItemOutputs(new ItemStack[] { new ItemStack(Items.gold_ingot, 13) }),
            "LCR native item ejection");
        check(reactor.addOutputAtomic(new FluidStack(FluidRegistry.LAVA, 123)), "LCR native fluid ejection");
        check(
            formed.getProvider()
                .getCachedAmount() == 13 && formed.fluids.getCachedAmount() == 1123,
            "native LCR conserves both outputs");
        tests.add("real LCR construction/forms/independent native item + fluid ejection");
    }

    private void emit(boolean recipeCheck, boolean expect) {
        IOutputBusTransaction it = bus.createTransaction();
        IOutputHatchTransaction ft = bus.fluidEndpoint.createTransaction();
        ((IOutputTransaction.IRecipeCheckAware) it).setRecipeCheck(recipeCheck);
        ((IOutputTransaction.IRecipeCheckAware) ft).setRecipeCheck(recipeCheck);
        ItemStack items = new ItemStack(Items.diamond, 10000);
        FluidStack water = new FluidStack(FluidRegistry.WATER, 10000);
        boolean i = it.storePartial(GTUtility.ItemId.createNoCopy(items), items, 10000, 10000);
        boolean f = ft.storePartial(GTUtility.FluidId.create(water), water, 10000, 10000);
        check(
            i == expect && f == expect
                && items.stackSize == (expect ? 0 : 10000)
                && water.amount == (expect ? 0 : 10000),
            "native tx recipe=" + recipeCheck
                + " expected="
                + expect
                + " actual="
                + i
                + "/"
                + f
                + " remaining="
                + items.stackSize
                + "/"
                + water.amount);
        if (!recipeCheck) {
            it.commit();
            ft.commit();
        }
    }

    private void conservation(long n) throws Exception {
        check(
            bus.getProvider()
                .getCachedAmount() == n && bus.fluids.getCachedAmount() == n,
            "cache totals " + n);
    }

    private void flush() throws Exception {
        bus.getProvider()
            .flushCachedStack();
        bus.fluids.flushCachedStack();
        conservation(0);
    }

    private void stock(long n) throws Exception {
        IAEItemStack items = drive.getProxy()
            .getStorage()
            .getItemInventory()
            .getStorageList()
            .findPrecise(AEItemStack.create(new ItemStack(Items.diamond)));
        IAEFluidStack fluids = drive.getProxy()
            .getStorage()
            .getFluidInventory()
            .getStorageList()
            .findPrecise(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)));
        check(
            items != null && fluids != null && items.getStackSize() == n && fluids.getStackSize() == n,
            "network conservation " + n);
    }

    private void persistence() throws Exception {
        NBTTagCompound nbt = new NBTTagCompound();
        bus.saveNBTData(nbt);
        MixedOutputBus copy = (MixedOutputBus) place(MixedOutputs.bus.getStackForm(1), x + 6, y, z);
        copy.loadNBTData(nbt);
        check(
            copy.getProvider()
                .getCachedAmount() == 40000 && copy.fluids.getCachedAmount() == 40000,
            "load both overflow caches");
        check(
            !nbt.getCompoundTag("dualFluidOutput")
                .hasKey("proxy"),
            "single AE node serialization");
        check(
            copy.getProvider()
                .getRefreshTime() == 10000 && copy.fluids.getRefreshTime() == 10000,
            "independent native settings saved");
        world.setBlockToAir(x + 6, y, z); // disposable fixture copy, deliberately never connected
        tests.add("NBT round trip preserves both overfull caches, channel settings, and a single AE node");
    }

    private void cellPersistence(ItemStack itemCell, ItemStack fluidCell) {
        MixedOutputBus isolated = (MixedOutputBus) place(MixedOutputs.bus.getStackForm(1), x + 7, y, z);
        isolated.inventoryHandler.setStackInSlot(0, itemCell.copy());
        isolated.inventoryHandler.setStackInSlot(1, fluidCell.copy());
        check(
            !isolated.getProxy()
                .isActive(),
            "dual-cell capacity test offline");
        for (int i = 0; i < 32; i++) {
            check(isolated.storePartial(new ItemStack(Items.diamond, 64), false), "offline item output");
        }
        check(
            isolated.fluidEndpoint.storePartial(new FluidStack(FluidRegistry.WATER, 256000), false),
            "offline fluid output");
        check(
            isolated.getProvider()
                .getCachedAmount() == 2048 && isolated.fluids.getCachedAmount() == 256000,
            "simultaneously buffer more than both native defaults");
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(x + 7, y, z);
        NBTTagCompound saved = new NBTTagCompound();
        tile.writeToNBT(saved);
        tile.readFromNBT(saved);
        MixedOutputBus restored = (MixedOutputBus) ((IGregTechTileEntity) tile).getMetaTileEntity();
        check(
            ItemStack.areItemStacksEqual(itemCell, restored.getCellStack())
                && ItemStack.areItemStacksEqual(fluidCell, restored.fluidCell()),
            "both physical cells survive GT inventory reload");
        check(restored.isFiltered() && restored.fluids.isFiltered(), "both partitions survive reload");
        check(
            restored.getProvider()
                .getCachedAmount() == 2048 && restored.fluids.getCachedAmount() == 256000,
            "all buffered outputs survive world reload");
        tests.add("Disconnected dual output retains 2,048 items and 256,000 mB across a GT tile reload");
        // Recreate the previous format by removing the item cell and storing the complete fluid cell in slot zero.
        net.minecraft.nbt.NBTTagList old = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound entry = new NBTTagCompound();
        fluidCell.writeToNBT(entry);
        entry.setInteger("IntSlot", 0);
        old.appendTag(entry);
        saved.setTag("Inventory", old);
        tile.readFromNBT(saved);
        restored = (MixedOutputBus) ((IGregTechTileEntity) tile).getMetaTileEntity();
        check(
            restored.getCellStack() == null && ItemStack.areItemStacksEqual(fluidCell, restored.fluidCell())
                && restored.fluids.isFiltered(),
            "old shared slot moves the entire partitioned fluid cell");
        NBTTagCompound migrated = new NBTTagCompound();
        tile.writeToNBT(migrated);
        check(
            migrated.getTagList("Inventory", 10)
                .tagCount() == 1
                && migrated.getTagList("Inventory", 10)
                    .getCompoundTagAt(0)
                    .getInteger("IntSlot") == 1,
            "migrated world saves exactly one fluid cell in slot one");
        world.setBlockToAir(x + 7, y, z);
        tests.add("GT tile save/load preserves both cells and migrates the old shared slot without duplication");
    }

    private void filters() throws Exception {
        ItemStack itemCell = AEApi.instance()
            .definitions()
            .items()
            .cell64k()
            .maybeStack(1)
            .get();
        ((appeng.api.storage.ICellWorkbenchItem) itemCell.getItem()).getConfigAEInventory(itemCell)
            .putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.diamond)));
        bus.inventoryHandler.setStackInSlot(0, itemCell);
        bus.onContentsChanged(0);
        bus.getProvider()
            .updateCell();
        bus.fluids.updateCell();
        check(bus.isFiltered(), "native item partition active");
        check(!bus.storePartial(new ItemStack(Items.gold_ingot, 1), false), "optimizer does not bypass item filter");
        check(bus.storePartial(new ItemStack(Items.diamond, 1), true), "partition accepts matching item in simulation");
        check(
            bus.fluidEndpoint.storePartial(new FluidStack(FluidRegistry.LAVA, 1), true),
            "item partition does not filter fluids");
        check(bus.fluids.getCacheCapacity() == 16000, "item cell affects only item capacity");
        Item fluidItem = (Item) Class.forName("com.glodblock.github.loader.ItemAndBlockHolder")
            .getField("CELL16384KM")
            .get(null);
        ItemStack fluidCell = new ItemStack(fluidItem);
        ((appeng.api.storage.ICellWorkbenchItem) fluidItem).getConfigAEInventory(fluidCell)
            .putAEStackInSlot(0, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)));
        check(bus.getSizeInventory() == 2 && bus.inventoryHandler.getSlots() == 2, "two native inventory slots");
        MetaTileEntity nativeBus = place(ItemList.Hatch_Output_Bus_ME.get(1), x + 9, y, z);
        check(nativeBus.getSizeInventory() == 1, "unmodified GT bus retains one slot");
        world.setBlockToAir(x + 9, y, z);
        check(
            bus.inventoryHandler.isItemValid(0, itemCell) && bus.inventoryHandler.isItemValid(1, fluidCell)
                && !bus.inventoryHandler.isItemValid(1, itemCell)
                && !bus.inventoryHandler.isItemValid(0, fluidCell),
            "GUI slots enforce channel type");
        bus.inventoryHandler.setStackInSlot(1, fluidCell);
        bus.getProvider()
            .updateCell();
        bus.fluids.updateCell();
        check(bus.fluids.isFiltered(), "native fluid partition active");
        check(
            !bus.fluidEndpoint.storePartial(new FluidStack(FluidRegistry.LAVA, 1), false),
            "optimizer does not bypass fluid filter");
        check(
            bus.fluidEndpoint.storePartial(new FluidStack(FluidRegistry.WATER, 1), true),
            "partition accepts matching fluid in simulation");
        check(
            bus.isFiltered() && !bus.storePartial(new ItemStack(Items.gold_ingot, 1), true),
            "adding fluid cell preserves item partition");
        check(
            bus.getProvider()
                .getCacheCapacity() > 16000 && bus.fluids.getCacheCapacity() > 16000,
            "both capacities upgraded simultaneously");
        bus.getProvider()
            .setCacheMode(true);
        bus.fluids.setCacheMode(true);
        check(
            bus.getCellArray(appeng.api.storage.StorageChannel.ITEMS)
                .size() == 1,
            "item cache exposed alongside fluid cache");
        cellPersistence(itemCell, fluidCell);
        bus.inventoryHandler.setStackInSlot(0, null);
        check(
            bus.getProvider()
                .getCacheCapacity() == 16000 && bus.fluids.getCacheCapacity() > 16000
                && bus.fluids.isFiltered(),
            "removing only the item cell leaves fluid capacity and partition intact");
        check(bus.storePartial(new ItemStack(Items.gold_ingot, 1), true), "item channel accepts again after removal");
        bus.getProvider()
            .setCacheMode(false);
        check(
            bus.getCellArray(appeng.api.storage.StorageChannel.FLUIDS)
                .size() == 1,
            "native fluid cache exposed to AE");
        check(
            bus.getCellArray(appeng.api.storage.StorageChannel.ITEMS)
                .isEmpty(),
            "no phantom item cache cell");
        bus.fluids.setCacheMode(false);
        bus.inventoryHandler.setStackInSlot(1, null);
        bus.getProvider()
            .updateCell();
        bus.fluids.updateCell();
        conservation(0);
        tests.add(
            "Dual cell slots: simultaneous expansion, filters, AE cache exposure, removal isolation and native bus compatibility");
    }

    private void performance() {
        long[] ns = new long[9];
        long count = 0;
        for (int round = -4; round < 9; round++) {
            long start = System.nanoTime();
            for (int i = 0; i < 100000; i++) if (OutputOverflow.enabled(bus.getProxy())) count++;
            long elapsed = System.nanoTime() - start;
            if (round >= 0) ns[round] = elapsed;
        }
        sink = count;
        Arrays.sort(ns);
        Map<String, Object> perf = new LinkedHashMap<>();
        perf.put("scope", "warmed live AE grid, 1 optimizer; lookup microbenchmark, NOT whole-server MSPT");
        perf.put("iterations_per_sample", 100000);
        perf.put("samples", 9);
        perf.put("median_ns_per_lookup", ns[4] / 100000.0);
        perf.put("max_ns_per_lookup", ns[8] / 100000.0);
        report.put("performance", perf);
    }

    private void step() throws Exception {
        switch (tick) {
            case 1:
                setup();
                break;
            case 40:
                check(
                    bus.getProxy()
                        .isActive() && bus.fluids.getProxy() == bus.getProxy(),
                    "single active AE proxy");
                bus.buildUI(
                    new com.cleanroommc.modularui.factory.PosGuiData(player, x + 2, y, z),
                    new com.cleanroommc.modularui.value.sync.PanelSyncManager(
                        new com.cleanroommc.modularui.value.sync.ModularSyncManager(false),
                        false),
                    new com.cleanroommc.modularui.screen.UISettings(
                        com.cleanroommc.modularui.api.RecipeViewerSettings.DUMMY));
                tests.add("native GT/MUI2 menu constructed on dedicated server");
                structures();
                emit(false, true);
                break;
            case 50:
                emit(false, true);
                break;
            case 60:
                emit(false, false);
                conservation(20000);
                flush();
                stock(20000);
                world.setBlock(x + 3, y, z, OrderOptimizer.BLOCK, 0, 3);
                tests.add(
                    "optimizer absent: two 10k completions fit native overshoot, later completion rejected in both channels");
                break;
            case 90:
            case 100:
            case 110:
            case 120:
                check(OutputOverflow.enabled(bus.getProxy()), "optimizer active on same network");
                emit(false, true);
                break;
            case 130:
                conservation(40000);
                emit(true, false);
                conservation(40000);
                check(
                    !bus.storePartial(new ItemStack(Items.diamond, 1), true),
                    "item simulation unchanged when overfull");
                check(
                    !bus.fluidEndpoint.storePartial(new FluidStack(FluidRegistry.WATER, 1), true),
                    "fluid simulation unchanged when overfull");
                persistence();
                performance();
                flush();
                stock(60000);
                filters();
                tests.add(
                    "optimizer: 4 x 10k retained per 16k channel; new recipe checks and simulations reject; flushed 60k items + 60k mB conserved");
                bus.getProvider()
                    .setCheckMode(true);
                bus.fluids.setCheckMode(true);
                emit(false, true);
                break;
            case 140:
                emit(false, true);
                break;
            case 150:
                emit(true, false);
                conservation(20000);
                world.setBlockToAir(x + 3, y, z);
                break;
            case 170:
                check(!OutputOverflow.enabled(bus.getProxy()), "optimizer removed");
                emit(false, false);
                conservation(20000);
                flush();
                stock(80000);
                tests.add(
                    "strict-check mode still retains actual outputs; removing optimizer preserves buffered contents and restores native acceptance");
                finish(true);
                break;
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || done) return;
        tick++;
        try {
            step();
        } catch (Throwable t) {
            t.printStackTrace();
            report.put("error", t.toString());
            finish(false);
        }
    }

    private void finish(boolean passed) throws Exception {
        done = true;
        report.put("passed", passed);
        report.put("tests", tests);
        Files.write(
            server.getFile("mixed-output-tests.json")
                .toPath(),
            new GsonBuilder().setPrettyPrinting()
                .create()
                .toJson(report)
                .getBytes(StandardCharsets.UTF_8));
        server.initiateShutdown();
    }
}
