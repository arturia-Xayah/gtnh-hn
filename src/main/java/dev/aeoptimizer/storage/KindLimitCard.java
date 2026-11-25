package dev.aeoptimizer.storage;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.glodblock.github.loader.ItemAndBlockHolder;

import appeng.api.AEApi;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class KindLimitCard extends Item implements IUpgradeModule {

    public static final KindLimitCard INSTANCE = new KindLimitCard();
    public static Upgrades TYPE;

    public static void register() {
        TYPE = Upgrades.valueOf("HN_KIND_LIMIT");
        INSTANCE.setUnlocalizedName("aeorderoptimizer.kind_limit_card");
        INSTANCE.setTextureName("appliedenergistics2:ItemMaterial.CardInverter");
        GameRegistry.registerItem(INSTANCE, "kind_limit_card");
    }

    public static void setup() {
        TYPE.registerItem(
            AEApi.instance()
                .definitions()
                .parts()
                .storageBus(),
            1);
        TYPE.registerItem(new ItemStack(ItemAndBlockHolder.FLUID_STORAGE_BUS), 1);
        TYPE.registerItem(new ItemStack(ItemDualStorageBus.INSTANCE), 1);
        INSTANCE.setCreativeTab(appeng.core.CreativeTab.instance);
        GameRegistry.addShapelessRecipe(
            new ItemStack(INSTANCE),
            AEApi.instance()
                .definitions()
                .materials()
                .advCard()
                .maybeStack(1)
                .get(),
            net.minecraft.init.Blocks.chest);
    }

    @Override
    public Upgrades getType(ItemStack stack) {
        return TYPE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return 0xa8ffd8;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        for (int i = 1; i <= 6; i++) lines.add(StatCollector.translateToLocal("aeoptimizer.kind_limit.tip." + i));
    }
}
