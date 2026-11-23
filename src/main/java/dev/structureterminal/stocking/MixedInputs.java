package dev.structureterminal.stocking;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public final class MixedInputs {

    public static final int GUI = 38;
    public static MixedInputBus basic, advanced;

    private MixedInputs() {}

    public static void register() {
        basic = new MixedInputBus(
            dev.structureterminal.TerminalConfig.mixedInputId,
            false,
            "modernization.mixed_input",
            "ME Dual Stocking Input Bus");
        advanced = new MixedInputBus(
            dev.structureterminal.TerminalConfig.mixedAdvancedInputId,
            true,
            "modernization.mixed_input_advanced",
            "Advanced ME Dual Stocking Input Bus");
        GameRegistry.addShapelessRecipe(
            basic.getStackForm(1),
            ItemList.Hatch_Input_Bus_ME.get(1),
            ItemList.Hatch_Input_ME.get(1));
        GameRegistry.addShapelessRecipe(
            advanced.getStackForm(1),
            ItemList.Hatch_Input_Bus_ME_Advanced.get(1),
            ItemList.Hatch_Input_ME_Advanced.get(1));
    }

    public static MixedInputBus at(World world, int x, int y, int z) {
        TileEntity t = world.getTileEntity(x, y, z);
        if (!(t instanceof IGregTechTileEntity)) return null;
        Object m = ((IGregTechTileEntity) t).getMetaTileEntity();
        return m instanceof MixedInputBus ? (MixedInputBus) m : null;
    }
}
