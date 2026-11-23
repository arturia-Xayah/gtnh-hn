package dev.modernization.packtest;

import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.AEApi;
import appeng.api.networking.storage.IStorageGrid;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import dev.aeoptimizer.OrderOptimizer;
import dev.aeoptimizer.dispatch.OutputOverflow;
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
                dev.structureterminal.stocking.MixedInputContainer menu = new dev.structureterminal.stocking.MixedInputContainer(
                    player,
                    advanced);
                check(menu.canInteractWith(player), "stocking menu stays open");
                check(menu.canDragIntoSlot(menu.getSlot(1)), "manual slot supports native AE dragging");
                menu.detectAndSendChanges();
                tests.add("mixed stocking server container opens, synchronizes and supports manual-slot dragging");
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
