package dev.structureterminal.stocking;

import java.util.Collections;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

/** Clear the input settings on one bus while retaining covers, upgrades, names and physical items. */
final class ClearMixedInputRecipe extends ShapelessRecipes {

    private static final String[] SETTINGS = { "mixedFilters", "slots", "sizes", "autoStock", "minAutoPullStackSize",
        "refreshTime", "additionalConnection", "mColor", "disableSort", "disableFilter", "disableLimited",
        "recipeMap" };

    ClearMixedInputRecipe(ItemStack bus) {
        super(bus, Collections.singletonList(bus.copy()));
    }

    @Override
    public boolean matches(InventoryCrafting grid, World world) {
        return getCraftingResult(grid) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting grid) {
        ItemStack bus = null;
        for (int i = 0; i < grid.getSizeInventory(); i++) {
            ItemStack stack = grid.getStackInSlot(i);
            if (stack == null) continue;
            if (bus != null || !getRecipeOutput().isItemEqual(stack) || !stack.hasTagCompound()) return null;
            bus = stack;
        }
        if (bus == null) return null;

        ItemStack result = bus.copy();
        result.stackSize = 1;
        NBTTagCompound tag = result.getTagCompound();
        for (String key : SETTINGS) tag.removeTag(key);
        // Current format: slot 0 is the virtual circuit, slot 1 is the manual item.
        // Legacy format: 0..31 were filter/display slots, 32 the circuit and 33 the manual item.
        if (tag.hasKey("Inventory", 9)) {
            int lastVirtualSlot = tag.getInteger("version") == 0 ? 2 * MixedInputBus.SLOT_COUNT : 0;
            NBTTagList slots = tag.getTagList("Inventory", 10);
            for (int i = slots.tagCount() - 1; i >= 0; i--) {
                NBTTagCompound slot = slots.getCompoundTagAt(i);
                int index = slot.getInteger("IntSlot");
                if (slot.hasKey("IntSlot") && index >= 0 && index <= lastVirtualSlot) slots.removeTag(i);
            }
            if (slots.tagCount() == 0) tag.removeTag("Inventory");
        }
        if (!tag.hasKey("Inventory")) {
            tag.removeTag("migrationCircuitSlot");
            if (tag.getInteger("version") == 1) tag.removeTag("version");
        }
        if (tag.hasNoTags()) result.setTagCompound(null);
        return ItemStack.areItemStackTagsEqual(bus, result) ? null : result;
    }
}
