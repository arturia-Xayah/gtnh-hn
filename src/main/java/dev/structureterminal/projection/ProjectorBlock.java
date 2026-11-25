package dev.structureterminal.projection;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

import appeng.api.AEApi;
import appeng.api.definitions.IBlocks;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dev.structureterminal.StructureTerminal;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;

public final class ProjectorBlock extends BlockContainer {

    private static final String PRESET = "structureterminal:portalPreset";
    private IIcon emitter;

    private ProjectorBlock() {
        super(Material.iron);
        setHardness(4);
        setResistance(8);
        setStepSound(soundTypeMetal);
        setBlockName("structureterminal.projector");
        setBlockTextureName("structureterminal:projector_side");
        setCreativeTab(CreativeTabs.tabRedstone);
    }

    public static void register() {
        ProjectorBlock projector = new ProjectorBlock();
        GameRegistry.registerBlock(projector, "portal_projector");
        MinecraftForge.EVENT_BUS.register(projector);
        GameRegistry.registerTileEntity(ProjectorTile.class, "structureterminal.portal_projector");
    }

    public static void recipe() {
        IBlocks blocks = AEApi.instance()
            .definitions()
            .blocks();
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                GameRegistry.findBlock(StructureTerminal.ID, "portal_projector"),
                "CRC",
                "RQR",
                "CRC",
                'Q',
                blocks.quantumLink()
                    .maybeStack(1)
                    .get(),
                'R',
                blocks.quantumRing()
                    .maybeStack(1)
                    .get(),
                'C',
                OrePrefixes.circuit.get(Materials.IV)
                    .toString()));
    }

    @SubscribeEvent
    public void copyPreset(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK || event.world.isRemote
            || event.world.getBlock(event.x, event.y, event.z) != this) return;
        EntityPlayer player = event.entityPlayer;
        ItemStack stick = player.getHeldItem();
        if (!ItemList.Tool_DataStick.isStackEqual(stick, false, true)) return;
        event.setCanceled(true);
        event.world.markBlockForUpdate(event.x, event.y, event.z);
        if (!canEdit(event.world, event.x, event.y, event.z, player, event.face)) return;
        TileEntity tile = event.world.getTileEntity(event.x, event.y, event.z);
        if (!(tile instanceof ProjectorTile)) return;
        if (!stick.hasTagCompound()) stick.setTagCompound(new NBTTagCompound());
        stick.getTagCompound()
            .setTag(PRESET, ((ProjectorTile) tile).writePreset(new NBTTagCompound()));
        player.inventory.markDirty();
        player.addChatMessage(new ChatComponentTranslation("projector.preset.copied"));
    }

    private static boolean canEdit(World world, int x, int y, int z, EntityPlayer player, int face) {
        return player.capabilities.allowEdit && world.canMineBlock(player, x, y, z)
            && player.canPlayerEdit(x, y, z, face, player.getHeldItem());
    }

    @Override
    public TileEntity createNewTileEntity(World w, int meta) {
        return new ProjectorTile();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister atlas) {
        super.registerBlockIcons(atlas);
        emitter = atlas.registerIcon("structureterminal:projector_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side == 1 ? emitter : blockIcon;
    }

    @Override
    public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase e, ItemStack stack) {
        if (w.isRemote) return;
        int turn = MathHelper.floor_double(e.rotationYaw / 90 + .5) & 3;
        w.setBlockMetadataWithNotify(x, y, z, turn, 3);

    }

    @Override
    public void breakBlock(World w, int x, int y, int z, net.minecraft.block.Block block, int metadata) {
        TileEntity t = w.getTileEntity(x, y, z);
        if (!w.isRemote && t instanceof ProjectorTile) GateDirectory.get()
            .remove((ProjectorTile) t);
        super.breakBlock(w, x, y, z, block, metadata);
    }

    @Override
    public boolean rotateBlock(World w, int x, int y, int z, net.minecraftforge.common.util.ForgeDirection axis) {
        return false;
    }

    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side, float hx, float hy,
        float hz) {
        ItemStack held = p.getHeldItem();
        if (ItemList.Tool_DataStick.isStackEqual(held, false, true)) {
            if (!w.isRemote && canEdit(w, x, y, z, p, side)) {
                TileEntity tile = w.getTileEntity(x, y, z);
                if (tile instanceof ProjectorTile) {
                    NBTTagCompound tag = held.getTagCompound();
                    String result = tag != null && tag.hasKey(PRESET, 10)
                        ? ((ProjectorTile) tile).applyPreset(tag.getCompoundTag(PRESET))
                        : "noPreset";
                    p.addChatMessage(
                        new ChatComponentTranslation(
                            result.isEmpty() ? "projector.preset.applied" : "projector.error." + result));
                }
            }
            return true;
        }
        if (!w.isRemote) p.openGui(StructureTerminal.instance, ProjectorMenu.ID, w, x, y, z);
        return true;
    }
}
