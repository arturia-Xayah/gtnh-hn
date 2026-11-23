package dev.aeoptimizer.ui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.security.ISecurityGrid;
import dev.aeoptimizer.network.TileOptimizer;

public class OptimizerContainer extends Container {

    public final TileOptimizer tile;
    public boolean detailed, conflict, autoRename, autoEncodeRename, outputOverflow;

    public OptimizerContainer(TileOptimizer tile) {
        this.tile = tile;
        this.detailed = tile.detailed;
        this.autoRename = tile.autoRename;
        this.autoEncodeRename = tile.autoEncodeRename;
        this.outputOverflow = tile.outputOverflow;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (player.worldObj != tile.getWorldObj() || tile.isInvalid()
            || tile.getWorldObj()
                .getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile
            || player.getDistanceSq(tile.xCoord + .5, tile.yCoord + .5, tile.zCoord + .5) > 64) return false;
        if (tile.getWorldObj().isRemote) return true;
        if (tile.getProxy()
            .getNode() == null) return false;
        ISecurityGrid security = tile.getProxy()
            .getNode()
            .getGrid()
            .getCache(ISecurityGrid.class);
        return security.hasPermission(player, SecurityPermissions.BUILD);
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int action) {
        if ((action < 0 || action > 3) || !canInteractWith(player) || tile.getWorldObj().isRemote) return false;
        if (action == 0) tile.detailed = !tile.detailed;
        else if (action == 1) tile.autoRename = !tile.autoRename;
        else if (action == 2) {
            if (!tile.autoRename) return false;
            tile.autoEncodeRename = !tile.autoEncodeRename;
        } else tile.outputOverflow = !tile.outputOverflow;
        tile.markDirty();
        detectAndSendChanges();
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        detailed = tile.detailed;
        conflict = tile.conflict;
        autoRename = tile.autoRename;
        autoEncodeRename = tile.autoEncodeRename;
        outputOverflow = tile.outputOverflow;
        for (Object viewer : crafters) {
            ((ICrafting) viewer).sendProgressBarUpdate(this, 0, detailed ? 1 : 0);
            ((ICrafting) viewer).sendProgressBarUpdate(this, 1, conflict ? 1 : 0);
            ((ICrafting) viewer).sendProgressBarUpdate(this, 2, autoRename ? 1 : 0);
            ((ICrafting) viewer).sendProgressBarUpdate(this, 3, autoEncodeRename ? 1 : 0);
            ((ICrafting) viewer).sendProgressBarUpdate(this, 4, outputOverflow ? 1 : 0);
        }
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id == 0) detailed = value != 0;
        if (id == 1) conflict = value != 0;
        if (id == 2) autoRename = value != 0;
        if (id == 3) autoEncodeRename = value != 0;
        if (id == 4) outputOverflow = value != 0;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
        return null;
    }
}
