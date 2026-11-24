package dev.aestoragevault;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import appeng.api.AEApi;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import dev.aestoragevault.config.VaultCacheConfig;
import dev.aestoragevault.storage.Repositories;
import dev.aestoragevault.ui.CommonProxy;
import dev.aestoragevault.world.TileAccess;
import dev.aestoragevault.world.TileController;
import dev.aestoragevault.world.TileOutput;
import dev.aestoragevault.world.VaultBlock;
import dev.aestoragevault.world.VaultCasingBlock;
import dev.aestoragevault.world.VaultItemBlock;
import dev.structureterminal.Tags;

@Mod(
    modid = VaultMod.ID,
    name = dev.structureterminal.StructureTerminal.DISPLAY_NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:structureterminal;required-after:gregtech;required-after:appliedenergistics2")
public final class VaultMod {

    public static final String ID = "aestoragevault";
    public static final Logger LOG = LogManager.getLogger(ID);
    public static Block CONTROLLER, ACCESS, OUTPUT, RESONANCE_CASING;
    @Mod.Instance(ID)
    public static VaultMod INSTANCE;
    @SidedProxy(clientSide = "dev.aestoragevault.ui.ClientProxy", serverSide = "dev.aestoragevault.ui.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        VaultCacheConfig.load(new java.io.File(event.getModConfigurationDirectory(), "gtnh-modernization.cfg"));
        CONTROLLER = new VaultBlock(1);
        ACCESS = new VaultBlock(2);
        OUTPUT = new VaultBlock(3);
        RESONANCE_CASING = new VaultCasingBlock();
        GameRegistry.registerBlock(CONTROLLER, VaultItemBlock.class, "controller");
        GameRegistry.registerBlock(ACCESS, VaultItemBlock.class, "access");
        GameRegistry.registerBlock(OUTPUT, VaultItemBlock.class, "output");
        GameRegistry.registerBlock(RESONANCE_CASING, VaultItemBlock.class, "resonance_casing");
        GameRegistry.registerTileEntity(TileController.class, ID + ":controller");
        GameRegistry.registerTileEntity(TileAccess.class, ID + ":access");
        GameRegistry.registerTileEntity(TileOutput.class, ID + ":output");
        dev.aestoragevault.world.VaultConstructable.register();
        FMLCommonHandler.instance()
            .bus()
            .register(Repositories.INSTANCE);
        MinecraftForge.EVENT_BUS.register(Repositories.INSTANCE);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
        proxy.configure(new java.io.File(event.getModConfigurationDirectory(), "gtnh-modernization-client.cfg"));
        proxy.init();
        dev.aestoragevault.ui.ReturnNetwork.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ItemStack drive = AEApi.instance()
            .definitions()
            .blocks()
            .drive()
            .maybeStack(1)
            .orNull();
        if (drive == null) throw new IllegalStateException("AE drive missing for vault recipes");
        GameRegistry.addRecipe(
            new ItemStack(RESONANCE_CASING),
            "DCD",
            "CSC",
            "DCD",
            'D',
            drive,
            'C',
            gregtech.api.enums.ItemList.Casing_LuV.get(1),
            'S',
            gregtech.api.enums.ItemList.Casing_Coil_Superconductor.get(1));
        gregtech.api.enums.GTValues.RA.stdBuilder()
            .itemInputs(
                gregtech.api.enums.ItemList.Hull_LuV.get(1),
                new Object[] { gregtech.api.enums.OrePrefixes.circuit.get(gregtech.api.enums.Materials.LuV), 4 },
                gregtech.api.enums.ItemList.Sensor_LuV.get(2),
                drive)
            .fluidInputs(gregtech.api.enums.Materials.SolderingAlloy.getMolten(576))
            .itemOutputs(new ItemStack(CONTROLLER))
            .duration(600)
            .eut(gregtech.api.enums.TierEU.RECIPE_LuV)
            .addTo(gregtech.api.recipe.RecipeMaps.assemblerRecipes);
        gregtech.api.enums.GTValues.RA.stdBuilder()
            .itemInputs(
                gregtech.api.enums.ItemList.Hull_LuV.get(1),
                new Object[] { gregtech.api.enums.OrePrefixes.circuit.get(gregtech.api.enums.Materials.LuV), 2 },
                gregtech.api.enums.ItemList.Emitter_LuV.get(2),
                drive)
            .fluidInputs(gregtech.api.enums.Materials.SolderingAlloy.getMolten(288))
            .itemOutputs(new ItemStack(ACCESS))
            .duration(400)
            .eut(gregtech.api.enums.TierEU.RECIPE_LuV)
            .addTo(gregtech.api.recipe.RecipeMaps.assemblerRecipes);
        gregtech.api.enums.GTValues.RA.stdBuilder()
            .itemInputs(
                gregtech.api.enums.ItemList.Hull_LuV.get(1),
                new Object[] { gregtech.api.enums.OrePrefixes.circuit.get(gregtech.api.enums.Materials.LuV), 2 },
                gregtech.api.enums.ItemList.Emitter_LuV.get(2),
                AEApi.instance()
                    .definitions()
                    .parts()
                    .exportBus()
                    .maybeStack(1)
                    .get())
            .fluidInputs(gregtech.api.enums.Materials.SolderingAlloy.getMolten(288))
            .itemOutputs(new ItemStack(OUTPUT))
            .duration(400)
            .eut(gregtech.api.enums.TierEU.RECIPE_LuV)
            .addTo(gregtech.api.recipe.RecipeMaps.assemblerRecipes);
        appeng.api.config.Upgrades.FUZZY.registerItem(new ItemStack(OUTPUT), 1);
        appeng.api.config.Upgrades.INVERTER.registerItem(new ItemStack(OUTPUT), 1);
        appeng.api.config.Upgrades.ORE_FILTER.registerItem(new ItemStack(OUTPUT), 1);
    }

    public static boolean isResonanceCasing(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return world.getBlock(x, y, z) == RESONANCE_CASING && world.getBlockMetadata(x, y, z) == 0;
    }

    /** The retired casing is intentionally removed, not silently mapped to a wrong GT metadata variant. */
    @Mod.EventHandler
    public void missingMappings(FMLMissingMappingsEvent event) {
        for (FMLMissingMappingsEvent.MissingMapping mapping : event.get()) {
            if ((ID + ":casing").equals(mapping.name)) {
                mapping.ignore();
                LOG.warn(
                    "Removed retired vault casing mapping: {} ({}). Old instances are not retained.",
                    mapping.name,
                    mapping.type);
            }
        }
    }

    @Mod.EventHandler
    public void stop(FMLServerStoppedEvent event) {
        Repositories.INSTANCE.closeAll();
        VaultCacheConfig.clear();
        dev.aestoragevault.ui.ReturnNetwork.clear();
    }

    @NetworkCheckHandler
    public boolean checkVersion(Map<String, String> remote, Side side) {
        return Tags.VERSION.equals(remote.get(ID));
    }
}
