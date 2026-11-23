package dev.aestoragevault.packtest;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;
import dev.aestoragevault.VaultMod;
import gregtech.api.enums.ItemList;

/** Verify the real loaded recipe, including slot layout and quantities. */
final class VaultCasingChecks {

    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }

    static void recipe(World world) {
        check(
            GameRegistry.findBlock(VaultMod.ID, "resonance_casing") == VaultMod.RESONANCE_CASING,
            "dedicated casing registry");
        check(!VaultMod.RESONANCE_CASING.hasTileEntity(0), "casing must remain a passive block");
        IRecipe recipe = null;
        int count = 0;
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            IRecipe candidate = (IRecipe) entry;
            ItemStack output = candidate.getRecipeOutput();
            if (output != null && Block.getBlockFromItem(output.getItem()) == VaultMod.RESONANCE_CASING) {
                count++;
                recipe = candidate;
                check(output.stackSize == 1 && output.getItemDamage() == 0, "wrong casing yield");
            }
        }
        check(count == 1, "expected exactly one casing recipe, got " + count);
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        ItemStack drive = AEApi.instance()
            .definitions()
            .blocks()
            .drive()
            .maybeStack(1)
            .get();
        for (int slot = 0; slot < 9; slot++) grid.setInventorySlotContents(
            slot,
            slot == 4 ? ItemList.Casing_Coil_Superconductor.get(1)
                : slot % 2 == 0 ? drive.copy() : ItemList.Casing_LuV.get(1));
        check(recipe.matches(grid, world), "four corners/drives, edges/LuV, center/superconductor do not match");
        ItemStack crafted = CraftingManager.getInstance()
            .findMatchingRecipe(grid, world);
        check(
            crafted != null && crafted.stackSize == 1
                && Block.getBlockFromItem(crafted.getItem()) == VaultMod.RESONANCE_CASING,
            "real crafting lookup did not return one casing");
        for (int slot = 0; slot < 9; slot++) {
            ItemStack expected = grid.getStackInSlot(slot);
            grid.setInventorySlotContents(slot, null);
            check(!recipe.matches(grid, world), "missing ingredient accepted in slot " + slot);
            grid.setInventorySlotContents(slot, expected);
        }
        ItemStack corner = grid.getStackInSlot(0), edge = grid.getStackInSlot(1);
        grid.setInventorySlotContents(0, edge);
        grid.setInventorySlotContents(1, corner);
        check(!recipe.matches(grid, world), "corner and edge ingredients may not be swapped");
        grid.setInventorySlotContents(0, corner);
        grid.setInventorySlotContents(1, ItemList.Hull_LuV.get(1));
        check(!recipe.matches(grid, world), "LuV hull substituted for the requested machine casing");
        grid.setInventorySlotContents(1, edge);
        grid.setInventorySlotContents(4, ItemList.Casing_LuV.get(1));
        check(!recipe.matches(grid, world), "recipe accepted an ordinary casing instead of the coil");
    }
}
