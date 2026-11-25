package dev.aeoptimizer;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import dev.aeoptimizer.network.BlockOptimizer;
import dev.aeoptimizer.network.TileOptimizer;
import dev.aeoptimizer.ui.CommonProxy;
import dev.structureterminal.Tags;

@Mod(
    modid = OrderOptimizer.ID,
    name = dev.structureterminal.StructureTerminal.DISPLAY_NAME,
    version = Tags.VERSION,
    dependencies = "required-after:structureterminal;required-after:appliedenergistics2;required-after:unimixins",
    acceptedMinecraftVersions = "[1.7.10]")
public final class OrderOptimizer {

    public static final String ID = "aeorderoptimizer";
    public static final Logger LOG = LogManager.getLogger(ID);
    @Mod.Instance(ID)
    public static OrderOptimizer INSTANCE;
    @SidedProxy(clientSide = "dev.aeoptimizer.ui.ClientProxy", serverSide = "dev.aeoptimizer.ui.CommonProxy")
    public static CommonProxy proxy;
    public static BlockOptimizer BLOCK;

    @Mod.EventHandler
    public void preInit(cpw.mods.fml.common.event.FMLPreInitializationEvent event) {
        dev.aeoptimizer.dispatch.ItemBatchLimitCard.register();
        dev.aeoptimizer.storage.ItemDualStorageBus.register();
        dev.aeoptimizer.storage.KindLimitCard.register();
    }

    @Mod.EventHandler
    public void postInit(cpw.mods.fml.common.event.FMLPostInitializationEvent event) {
        dev.aeoptimizer.dispatch.ItemBatchLimitCard.registerHosts();
        dev.aeoptimizer.storage.ItemDualStorageBus.setup();
        dev.aeoptimizer.storage.KindLimitCard.setup();
    }

    @Mod.EventHandler
    public void serverStarting(cpw.mods.fml.common.event.FMLServerStartingEvent event) {
        event.registerServerCommand(new dev.aeoptimizer.planning.PatternLocations());
    }

    @Mod.EventHandler
    public void serverStopped(cpw.mods.fml.common.event.FMLServerStoppedEvent event) {
        dev.aeoptimizer.planning.PatternLocations.clear();
        dev.aeoptimizer.emitter.EmitterUpdates.INSTANCE.clear();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(dev.aeoptimizer.emitter.EmitterUpdates.INSTANCE);
        BLOCK = new BlockOptimizer();
        GameRegistry.registerBlock(BLOCK, "core");
        GameRegistry.registerTileEntity(TileOptimizer.class, ID + ":core");
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
        GameRegistry.addRecipe(
            new ItemStack(BLOCK),
            "GQG",
            "RCR",
            "GQG",
            'G',
            Items.gold_ingot,
            'Q',
            Items.quartz,
            'R',
            Items.redstone,
            'C',
            Blocks.crafting_table);
    }
}
