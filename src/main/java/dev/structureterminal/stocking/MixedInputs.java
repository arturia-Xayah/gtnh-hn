package dev.structureterminal.stocking;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.RecipeSorter;

import com.glodblock.github.loader.ItemAndBlockHolder;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;
import dev.structureterminal.TerminalConfig;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;

public final class MixedInputs {

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
    }

    public static void registerRecipes() {
        RecipeSorter.register(
            "structureterminal:clear_mixed_input",
            ClearMixedInputRecipe.class,
            RecipeSorter.Category.SHAPELESS,
            "after:minecraft:shapeless");
        GameRegistry.addRecipe(new ClearMixedInputRecipe(basic.getStackForm(1)));
        GameRegistry.addRecipe(new ClearMixedInputRecipe(advanced.getStackForm(1)));
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Hatch_Input_Bus_ME.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.IV), 4 },
                AEApi.instance()
                    .definitions()
                    .materials()
                    .cell16kPart()
                    .maybeStack(16)
                    .get(),
                new ItemStack(ItemAndBlockHolder.CELL_PART, 16, 2))
            .nbtSensitive()
            .itemOutputs(basic.getStackForm(1))
            .duration(60 * GTRecipeBuilder.SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(
                basic.getStackForm(1),
                new Object[] { OrePrefixes.circuit.get(Materials.IV), 16 },
                AEApi.instance()
                    .definitions()
                    .materials()
                    .cell64kPart()
                    .maybeStack(8)
                    .get(),
                new ItemStack(ItemAndBlockHolder.CELL_PART, 8, 3))
            .nbtSensitive()
            .itemOutputs(advanced.getStackForm(1))
            .duration(120 * GTRecipeBuilder.SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    public static boolean owns(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof gregtech.common.blocks.ItemMachines)) return false;
        int id = stack.getItemDamage();
        return id == TerminalConfig.mixedInputId || id == TerminalConfig.mixedAdvancedInputId
            || id == TerminalConfig.mixedOutputId;
    }

}
