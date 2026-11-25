package dev.modernization.packtest;

import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.loader.ItemAndBlockHolder;

import appeng.api.AEApi;
import appeng.api.networking.storage.IStorageGrid;
import appeng.container.ContainerNull;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import dev.aeoptimizer.OrderOptimizer;
import dev.aeoptimizer.dispatch.OutputOverflow;
import dev.structureterminal.TerminalConfig;
import dev.structureterminal.stocking.MixedInputBus;
import dev.structureterminal.stocking.MixedInputs;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.IMEConnectable;
import gregtech.api.interfaces.IOutputBusTransaction;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.interfaces.IOutputTransaction;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;

final class MachineChecks {

    private final WorldServer world;
    private final FakePlayer player;
    private final int x, y, z;
    private final List<String> tests;
    private final IStorageGrid subnet;
    private final MTEHatchOutputBusME output;
    private final MTEHatchOutputME fluidOutput;
    private final MixedInputBus mixed, advanced;
    private final MTEMultiBlockBase controller;

    MachineChecks(WorldServer world, FakePlayer player, TileDrive drive, int x, int y, int z, List<String> tests)
        throws Exception {
        this.world = world;
        this.player = player;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tests = tests;
        checkAssemblerRecipe(
            ItemList.Hatch_Input_Bus_ME.get(1),
            MixedInputs.basic,
            AEApi.instance()
                .definitions()
                .materials()
                .cell16kPart()
                .maybeStack(16)
                .get(),
            new ItemStack(ItemAndBlockHolder.CELL_PART, 16, 2),
            new ItemStack(ItemAndBlockHolder.CELL16K, 16),
            4,
            1200);
        checkAssemblerRecipe(
            MixedInputs.basic.getStackForm(1),
            MixedInputs.advanced,
            AEApi.instance()
                .definitions()
                .materials()
                .cell64kPart()
                .maybeStack(8)
                .get(),
            new ItemStack(ItemAndBlockHolder.CELL_PART, 8, 3),
            new ItemStack(ItemAndBlockHolder.CELL64K, 8),
            16,
            2400);
        checkInputPair(world, ItemList.Hatch_Input_Bus_ME, ItemList.Hatch_Input_ME, MixedInputs.basic);
        checkInputPair(
            world,
            ItemList.Hatch_Input_Bus_ME_Advanced,
            ItemList.Hatch_Input_ME_Advanced,
            MixedInputs.advanced);
        checkInputPair(world, ItemList.Hatch_Input_Bus_ME, ItemList.Hatch_Input_ME_Advanced, null);
        checkInputPair(world, ItemList.Hatch_Input_Bus_ME_Advanced, ItemList.Hatch_Input_ME, null);
        tests.add(
            TerminalConfig.shapelessDualMEInputs
                ? "Shapeless: both same-tier pairs craft in both orders; mixed tiers and former assembler recipes rejected"
                : "Default: both EV assembler recipes consume exact quantities; no shapeless input pairs");
        checkCraftingReset(MixedInputs.basic, 2, world);
        checkCraftingReset(MixedInputs.advanced, 3, world);
        tests.add("2x2 and 3x3 reset for both tiers, retaining the name, cover, upgrade and manual slot");
        subnet = drive.getProxy()
            .getStorage();
        drive.getInternalInventory()
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .items()
                    .cell64k()
                    .maybeStack(1)
                    .get());
        output = (MTEHatchOutputBusME) machine(ItemList.Hatch_Output_Bus_ME.get(1), 4);
        fluidOutput = (MTEHatchOutputME) machine(ItemList.Hatch_Output_ME.get(1), 5);
        mixed = (MixedInputBus) machine(MixedInputs.basic.getStackForm(1), 6);
        advanced = (MixedInputBus) machine(MixedInputs.advanced.getStackForm(1), 7);
        controller = (MTEMultiBlockBase) machine(ItemList.Machine_Multi_LargeChemicalReactor.get(1), 10);
        NBTTagCompound config = new NBTTagCompound();
        config.setLong("baseCapacity", 16000);
        config.setBoolean("additionalConnection", true);
        config.setInteger("refreshTime", 10000);
        output.getProvider()
            .loadNBTData(config);
        fluidOutput.getProvider()
            .loadNBTData(config);
    }

    private static void checkInputPair(WorldServer world, ItemList bus, ItemList hatch, MixedInputBus output) {
        for (int size : new int[] { 2, 3 }) {
            InventoryCrafting grid = new InventoryCrafting(new ContainerNull(), size, size);
            for (boolean swapped : new boolean[] { false, true }) {
                grid.setInventorySlotContents(0, (swapped ? hatch : bus).get(1));
                grid.setInventorySlotContents(size * size - 1, (swapped ? bus : hatch).get(1));
                ItemStack result = CraftingManager.getInstance()
                    .findMatchingRecipe(grid, world);
                if (output != null && TerminalConfig.shapelessDualMEInputs) check(
                    ItemStack.areItemStacksEqual(result, output.getStackForm(1)),
                    "same-tier pair crafting result");
                else check(result == null || !MixedInputs.owns(result), "unexpected dual bus from input pair");
            }
        }
    }

    private static void checkAssemblerRecipe(ItemStack bus, MixedInputBus output, ItemStack itemPart,
        ItemStack fluidPart, ItemStack finishedFluidCell, int circuits, int ticks) {
        ItemStack[] inputs = { bus, ItemList.Circuit_Elitenanocomputer.get(circuits), itemPart, fluidPart };
        GTRecipe recipe = RecipeMaps.assemblerRecipes.findRecipeQuery()
            .items(inputs)
            .voltage(2048)
            .find();
        if (TerminalConfig.shapelessDualMEInputs) {
            check(
                recipe == null || !recipe.mOutputs[0].isItemEqual(output.getStackForm(1)),
                "old assembler recipe still present");
            return;
        }
        check(recipe != null && recipe.mOutputs[0].isItemEqual(output.getStackForm(1)), "find mixed input recipe");
        check(
            recipe.mDuration == ticks && recipe.mEUt == 1920 && recipe.mFluidInputs.length == 0,
            "mixed input assembler time, power and fluids");
        FluidStack[] dry = new FluidStack[0];
        fluidPart.stackSize--;
        check(!recipe.isRecipeInputEqual(false, false, dry, inputs), "one missing fluid component");
        fluidPart.stackSize++;
        inputs[3] = finishedFluidCell;
        check(!recipe.isRecipeInputEqual(false, false, dry, inputs), "finished cell cannot replace a component");
        inputs[3] = fluidPart;
        check(recipe.isRecipeInputEqual(true, false, dry, inputs), "consume mixed input recipe");
        for (ItemStack stack : inputs) check(stack.stackSize == 0, "exact mixed input ingredient counts");
    }

    private static void checkCraftingReset(MixedInputBus type, int size, WorldServer world) {
        InventoryCrafting grid = new InventoryCrafting(new ContainerNull(), size, size);
        ItemStack bus = type.getStackForm(1);
        int pos = grid.getSizeInventory() - 1;
        grid.setInventorySlotContents(pos, bus);
        CraftingManager manager = CraftingManager.getInstance();
        check(manager.findMatchingRecipe(grid, world) == null, "clean bus has no reset loop");
        NBTTagCompound config = new NBTTagCompound();
        config.setTag("mixedFilters", new NBTTagCompound());
        config.setBoolean("autoStock", true);
        config.setInteger("refreshTime", 800);
        config.setByte("mColor", (byte) 12);
        bus.setTagCompound(config);
        ItemStack reset = manager.findMatchingRecipe(grid, world);
        check(ItemStack.areItemStacksEqual(reset, type.getStackForm(1)), "reset yields plain bus of same tier");
        check(config.getBoolean("autoStock") && config.hasKey("mixedFilters"), "preview changed source");
        bus.setStackDisplayName("Assembly");
        config.setByte("mOtherUpgrades", (byte) 1);
        config.setTag("mCoverData", new NBTTagCompound());
        config.setInteger("version", 1);
        config.setBoolean("migrationCircuitSlot", true);
        NBTTagList slots = new NBTTagList();
        NBTTagCompound manual = new ItemStack(Items.diamond, 3).writeToNBT(new NBTTagCompound());
        manual.setInteger("IntSlot", 1);
        slots.appendTag(manual);
        NBTTagCompound circuit = GTUtility.getIntegratedCircuit(12)
            .writeToNBT(new NBTTagCompound());
        circuit.setInteger("IntSlot", 0);
        slots.appendTag(circuit);
        config.setTag("Inventory", slots);
        reset = manager.findMatchingRecipe(grid, world);
        check(reset != null && reset.stackSize == 1 && reset.isItemEqual(bus), "decorated bus reset");
        NBTTagCompound kept = reset.getTagCompound();
        check(
            reset.getDisplayName()
                .equals("Assembly") && kept.getByte("mOtherUpgrades") == 1
                && kept.hasKey("mCoverData")
                && !kept.hasKey("mixedFilters")
                && !kept.hasKey("autoStock")
                && !kept.hasKey("refreshTime")
                && !kept.hasKey("mColor"),
            "only bus configuration removed");
        NBTTagList remaining = kept.getTagList("Inventory", 10);
        check(
            remaining.tagCount() == 1 && manual.equals(remaining.getCompoundTagAt(0)) && slots.tagCount() == 2,
            "real item retained and virtual circuit cleared");
        grid.setInventorySlotContents(0, new ItemStack(Items.apple));
        check(manager.findMatchingRecipe(grid, world) == null, "extra input rejected");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private MetaTileEntity machine(ItemStack stack, int dx) {
        check(
            ((ItemBlock) stack.getItem())
                .placeBlockAt(stack, player, world, x + dx, y, z, 1, 0, 0, 0, stack.getItemDamage()),
            "place " + dx);
        IGregTechTileEntity base = (IGregTechTileEntity) world.getTileEntity(x + dx, y, z);
        if (base.getMetaTileEntity() instanceof IMEConnectable)
            ((IMEConnectable) base.getMetaTileEntity()).setConnectsToAllSides(true);
        return (MetaTileEntity) base.getMetaTileEntity();
    }

    private void eject(boolean recipeCheck) {
        IOutputBusTransaction tx = output.createTransaction();
        ((IOutputTransaction.IRecipeCheckAware) tx).setRecipeCheck(recipeCheck);
        ItemStack item = new ItemStack(Items.diamond, 10000);
        boolean accepted = tx.storePartial(GTUtility.ItemId.createNoCopy(item), item, 10000, 10000);
        check(
            recipeCheck ? !accepted && item.stackSize == 10000 : accepted && item.stackSize == 0,
            "item output transaction");
        if (!recipeCheck) tx.commit();
        IOutputHatchTransaction ft = fluidOutput.createTransaction();
        ((IOutputTransaction.IRecipeCheckAware) ft).setRecipeCheck(recipeCheck);
        FluidStack water = new FluidStack(FluidRegistry.WATER, 10000);
        accepted = ft.storePartial(GTUtility.FluidId.create(water), water, 10000, 10000);
        check(
            recipeCheck ? !accepted && water.amount == 10000 : accepted && water.amount == 0,
            "fluid output transaction");
        if (!recipeCheck) ft.commit();
    }

    boolean step(int tick) throws Exception {
        switch (tick) {
            case 40:
                check(
                    output.getProxy()
                        .isActive()
                        && fluidOutput.getProxy()
                            .isActive()
                        && mixed.getProxy()
                            .isActive()
                        && advanced.getProxy()
                            .isActive(),
                    "ME automation powered");
                check(
                    output.getProvider()
                        .getCacheCapacity() == 16000,
                    "16k capacity");
                eject(false);
                break;
            case 50:
                eject(false);
                break;
            case 60:
                check(!output.storePartial(new ItemStack(Items.diamond, 10000), false), "native full item cache");
                check(
                    fluidOutput.fill(new FluidStack(FluidRegistry.WATER, 10000), true) == 0,
                    "native full fluid cache");
                tests.add("without optimizer, full outputs reject a later completion on another tick");
                output.getProvider()
                    .flushCachedStack();
                fluidOutput.getProvider()
                    .flushCachedStack();
                world.setBlock(x + 8, y, z, OrderOptimizer.BLOCK, 0, 3);
                break;
            case 90:
            case 100:
            case 110:
            case 120:
                check(OutputOverflow.enabled(output.getProxy()), "optimizer on output grid");
                eject(false);
                break;
            case 130:
                check(
                    output.getProvider()
                        .getCachedAmount() == 40000
                        && fluidOutput.getProvider()
                            .getCachedAmount() == 40000,
                    "four 10k completions retained");
                eject(true);
                output.getProvider()
                    .setCheckMode(true);
                fluidOutput.getProvider()
                    .setCheckMode(true);
                eject(true);
                output.getProvider()
                    .setCheckMode(false);
                fluidOutput.getProvider()
                    .setCheckMode(false);
                tests.add(
                    "native strict output checks clamp negative free space to zero without changing pending amounts");
                NBTTagCompound saved = new NBTTagCompound();
                output.saveNBTData(saved);
                check(
                    saved.getTagList("cache", 10)
                        .tagCount() == 1,
                    "overflow persisted");
                output.getProvider()
                    .flushCachedStack();
                fluidOutput.getProvider()
                    .flushCachedStack();
                check(
                    output.getProvider()
                        .getCachedAmount() == 0
                        && fluidOutput.getProvider()
                            .getCachedAmount() == 0,
                    "overflow flush");
                check(
                    subnet.getItemInventory()
                        .getStorageList()
                        .findPrecise(AEItemStack.create(new ItemStack(Items.diamond)))
                        .getStackSize() == 60000,
                    "no lost diamonds");
                tests.add(
                    "optimizer: 40k retained by 16k item/fluid outputs, new recipe rejected, flush conserved 60k total");
                mixed.setFilter(0, AEItemStack.create(new ItemStack(Items.diamond)));
                mixed.setFilter(1, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)));
                controller.mInputBusses.add(mixed);
                controller.startRecipeProcessing();
                check(
                    controller.getStoredInputsFromME()
                        .size() == 1 && controller.getStoredFluidsFromME()
                            .containsKey(FluidRegistry.WATER),
                    "mixed recipe inputs");
                controller.getStoredInputsFromME()
                    .values()
                    .iterator()
                    .next().stackSize -= 7;
                controller.getStoredFluidsFromME()
                    .get(FluidRegistry.WATER).amount -= 123;
                controller.endRecipeProcessing();
                check(
                    subnet.getItemInventory()
                        .getStorageList()
                        .findPrecise(AEItemStack.create(new ItemStack(Items.diamond)))
                        .getStackSize() == 59993,
                    "mixed item consumption");
                long water = subnet.getFluidInventory()
                    .getStorageList()
                    .findPrecise(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1)))
                    .getStackSize();
                check(water == 60377, "mixed fluid consumption=" + water);
                NBTTagCompound config = new NBTTagCompound();
                mixed.saveNBTData(config);
                check(config.hasKey("mixedFilters"), "mixed filters saved");
                advanced.setAutoPullItemList(true);
                tests.add("dual input: item/fluid recipe lists, exact consumption and persisted filters");
                break;
            case 250:
                boolean hasItem = false, hasFluid = false;
                for (int i = 0; i < 16; i++) {
                    Object marker = advanced.filters.getAEStackInSlot(i);
                    hasItem |= marker instanceof appeng.api.storage.data.IAEItemStack;
                    hasFluid |= marker instanceof appeng.api.storage.data.IAEFluidStack;
                }
                check(
                    hasItem && hasFluid,
                    "advanced mixed auto pull item=" + hasItem
                        + ", fluid="
                        + hasFluid
                        + ", active="
                        + advanced.isActive()
                        + ", allowed="
                        + advanced.isAllowedToWork()
                        + ", auto="
                        + advanced.isAutoPullItemList());
                tests.add("advanced dual input auto-pull selects both item and fluid types");
                player.setPosition(x + 7.5, y + .5, z + .5);
                advanced.buildUI(
                    new com.cleanroommc.modularui.factory.PosGuiData(
                        player,
                        advanced.getBaseMetaTileEntity()
                            .getXCoord(),
                        advanced.getBaseMetaTileEntity()
                            .getYCoord(),
                        advanced.getBaseMetaTileEntity()
                            .getZCoord()),
                    new com.cleanroommc.modularui.value.sync.PanelSyncManager(
                        new com.cleanroommc.modularui.value.sync.ModularSyncManager(false),
                        false),
                    new com.cleanroommc.modularui.screen.UISettings(
                        com.cleanroommc.modularui.api.RecipeViewerSettings.DUMMY));
                mixed.setSlotConfig(
                    1,
                    gregtech.api.util.GTUtility.getFluidDisplayStack(new FluidStack(FluidRegistry.WATER, 1), false));
                check(
                    mixed.filters.getAEStackInSlot(1) instanceof appeng.api.storage.data.IAEFluidStack,
                    "native ghost slot retains fluid identity");
                tests.add("native GT stocking ModularUI builds with dual filters");
                NBTTagCompound copied = mixed.getCopiedData(player);
                check(advanced.pasteCopiedData(player, copied), "copy mixed configuration");
                check(
                    !advanced.isAutoPullItemList()
                        && advanced.filters.getAEStackInSlot(0) instanceof appeng.api.storage.data.IAEItemStack
                        && advanced.filters.getAEStackInSlot(1) instanceof appeng.api.storage.data.IAEFluidStack,
                    "mixed configuration restored");
                NBTTagCompound empty = (NBTTagCompound) copied.copy();
                empty.setTag("mixedFilters", new NBTTagCompound());
                check(
                    advanced.pasteCopiedData(player, empty) && advanced.filters.isEmpty(),
                    "empty copy clears all old filters");
                NBTTagCompound nativeCopy = (NBTTagCompound) copied.copy();
                nativeCopy.removeTag("mixedFilters");
                check(
                    advanced.pasteCopiedData(player, nativeCopy)
                        && advanced.filters.getAEStackInSlot(0) instanceof appeng.api.storage.data.IAEItemStack
                        && advanced.filters.getAEStackInSlot(1) == null,
                    "native bus configuration import");
                tests.add(
                    "mixed copy/paste preserves item and fluid; empty copy clears old filters; native item-only configuration imports");
                return true;
            default:
                break;
        }
        return false;
    }
}
