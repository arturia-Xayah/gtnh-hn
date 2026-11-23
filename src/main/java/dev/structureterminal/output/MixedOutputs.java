package dev.structureterminal.output;

import cpw.mods.fml.common.registry.GameRegistry;
import dev.structureterminal.TerminalConfig;
import gregtech.api.enums.ItemList;

public final class MixedOutputs {

    public static MixedOutputBus bus;

    private MixedOutputs() {}

    public static void register() {
        bus = new MixedOutputBus(TerminalConfig.mixedOutputId);
        GameRegistry.addShapelessRecipe(
            bus.getStackForm(1),
            ItemList.Hatch_Output_Bus_ME.get(1),
            ItemList.Hatch_Output_ME.get(1));
    }
}
