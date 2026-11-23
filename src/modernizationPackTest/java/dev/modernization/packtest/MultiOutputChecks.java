package dev.modernization.packtest;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.inventory.ICrafting;
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
import appeng.api.storage.ICellWorkbenchItem;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.aeoptimizer.OrderOptimizer;
import dev.aeoptimizer.dispatch.OutputOverflow;
import dev.aeoptimizer.network.TileOptimizer;
import dev.aeoptimizer.ui.OptimizerContainer;
import dev.structureterminal.output.MixedOutputBus;
import dev.structureterminal.output.MixedOutputs;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.IMEConnectable;
import gregtech.api.interfaces.IOutputBus;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;
import gregtech.common.tileentities.machines.outputme.base.MTEHatchOutputMEBase;
import gregtech.common.tileentities.machines.outputme.util.AECacheCounter;

/** Exercises native multi-output dispatch, including native/combined ports on different AE grids. */
public final class MultiOutputChecks {

    private final MinecraftServer server = MinecraftServer.getServer();
    private final WorldServer world = server.worldServerForDimension(0);
    private final FakePlayer player = FakePlayerFactory.getMinecraft(world);
    private final int x = world.getSpawnPoint().posX, y = 100, z = world.getSpawnPoint().posZ;
    private final List<String> tests = new ArrayList<>();
    private final Map<String, Object> report = new LinkedHashMap<>();
    private final List<MetaTileEntity> machines = new ArrayList<>();
    private final List<MTEHatchOutputMEBase<IAEItemStack>> items = new ArrayList<>();
    private final List<MTEHatchOutputMEBase<IAEFluidStack>> fluids = new ArrayList<>();
    private final List<IOutputBus> buses = new ArrayList<>();
    private final List<IOutputHatch> hatches = new ArrayList<>();
    private final List<MetaTileEntity> itemOwners = new ArrayList<>(), fluidOwners = new ArrayList<>();
    private TileDrive drive;
    private Item fluidCell;
    private int tick;
    private long logicalTick = 1000;
    private boolean done;

    private void check(boolean value, String msg) {
        if (!value) throw new AssertionError(msg);
    }

    private MetaTileEntity place(ItemStack stack, int dx, int dy) {
        check(
            ((ItemBlock) stack.getItem())
                .placeBlockAt(stack, player, world, x + dx, y + dy, z, 1, 0, 0, 0, stack.getItemDamage()),
            "place " + dx + "," + dy);
        MetaTileEntity m = (MetaTileEntity) ((IGregTechTileEntity) world.getTileEntity(x + dx, y + dy, z))
            .getMetaTileEntity();
        if (m instanceof IMEConnectable) ((IMEConnectable) m).setConnectsToAllSides(true);
        return m;
    }

    private void port(ItemStack stack, int dx, int dy) {
        MetaTileEntity m = place(stack, dx, dy);
        machines.add(m);
        if (m instanceof MTEHatchOutputBusME) {
            MTEHatchOutputBusME b = (MTEHatchOutputBusME) m;
            items.add(b.getProvider());
            buses.add(b);
            itemOwners.add(m);
        }
        if (m instanceof MTEHatchOutputME) {
            MTEHatchOutputME h = (MTEHatchOutputME) m;
            fluids.add(h.getProvider());
            hatches.add(h);
            fluidOwners.add(m);
        } else if (m instanceof MixedOutputBus) {
            MixedOutputBus b = (MixedOutputBus) m;
            fluids.add(b.fluids);
            hatches.add(b.fluidEndpoint);
            fluidOwners.add(m);
        }
    }

