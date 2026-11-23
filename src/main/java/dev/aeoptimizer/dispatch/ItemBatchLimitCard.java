package dev.aeoptimizer.dispatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import appeng.api.AEApi;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IItemGroup;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.util.Platform;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ItemBatchLimitCard extends Item implements IUpgradeModule {

    public static Upgrades TYPE;
    public static ItemBatchLimitCard INSTANCE;

    public static void register() {
        // Added during enum initialization, before enum maps/caches, on Java 8 and lwjgl3ify Java 17+.
        TYPE = Upgrades.valueOf("MODERNIZATION_BATCH_LIMIT");
        INSTANCE = new ItemBatchLimitCard();
        GameRegistry.registerItem(INSTANCE, "batch_dispatch_limit_card");
    }

    public static void registerHosts() {
        // Match the native Advanced Blocking Card's supported block/part interfaces, including AE2FC.
        for (ItemStack host : Upgrades.ADVANCED_BLOCKING.getSupported()
            .keySet()) TYPE.registerItem(host, 1);
        INSTANCE.setCreativeTab(appeng.core.CreativeTab.instance);
        GameRegistry.addShapelessRecipe(
            new ItemStack(INSTANCE),
            AEApi.instance()
                .definitions()
                .materials()
                .advCard()
                .maybeStack(1)
                .get(),
            net.minecraft.init.Items.comparator);
    }

    private ItemBatchLimitCard() {
        setUnlocalizedName("aeorderoptimizer.batch_limit_card");
        setTextureName("appliedenergistics2:ItemMaterial.CardCapacity");
        setCreativeTab(CreativeTabs.tabMisc);
    }

    public Upgrades getType(ItemStack stack) {
        return TYPE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return 0x99CCFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        for (String key : new String[] { "install", "require", "limit", "why", "strict", "configure" })
            lines.add(StatCollector.translateToLocal("aeoptimizer.batch.card." + key));
        if (!net.minecraft.client.gui.GuiScreen.isShiftKeyDown()) {
            lines.add(StatCollector.translateToLocal("aeoptimizer.batch.card.more"));
            return;
        }
        lines.add("");
        for (String key : new String[] { "same", "smart", "not", "small", "queue" })
            lines.add(StatCollector.translateToLocal("aeoptimizer.batch.detail." + key));
        lines.add("");
        lines.add(StatCollector.translateToLocal("aeoptimizer.batch.card.devices"));
        // Same localized grouping/deduplication as AE's own upgrade-card tooltips.
        List<String> devices = new ArrayList<>();
        for (Map.Entry<ItemStack, Integer> entry : TYPE.getSupported()
            .entrySet()) {
            ItemStack host = entry.getKey();
            String name = null;
            if (host.getItem() instanceof IItemGroup) {
                String group = ((IItemGroup) host.getItem()).getUnlocalizedGroupName(
                    TYPE.getSupported()
                        .keySet(),
                    host);
                if (group != null) name = Platform.gui_localize(group);
            }
            if (name == null) name = host.getDisplayName();
            if (!devices.contains(name)) devices.add(name);
        }
        java.util.Collections.sort(devices);
        for (String name : devices) lines.add("§7  " + name);
    }
}
