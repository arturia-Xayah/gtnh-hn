package dev.aestoragevault.world;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Passive structure material: no inventory, ticking tile entity or AE node. */
public final class VaultCasingBlock extends Block {

    public VaultCasingBlock() {
        super(Material.iron);
        setBlockName("aestoragevault.resonance_casing");
        setHardness(4);
        setResistance(15);
        setStepSound(soundTypeMetal);
        setCreativeTab(CreativeTabs.tabRedstone);
        setHarvestLevel("pickaxe", 2);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister registry) {
        VaultSkin.register(registry);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return VaultSkin.whole();
    }

    @Override
    public int getRenderType() {
        return VaultBlock.renderId;
    }
}
