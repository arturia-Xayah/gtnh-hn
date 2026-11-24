package dev.aeoptimizer.storage;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.glodblock.github.loader.ItemAndBlockHolder;

import appeng.api.AEApi;
import appeng.api.config.Upgrades;
import appeng.api.parts.IPartItem;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ItemDualStorageBus extends Item implements IPartItem {

    public static final ItemDualStorageBus INSTANCE = new ItemDualStorageBus();

    public static void register() {
        INSTANCE.setUnlocalizedName("aeorderoptimizer.dual_storage_bus");
        INSTANCE.setTextureName("ae2fc:fluid_storage_bus");
        GameRegistry.registerItem(INSTANCE, "dual_storage_bus");
    }

    public static void setup() {
        INSTANCE.setCreativeTab(appeng.core.CreativeTab.instance);
        AEApi.instance()
            .partHelper()
            .setItemBusRenderer(INSTANCE);
        ItemStack bus = new ItemStack(INSTANCE);
        Upgrades.CAPACITY.registerItem(bus, 5);
        Upgrades.INVERTER.registerItem(bus, 1);
        Upgrades.FUZZY.registerItem(bus, 1);
        Upgrades.STICKY.registerItem(bus, 1);
        GameRegistry.addShapelessRecipe(
            bus,
            AEApi.instance()
                .definitions()
                .parts()
                .storageBus()
                .maybeStack(1)
                .get(),
            new ItemStack(ItemAndBlockHolder.FLUID_STORAGE_BUS));
    }

    @Override
    public DualStorageBus createPartFromItemStack(ItemStack stack) {
        return new DualStorageBus(stack.copy());
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        return AEApi.instance()
            .partHelper()
            .placeBus(stack, x, y, z, side, player, world);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add(StatCollector.translateToLocal("item.aeorderoptimizer.dual_storage_bus.tip"));
    }
}
