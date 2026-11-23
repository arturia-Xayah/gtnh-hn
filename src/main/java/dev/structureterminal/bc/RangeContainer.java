package dev.structureterminal.bc;

import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import dev.structureterminal.net.TerminalNetwork;

public final class RangeContainer extends Container {

    public static final int GUI = 37;
    public final TileEntity tile;
    private int[] preview;
    private int previewFace, w, h, d;

    private RangeContainer(TileEntity tile) {
        this.tile = tile;
    }

    public static RangeContainer at(EntityPlayer p, World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) return null;
        TileEntity t = world.getTileEntity(x, y, z);
        if (!(t instanceof RangeMachine)) return null;
        RangeContainer c = new RangeContainer(t);
        return c.canInteractWith(p) ? c : null;
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        World world = p.worldObj;
        return world == tile.getWorldObj() && !tile.isInvalid()
            && world.blockExists(tile.xCoord, tile.yCoord, tile.zCoord)
            && world.getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile
            && p.getDistanceSq(tile.xCoord + .5, tile.yCoord + .5, tile.zCoord + .5) <= 64;
    }

    public int front() {
        return PortableRange.front(tile);
    }

    public void handle(EntityPlayerMP p, NBTTagCompound cmd) {
        NBTTagCompound reply = new NBTTagCompound();
        reply.setBoolean("bcRange", true);
        reply.setInteger("window", windowId);
        try {
            if (!canInteractWith(p) || !p.capabilities.allowEdit
                || !p.worldObj.canMineBlock(p, tile.xCoord, tile.yCoord, tile.zCoord)
                || !p.canPlayerEdit(tile.xCoord, tile.yCoord, tile.zCoord, front(), p.getHeldItem()))
                throw new IllegalArgumentException("bc.range.denied");
            if (cmd.getBoolean("load")) {
                reply.setIntArray("size", PortableRange.draft(tile));
            } else if (cmd.getBoolean("reset")) {
                int[] size = PortableRange.actualDimensions(tile);
                PortableRange.saveDraft(tile, size);
                reply.setIntArray("size", size);
                reply.setBoolean("reset", true);
                preview = null;
            } else {
                int[] box = preview(cmd.getInteger("w"), cmd.getInteger("h"), cmd.getInteger("d"));
                PortableRange.saveDraft(tile, new int[] { w, h, d });
                if (cmd.getBoolean("apply")) {
                    boolean queued = applyPreview();
                    p.addChatMessage(new ChatComponentTranslation(queued ? "bc.range.queued" : "bc.range.applied"));
                    reply.setBoolean("reset", true);
                } else if (cmd.getBoolean("close")) {
                    reply.setBoolean("close", true);
                } else if (!cmd.getBoolean("draft")) {
                    reply.setIntArray("bounds", box);
                } else return;
            }
        } catch (IllegalArgumentException invalid) {
            preview = null;
            reply.setString("error", invalid.getMessage());
        }
        TerminalNetwork.channel.sendTo(new TerminalNetwork.Reply(reply), p);
    }

    public int[] preview(int width, int height, int depth) {
        preview = null;
        w = width;
        h = height;
        d = depth;
        previewFace = front();
        preview = calculate();
        return preview.clone();
    }

    public boolean applyPreview() {
        if (preview == null || front() != previewFace || !Arrays.equals(preview, calculate())) {
            preview = null;
            throw new IllegalArgumentException("bc.range.rotated");
        }
        boolean queued = PortableRange.apply(tile, new int[] { w, h, d });
        preview = null;
        return queued;
    }

    private int[] calculate() {
        return PortableRange.bounds(
            tile.xCoord,
            tile.yCoord,
            tile.zCoord,
            front(),
            new int[] { w, h, d },
            tile.getWorldObj()
                .getHeight());
    }
}