    private void setup() throws Exception {
        Block power = AEApi.instance()
            .definitions()
            .blocks()
            .energyCellCreative()
            .maybeBlock()
            .get();
        world.setBlock(x, y, z, power, 0, 3);
        world.setBlock(x, y + 5, z, power, 0, 3);
        ItemStack ds = AEApi.instance()
            .definitions()
            .blocks()
            .drive()
            .maybeStack(1)
            .get();
        Block db = Block.getBlockFromItem(ds.getItem());
        world.setBlock(x + 1, y, z, db, ds.getItemDamage(), 3);
        db.onBlockPlacedBy(world, x + 1, y, z, player, ds);
        drive = (TileDrive) world.getTileEntity(x + 1, y, z);
        fluidCell = (Item) Class.forName("com.glodblock.github.loader.ItemAndBlockHolder")
            .getField("CELL1K")
            .get(null);
        drive.getInternalInventory()
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .items()
                    .cell64k()
                    .maybeStack(1)
                    .get());
        drive.getInternalInventory()
            .setInventorySlotContents(1, new ItemStack(fluidCell));
        port(ItemList.Hatch_Output_Bus_ME.get(1), 2, 0);
        port(ItemList.Hatch_Output_Bus_ME.get(1), 3, 0);
        port(ItemList.Hatch_Output_ME.get(1), 4, 0);
        port(ItemList.Hatch_Output_ME.get(1), 5, 0);
        port(MixedOutputs.bus.getStackForm(1), 6, 0);
        port(MixedOutputs.bus.getStackForm(1), 7, 0);
        world.setBlock(x + 8, y, z, OrderOptimizer.BLOCK, 0, 3);
        port(ItemList.Hatch_Output_Bus_ME.get(1), 1, 5);
        port(ItemList.Hatch_Output_ME.get(1), 2, 5);
        port(ItemList.Hatch_Output_Bus_ME.get(1), 3, 5);
        port(ItemList.Hatch_Output_ME.get(1), 4, 5);
        reset();
    }

    private void reset() throws Exception {
        for (MetaTileEntity owner : machines) {
            owner.setInventorySlotContents(0, null);
            owner.onContentsChanged(0);
        }
        for (MTEHatchOutputMEBase<?> p : providers()) {
            // Test-only reset between independent scenarios; production transfers still use native transactions.
            Field f = MTEHatchOutputMEBase.class.getDeclaredField("cache");
            f.setAccessible(true);
            ((AECacheCounter<?>) f.get(p)).updateAll((key, n) -> 0L);
            NBTTagCompound cfg = new NBTTagCompound();
            cfg.setLong("baseCapacity", 16000);
            cfg.setInteger("refreshTime", 1000000000);
            cfg.setBoolean("additionalConnection", true);
            p.loadNBTData(cfg);
            p.setCheckMode(false);
            p.setCacheMode(false);
            p.updateCell();
        }
        advance();
    }

    private List<MTEHatchOutputMEBase<?>> providers() {
        List<MTEHatchOutputMEBase<?>> p = new ArrayList<>();
        p.addAll(items);
        p.addAll(fluids);
        return p;
    }

    private void advance() {
        logicalTick++;
        for (int i = 0; i < items.size(); i++) items.get(i)
            .onPostTick(
                itemOwners.get(i)
                    .getBaseMetaTileEntity(),
                logicalTick);
        for (int i = 0; i < fluids.size(); i++) fluids.get(i)
            .onPostTick(
                fluidOwners.get(i)
                    .getBaseMetaTileEntity(),
                logicalTick);
    }

    private ItemStack diamond(int n) {
        return new ItemStack(Items.diamond, n);
    }

    private FluidStack water(int n) {
        return new FluidStack(FluidRegistry.WATER, n);
    }

    private void prefill(int i, long n) {
        items.get(i)
            .addToCache(
                AEItemStack.create(diamond(1))
                    .setStackSize(n));
        fluids.get(i)
            .addToCache(
                AEFluidStack.create(water(1))
                    .setStackSize(n));
    }

    private long itemCount(int i) {
        return items.get(i)
            .getCachedAmount();
    }

    private long fluidCount(int i) {
        return fluids.get(i)
            .getCachedAmount();
    }

    private long itemTotal() {
        long n = 0;
        for (MTEHatchOutputMEBase<?> p : items) n += p.getCachedAmount();
        return n;
    }

    private long fluidTotal() {
        long n = 0;
        for (MTEHatchOutputMEBase<?> p : fluids) n += p.getCachedAmount();
        return n;
    }

    private int putItems(List<IOutputBus> list, ItemStack output, boolean commit) {
        ItemEjectionHelper helper = new ItemEjectionHelper(list, true, false);
        int n = helper.ejectStack(output);
        if (commit) helper.commit();
        return n;
    }

    private int putFluids(List<IOutputHatch> list, FluidStack output, boolean commit) {
        FluidEjectionHelper helper = new FluidEjectionHelper(list, true, false);
        int n = helper.ejectStack(output);
        if (commit) helper.commit();
        return n;
    }

    private int checkItems(List<IOutputBus> list, int n) {
        return new ItemEjectionHelper(list, true, true).ejectItems(Collections.singletonList(diamond(n)), 1);
    }

    private int checkFluids(List<IOutputHatch> list, int n) {
        return new FluidEjectionHelper(list, true, true).ejectFluids(Collections.singletonList(water(n)), 1);
    }

    private void baseline() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("item_caches", Arrays.asList(itemCount(0), itemCount(1)));
        state.put("fluid_caches", Arrays.asList(fluidCount(0), fluidCount(1)));
        state.put("next_item_recipe", checkItems(buses.subList(0, 2), 10000));
        state.put("next_fluid_recipe", checkFluids(hatches.subList(0, 2), 10000));
        report.put("four_completions", state);
        check(itemTotal() == 40000 && fluidTotal() == 40000, "four completions conserved");
        check(
            itemCount(1) > 0 && fluidCount(1) > 0,
            "BUG: full first port monopolizes completions while second stays empty");
        check(
            checkItems(buses.subList(0, 2), 10000) == 0 && checkFluids(hatches.subList(0, 2), 10000) == 0,
            "new recipes must stop after both ports fill");
        tests.add("four separate 10k completions fill both 16k ports; new recipes stop; no loss or duplicate outputs");
    }

    private void matrix() throws Exception {
        reset();
        prefill(0, 16000);
        prefill(1, 16000);
        advance();
        long before = itemTotal();
        check(putItems(buses.subList(0, 2), diamond(10000), false) == 10000, "stage overflow");
        check(itemTotal() == before, "uncommitted item transaction does not mutate caches");
        check(putFluids(hatches.subList(0, 2), water(10000), false) == 10000, "stage fluid overflow");
        check(fluidTotal() == before, "uncommitted fluid transaction does not mutate caches");
        check(
            putItems(buses.subList(0, 2), diamond(10000), true) == 10000
                && putFluids(hatches.subList(0, 2), water(10000), true) == 10000,
            "all-full completion");
        check(itemTotal() == 42000 && fluidTotal() == 42000, "all-full exactly once commit");
        for (int i = 0; i < 2; i++) {
            items.get(i)
                .setCheckMode(true);
            fluids.get(i)
                .setCheckMode(true);
        }
        check(
            checkItems(buses.subList(0, 2), 1) == 0 && checkFluids(hatches.subList(0, 2), 1) == 0,
            "strict full precheck");
        check(itemTotal() == 42000 && fluidTotal() == 42000, "strict precheck nonmutation");
        tests.add(
            "all-full fallback retains completion; uncommitted/simulated work is invisible; single commit and strict checks conserve quantities");
        reset();
        prefill(0, 16000);
        prefill(1, 16000);
        advance();
        for (int i = 0; i < 3; i++) {
            putItems(buses.subList(0, 4), diamond(10000), true);
            putFluids(hatches.subList(0, 4), water(10000), true);
            advance();
        }
        check(
            itemCount(0) == 16000 && itemCount(1) == 16000 && itemCount(2) > 0 && itemCount(3) > 0,
            "native + mixed item routing");
        check(
            fluidCount(0) == 16000 && fluidCount(1) == 16000 && fluidCount(2) > 0 && fluidCount(3) > 0,
            "native + mixed fluid routing");
        check(itemTotal() == 62000 && fluidTotal() == 62000, "native/mixed quantities");
        tests.add(
            "two native plus two combined output endpoints use available combined caches before overfilling native peers");
        reset();
        prefill(0, 16000);
        prefill(4, 16000);
        advance();
        check(
            !OutputOverflow.enabled(
                items.get(4)
                    .getProxy())
                && !OutputOverflow.enabled(
                    fluids.get(4)
                        .getProxy()),
            "separate grid stays disabled");
        putItems(Arrays.asList(buses.get(4), buses.get(0)), diamond(10000), true);
        putFluids(Arrays.asList(hatches.get(4), hatches.get(0)), water(10000), true);
        check(
            itemCount(4) == 16000 && fluidCount(4) == 16000 && itemCount(0) == 26000 && fluidCount(0) == 26000,
            "overflow only on authorized grid");
        tests.add("mixed grids: the optimizer-free full endpoint never receives overflow");
        nativeWithoutOptimizer();
        products();
        ordinary();
        filters();
        randomized();
        backpressure();
    }

    private void nativeWithoutOptimizer() throws Exception {
        reset();
        for (int n = 0; n < 4; n++) {
            advance();
            check(putItems(buses.subList(4, 6), diamond(10000), true) == 10000, "native no-optimizer item accept");
            check(putFluids(hatches.subList(4, 6), water(10000), true) == 10000, "native no-optimizer fluid accept");
        }
        advance();
        check(
            putItems(buses.subList(4, 6), diamond(10000), true) == 0
                && putFluids(hatches.subList(4, 6), water(10000), true) == 0,
            "optimizer-free full ports do not acquire fallback");
        check(
            itemCount(4) == 20000 && itemCount(5) == 20000 && fluidCount(4) == 20000 && fluidCount(5) == 20000,
            "native two-port quantities");
        tests.add("optimizer absent: native two-port routing retained, both full ports reject a later completion");
    }

    private void products() throws Exception {
        reset();
        ItemStack tagged = diamond(7);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("fixture", "tagged-diamond");
        tagged.setTagCompound(tag);
        List<ItemStack> out = Arrays.asList(diamond(20000), new ItemStack(Items.gold_ingot, 30000), tagged);
        FluidStack taggedWater = water(8000);
        taggedWater.tag = (NBTTagCompound) tag.copy();
        List<FluidStack> liquids = Arrays.asList(
            water(5000),
            new FluidStack(FluidRegistry.LAVA, 6000),
            new FluidStack(FluidRegistry.getFluid("oxygen"), 7000),
            taggedWater);
        ItemEjectionHelper ih = new ItemEjectionHelper(buses.subList(0, 4), true, false);
        FluidEjectionHelper fh = new FluidEjectionHelper(hatches.subList(0, 4), true, false);
        check(ih.ejectItems(out, 3) == 3 && fh.ejectFluids(liquids, 3) == 3, "all multi-product parallels accepted");
        check(itemTotal() == 0 && fluidTotal() == 0, "multi-product transaction invisible before commit");
        ih.commit();
        fh.commit();
        for (ItemStack item : out) {
            long n = 0;
            for (int i = 0; i < 4; i++) n += items.get(i)
                .getCachedAmount(AEItemStack.create(item));
            check(n == 3L * item.stackSize, "exact item/NBT identity quantity");
        }
        Map<IAEFluidStack, Long> nativeFluidKeys = new LinkedHashMap<>();
        for (FluidStack fluid : liquids) {
            IAEFluidStack key = AEFluidStack.create(fluid);
            nativeFluidKeys.put(key, nativeFluidKeys.getOrDefault(key, 0L) + 3L * fluid.amount);
        }
        boolean upstreamMerges = AEFluidStack.create(water(1))
            .equals(AEFluidStack.create(taggedWater));
        check(upstreamMerges, "pinned AE conversion behavior changed; re-evaluate fluid NBT support");
        report.put(
            "native_fluid_nbt_boundary",
            "AE2 1104 AEFluidStack.create merges custom FluidStack.tag before output routing; quantities checked by native AE keys. Item NBT remains distinct.");
        for (Map.Entry<IAEFluidStack, Long> e : nativeFluidKeys.entrySet()) {
            long n = 0;
            for (int i = 0; i < 4; i++) n += fluids.get(i)
                .getCachedAmount(e.getKey());
            check(n == e.getValue(), "exact quantity per native AE fluid key");
        }
        tests.add(
            "multiple simultaneous products: distinct item NBT preserved; registered fluids and native AE fluid-key totals commit exactly once");
        reset();
        ih = new ItemEjectionHelper(buses.subList(0, 4), true, false);
        fh = new FluidEjectionHelper(hatches.subList(0, 4), true, false);
        check(
            ih.ejectItems(Collections.singletonList(diamond(1000000000)), 3) == 3
                && fh.ejectFluids(Collections.singletonList(water(1000000000)), 3) == 3,
            "greater-than-int output split accepted");
        ih.commit();
        fh.commit();
        check(itemTotal() == 3000000000L && fluidTotal() == 3000000000L, "greater-than-int totals exact");
        int index = 0;
        for (MetaTileEntity original : machines) {
            NBTTagCompound saved = new NBTTagCompound();
            original.saveNBTData(saved);
            MetaTileEntity copy = place(original.getStackForm(1), 22 + index, 10);
            copy.loadNBTData(saved);
            if (original instanceof MTEHatchOutputBusME) check(
                ((MTEHatchOutputBusME) copy).getProvider()
                    .getCachedAmount()
                    == ((MTEHatchOutputBusME) original).getProvider()
                        .getCachedAmount(),
                "native item cache NBT");
            if (original instanceof MTEHatchOutputME) check(
                ((MTEHatchOutputME) copy).getProvider()
                    .getCachedAmount()
                    == ((MTEHatchOutputME) original).getProvider()
                        .getCachedAmount(),
                "native fluid cache NBT");
            if (original instanceof MixedOutputBus) check(
                ((MixedOutputBus) copy).fluids.getCachedAmount()
                    == ((MixedOutputBus) original).fluids.getCachedAmount(),
                "combined fluid cache NBT");
            world.setBlockToAir(x + 22 + index, y + 10, z);
            index++;
        }
        tests.add(
            "3,000,000,000 items and mB distributed without int truncation; every native/combined cache survives NBT save/load");
    }

    private void ordinary() throws Exception {
        reset();
        prefill(0, 16000);
        advance();
        MTEHatchOutputBus normal = (MTEHatchOutputBus) place(ItemList.Hatch_Output_Bus_LV.get(1), 14, 7);
        MTEHatchOutput tank = (MTEHatchOutput) place(ItemList.Hatch_Output_LV.get(1), 15, 7);
        check(putItems(Arrays.asList(buses.get(0), normal), diamond(64), true) == 64, "normal item bus accepts");
        check(putFluids(Arrays.asList(hatches.get(0), tank), water(1000), true) == 1000, "normal hatch accepts");
        long count = 0;
        for (ItemStack s : normal.mInventory) if (s != null) count += s.stackSize;
        check(
            count == 64 && tank.getFluidAmount() == 1000 && itemCount(0) == 16000 && fluidCount(0) == 16000,
            "ordinary ports precede emergency overflow");
        tests.add("ordinary output bus/hatch mixed with full ME ports: ordinary free capacity used first");
    }

    private void partition(MetaTileEntity owner, IAEStack<?> accepted) {
        ItemStack cell = accepted instanceof IAEFluidStack ? new ItemStack(fluidCell)
            : AEApi.instance()
                .definitions()
                .items()
                .cell1k()
                .maybeStack(1)
                .get();
        ((ICellWorkbenchItem) cell.getItem()).getConfigAEInventory(cell)
            .putAEStackInSlot(0, accepted);
        owner.setInventorySlotContents(0, cell);
        owner.onContentsChanged(0);
        if (owner instanceof MTEHatchOutputBusME) ((MTEHatchOutputBusME) owner).getProvider()
            .updateCell();
        if (owner instanceof MTEHatchOutputME) ((MTEHatchOutputME) owner).getProvider()
            .updateCell();
        if (owner instanceof MixedOutputBus) ((MixedOutputBus) owner).fluids.updateCell();
    }

    private void filters() throws Exception {
        reset();
        partition(itemOwners.get(0), AEItemStack.create(new ItemStack(Items.gold_ingot)));
        partition(fluidOwners.get(0), AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1)));
        prefill(1, 16000);
        advance();
        check(
            putItems(buses.subList(0, 2), diamond(10000), true) == 10000
                && putFluids(hatches.subList(0, 2), water(10000), true) == 10000,
            "matching full peer fallback");
        check(
            itemCount(0) == 0 && fluidCount(0) == 0 && itemCount(1) == 26000 && fluidCount(1) == 26000,
            "incompatible empty port not used");
        tests.add(
            "filtered incompatible empty peer is skipped; correct full peer receives fallback without bypassing filters");
        reset();
        partition(itemOwners.get(0), AEItemStack.create(diamond(1)));
        partition(fluidOwners.get(0), AEFluidStack.create(water(1)));
        items.get(0)
            .addToCache(
                AEItemStack.create(diamond(1))
                    .setStackSize(
                        items.get(0)
                            .getCacheCapacity()));
        fluids.get(0)
            .addToCache(
                AEFluidStack.create(water(1))
                    .setStackSize(
                        fluids.get(0)
                            .getCacheCapacity()));
        advance();
        long ib = itemCount(0), fb = fluidCount(0);
        putItems(buses.subList(0, 2), diamond(10000), true);
        putFluids(hatches.subList(0, 2), water(10000), true);
        check(
            itemCount(0) == ib && fluidCount(0) == fb && itemCount(1) == 10000 && fluidCount(1) == 10000,
            "free unfiltered endpoint precedes full filtered endpoint");
        tests.add("priority categories: full filtered ME port does not hide free unfiltered capacity");
    }

    private void randomized() throws Exception {
        Random r = new Random(6192026);
        for (int c = 0; c < 200; c++) {
            reset();
            List<IOutputBus> bs = new ArrayList<>(buses.subList(0, 4));
            List<IOutputHatch> hs = new ArrayList<>(hatches.subList(0, 4));
            Collections.shuffle(bs, r);
            Collections.shuffle(hs, r);
            for (int i = 0; i < 4; i++) {
                prefill(i, r.nextInt(40001));
                items.get(i)
                    .setCheckMode((c & 1) != 0);
                fluids.get(i)
                    .setCheckMode((c & 1) != 0);
            }
            advance();
            long ib = itemTotal(), fb = fluidTotal();
            int n = 1 + r.nextInt(120000);
            check(
                putItems(bs, diamond(n), true) == n && putFluids(hs, water(n), true) == n,
                "random full acceptance " + c);
            check(itemTotal() == ib + n && fluidTotal() == fb + n, "random quantity conservation " + c);
        }
        tests.add(
            "200 seeded configurations: reordered native/combined ports, empty/partial/overfull caches, strict mode on/off; per-channel quantity conserved");
    }

    private void backpressure() throws Exception {
        reset();
        ItemStack i = drive.getInternalInventory()
            .getStackInSlot(0),
            f = drive.getInternalInventory()
                .getStackInSlot(1);
        drive.getInternalInventory()
            .setInventorySlotContents(0, null);
        drive.getInternalInventory()
            .setInventorySlotContents(1, null);
        for (int n = 0; n < 4; n++) prefill(n, 20000);
        for (MTEHatchOutputMEBase<?> p : providers()) p.flushCachedStack();
        check(itemTotal() == 80000 && fluidTotal() == 80000, "no destination storage retains all buffers");
        drive.getInternalInventory()
            .setInventorySlotContents(0, i);
        drive.getInternalInventory()
            .setInventorySlotContents(1, f);
        for (MTEHatchOutputMEBase<?> p : providers()) p.flushCachedStack();
        check(itemTotal() == 0 && fluidTotal() == 0, "restored storage drains all buffers");
        long ni = drive.getProxy()
            .getStorage()
            .getItemInventory()
            .getStorageList()
            .findPrecise(AEItemStack.create(diamond(1)))
            .getStackSize();
        long nf = drive.getProxy()
            .getStorage()
            .getFluidInventory()
            .getStorageList()
            .findPrecise(AEFluidStack.create(water(1)))
            .getStackSize();
        check(ni == 80000 && nf == 80000, "flush exactly once across all output ports");
        tests.add(
            "AE destination storage removed/restored: 80k per channel retained then flushed exactly once across four ports");
    }

    private void outputSwitches() throws Exception {
        TileOptimizer primary = (TileOptimizer) world.getTileEntity(x + 8, y, z);
        TileOptimizer secondary = (TileOptimizer) world.getTileEntity(x + 5, y + 5, z);
        check(primary.outputOverflow && secondary.outputOverflow, "new optimizer defaults on");
        TileOptimizer restored = new TileOptimizer();
        restored.outputOverflow = false;
        restored.readSettings(new NBTTagCompound());
        check(restored.outputOverflow, "old world without switch retains protection");
        tests.add("output switch defaults ON for new optimizers and legacy NBT without the setting");

        player.setPosition(x + 8.5, y + .5, z + .5);
        OptimizerContainer menu = new OptimizerContainer(primary);
        OptimizerContainer mirror = new OptimizerContainer(primary);
        int[] syncs = { 0 };
        menu.addCraftingToCrafters(
            (ICrafting) Proxy.newProxyInstance(
                ICrafting.class.getClassLoader(),
                new Class<?>[] { ICrafting.class },
                (proxy, method, args) -> {
                    if (method.getName()
                        .equals("sendProgressBarUpdate")
                        || method.getName()
                            .equals("func_71112_a")) {
                        int id = (Integer) args[1];
                        mirror.updateProgressBar(id, (Integer) args[2]);
                        if (id == 4) syncs[0]++;
                    }
                    return null;
                }));
        boolean detail = primary.detailed, rename = primary.autoRename, encode = primary.autoEncodeRename;
        check(menu.enchantItem(player, 3) && !primary.outputOverflow, "server action disables output protection");
        check(syncs[0] > 0 && !mirror.outputOverflow, "open viewers receive OFF");
        check(
            primary.detailed == detail && primary.autoRename == rename && primary.autoEncodeRename == encode,
            "output switch does not change report or rename settings");
        NBTTagCompound saved = new NBTTagCompound();
        primary.writeToNBT(saved);
        restored.readFromNBT(saved);
        check(!restored.outputOverflow, "OFF survives complete tile NBT round trip");
        check(!menu.enchantItem(player, 4) && !primary.outputOverflow, "invalid action rejected");
        player.setPosition(x + 100, y, z);
        check(!menu.enchantItem(player, 3) && !primary.outputOverflow, "remote player cannot toggle");
        player.setPosition(x + 8.5, y + .5, z + .5);
        tests
            .add("server-authoritative GUI toggle, observer sync, OFF persistence, distance and invalid-action guards");

        for (int i = 0; i < 4; i++) check(
            !OutputOverflow.enabled(
                items.get(i)
                    .getProxy())
                && !OutputOverflow.enabled(
                    fluids.get(i)
                        .getProxy()),
            "native and dual channels all disabled");
        check(
            OutputOverflow.enabled(
                items.get(4)
                    .getProxy())
                && OutputOverflow.enabled(
                    fluids.get(4)
                        .getProxy()),
            "independent network retains its own ON setting");
        tests.add("per-network isolation: disabling one optimizer leaves a second network protected");

        for (int mode = 0; mode < 2; mode++) {
            reset();
            for (int i = 0; i < 4; i++) {
                prefill(i, 40000);
                items.get(i)
                    .setCheckMode(mode != 0);
                fluids.get(i)
                    .setCheckMode(mode != 0);
            }
            advance();
            for (int i = 0; i < 4; i++) {
                check(
                    putItems(Collections.singletonList(buses.get(i)), diamond(1000), true) == 0
                        && putFluids(Collections.singletonList(hatches.get(i)), water(1000), true) == 0,
                    "disabled full native/dual transaction rejects later completion " + i + ":" + mode);
                IAEItemStack item = AEItemStack.create(diamond(1000));
                IAEFluidStack fluid = AEFluidStack.create(water(1000));
                check(
                    !items.get(i)
                        .storePartial(item, false) && item.getStackSize() == 1000
                        && !fluids.get(i)
                            .storePartial(fluid, false)
                        && fluid.getStackSize() == 1000,
                    "disabled direct provider keeps rejected input intact " + i + ":" + mode);
                check(
                    checkItems(Collections.singletonList(buses.get(i)), 1000) == 0
                        && checkFluids(Collections.singletonList(hatches.get(i)), 1000) == 0,
                    "overfull precheck rejects without negative insertion " + i + ":" + mode);
            }
            check(itemTotal() == 160000 && fluidTotal() == 160000, "OFF never truncates or duplicates excess");
        }
        tests.add(
            "OFF with 40k/16k caches: native/dual direct and transaction paths reject later outputs; strict and normal prechecks preserve every item and mB");

        reset();
        for (int i = 0; i < 4; i++) {
            if (i % 2 == 0) prefill(i, 40000);
            items.get(i)
                .setCheckMode(true);
            fluids.get(i)
                .setCheckMode(true);
        }
        advance();
        for (int i = 0; i < 4; i += 2) {
            check(
                checkItems(buses.subList(i, i + 2), 16000) == 1 && checkFluids(hatches.subList(i, i + 2), 16000) == 1,
                "disabled overfull first port cannot subtract free second-port capacity " + i);
            check(
                putItems(buses.subList(i, i + 2), diamond(16000), true) == 16000
                    && putFluids(hatches.subList(i, i + 2), water(16000), true) == 16000,
                "disabled routing still uses remaining ordinary capacity " + i);
            check(
                itemCount(i) == 40000 && fluidCount(i) == 40000
                    && itemCount(i + 1) == 16000
                    && fluidCount(i + 1) == 16000,
                "no negative-space loss or duplicate on available peers " + i);
        }
        tests.add(
            "OFF strict multi-output: an overfilled first port does not hide free peers or create negative acceptance");

        reset();
        // The previous backpressure test already placed 80k per channel in the drive.
        for (int i = 0; i < 4; i++) prefill(i, 40000);
        for (MTEHatchOutputMEBase<?> p : providers()) p.flushCachedStack();
        check(itemTotal() == 0 && fluidTotal() == 0, "OFF still flushes every overfilled cache");
        long ni = drive.getProxy()
            .getStorage()
            .getItemInventory()
            .getStorageList()
            .findPrecise(AEItemStack.create(diamond(1)))
            .getStackSize();
        long nf = drive.getProxy()
            .getStorage()
            .getFluidInventory()
            .getStorageList()
            .findPrecise(AEFluidStack.create(water(1)))
            .getStackSize();
        check(ni == 240000 && nf == 240000, "OFF excess delivered once with quantity conserved");
        tests.add("OFF drains existing excess to AE: 160k per channel delivered exactly once, on top of the prior 80k");

        check(menu.enchantItem(player, 3) && primary.outputOverflow && mirror.outputOverflow, "GUI re-enables live");
        primary.writeToNBT(saved);
        restored.readFromNBT(saved);
        check(restored.outputOverflow, "ON persists too");
        secondary.outputOverflow = false;
        for (int i = 0; i < 4; i++) prefill(i, 16000);
        advance();
        for (int i = 0; i < 4; i++) check(
            putItems(Collections.singletonList(buses.get(i)), diamond(1000), true) == 1000
                && putFluids(Collections.singletonList(hatches.get(i)), water(1000), true) == 1000,
            "re-enabled native/dual completions accepted " + i);
        check(itemTotal() == 68000 && fluidTotal() == 68000, "re-enabled exactly once");
        check(
            !OutputOverflow.enabled(
                items.get(4)
                    .getProxy())
                && !OutputOverflow.enabled(
                    fluids.get(4)
                        .getProxy()),
            "primary ON does not override secondary OFF");
        tests.add(
            "re-enable takes effect live on native/dual outputs, synchronizes/persists ON, and leaves another network OFF");
    }

    private void step() throws Exception {
        if (tick == 1) setup();
        if (tick == 40) {
            for (int i = 0; i < 4; i++) check(
                OutputOverflow.enabled(
                    items.get(i)
                        .getProxy())
                    && OutputOverflow.enabled(
                        fluids.get(i)
                            .getProxy()),
                "primary grid powered");
        }
        if (tick == 40 || tick == 50 || tick == 60 || tick == 70) {
            advance();
            check(
                putItems(buses.subList(0, 2), diamond(10000), true) == 10000
                    && putFluids(hatches.subList(0, 2), water(10000), true) == 10000,
                "four completions accepted");
        }
        if (tick == 80) {
            baseline();
            matrix();
            world.setBlock(x + 5, y + 5, z, OrderOptimizer.BLOCK, 0, 3);
        }
        if (tick == 110) {
            outputSwitches();
            finish(true);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent e) throws Exception {
        if (e.phase != TickEvent.Phase.END || done) return;
        tick++;
        try {
            step();
        } catch (Throwable t) {
            t.printStackTrace();
            report.put("error", t.toString());
            finish(false);
        }
    }

    private void finish(boolean ok) throws Exception {
        done = true;
        report.put("passed", ok);
        report.put("tests", tests);
        Files.write(
            server.getFile("multi-output-tests.json")
                .toPath(),
            new GsonBuilder().setPrettyPrinting()
                .create()
                .toJson(report)
                .getBytes(StandardCharsets.UTF_8));
        server.initiateShutdown();
    }
}
