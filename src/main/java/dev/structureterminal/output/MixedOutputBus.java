package dev.structureterminal.output;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.glodblock.github.common.item.FCBaseItemCell;
import com.glodblock.github.common.item.ItemFluidVoidStorageCell;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.items.storage.ItemBasicStorageCell;
import appeng.items.storage.ItemVoidStorageCell;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.item.AEFluidStack;
import gregtech.api.enums.OutputHatchType;
import gregtech.api.enums.Textures.BlockIcons;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity.SkipGenerateDescription;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;
import gregtech.common.tileentities.machines.outputme.base.MTEHatchOutputMEBase;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

/** One physical output bus and AE node, with independent native item/fluid cache providers. */
@SkipGenerateDescription
public final class MixedOutputBus extends MTEHatchOutputBusME {

    public static final String MACHINE_NAME = "modernization.mixed_output";
    public static final int ITEM_SLOT = 0;
    public static final int FLUID_SLOT = 1;

    public final MTEHatchOutputMEBase<IAEFluidStack> fluids = new MTEHatchOutputMEBase<IAEFluidStack>(
        new FluidEnvironment()) {

        @Override
        public AENetworkProxy getProxy() {
            return MixedOutputBus.this.getProxy();
        }
    };

    public final IOutputHatch fluidEndpoint = new IOutputHatch() {

        public boolean isFiltered() {
            return fluids.isFiltered();
        }

        public boolean isFilteredToFluid(GTUtility.FluidId id) {
            return fluids.canStore(AEFluidStack.create(id.getFluidStack()));
        }

        public OutputHatchType getHatchType() {
            return fluids.getCacheMode()
                ? (isFiltered() ? OutputHatchType.MECacheFiltered : OutputHatchType.MECacheUnfiltered)
                : (isFiltered() ? OutputHatchType.MEFiltered : OutputHatchType.MEUnfiltered);
        }

        public boolean storePartial(FluidStack stack, boolean simulate) {
            if (stack == null || stack.amount <= 0) return true;
            IAEFluidStack input = AEFluidStack.create(stack);
            boolean accepted = fluids.storePartial(input, simulate);
            stack.amount = (int) input.getStackSize();
            return accepted;
        }

        public IOutputHatchTransaction createTransaction() {
            return new FluidOutputTransaction(MixedOutputBus.this);
        }
    };

    public MixedOutputBus(int id) {
        super(id, MACHINE_NAME, "ME Dual Output Bus");
    }

    private MixedOutputBus(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, tier, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MixedOutputBus(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(BlockIcons.OVERLAY_ME_HATCH_ACTIVE),
            TextureFactory.of(BlockIcons.OVERLAY_PIPE_OUT) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(BlockIcons.OVERLAY_ME_HATCH),
            TextureFactory.of(BlockIcons.OVERLAY_PIPE_OUT) };
    }

    @Override
    public String[] getDescription() {
        return new String[] { tr("description"), tr("placement"), tr("cell"), tr("overflow") };
    }

    public static String tr(String key) {
        return StatCollector.translateToLocal("mixed.output." + key);
    }

    private static boolean fluidCell(ItemStack stack) {
        return stack != null
            && (stack.getItem() instanceof FCBaseItemCell || stack.getItem() instanceof ItemFluidVoidStorageCell);
    }

    public ItemStack fluidCell() {
        return mInventory[FLUID_SLOT];
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == FLUID_SLOT) return fluidCell(stack);
        return index == ITEM_SLOT && stack != null
            && (stack.getItem() instanceof ItemBasicStorageCell || stack.getItem() instanceof ItemVoidStorageCell);
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return side == tile.getFrontFacing() && isItemValidForSlot(index, stack);
    }

    @Override
    public void onContentsChanged(int slot) {
        if (slot == ITEM_SLOT) super.onContentsChanged(0);
        // Each native provider numbers its own cell slot as zero.
        if (slot == FLUID_SLOT) fluids.onContentsChanged(0);
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        fluids.onPostTick(tile, tick);
    }

    @Override
    public List<IMEInventoryHandler> getCellArray(StorageChannel channel) {
        return channel == StorageChannel.FLUIDS ? fluids.getCellArray(channel) : super.getCellArray(channel);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        NBTTagCompound fluid = new NBTTagCompound();
        fluids.saveNBTData(fluid);
        fluid.removeTag("proxy"); // The single network node belongs to the item provider.
        tag.setTag("dualFluidOutput", fluid);
    }

    @Override
    public void setItemNBT(NBTTagCompound tag) {
        super.setItemNBT(tag);
        NBTTagCompound fluid = new NBTTagCompound();
        fluids.setItemNBT(fluid);
        tag.setTag("dualFluidOutput", fluid);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        // GT restores its inventory before loading either provider. Preserve the original cell including its NBT.
        if (mInventory[FLUID_SLOT] == null && fluidCell(mInventory[ITEM_SLOT])) {
            mInventory[FLUID_SLOT] = mInventory[ITEM_SLOT];
            mInventory[ITEM_SLOT] = null;
        }
        super.loadNBTData(tag);
        fluids.loadNBTData(tag.getCompoundTag("dualFluidOutput"));
    }

    @Override
    public NBTTagCompound getCopiedData(EntityPlayer player) {
        NBTTagCompound tag = super.getCopiedData(player);
        tag.setTag("dualFluidOutput", fluids.getCopiedData(player));
        return tag;
    }

    @Override
    public boolean pasteCopiedData(EntityPlayer player, NBTTagCompound tag) {
        if (!super.pasteCopiedData(player, tag)) return false;
        if (tag.hasKey("dualFluidOutput", 10)) fluids.pasteCopiedData(player, tag.getCompoundTag("dualFluidOutput"));
        return true;
    }

    @Override
    public ItemStack getVisual() {
        return getStackForm(1);
    }

    @Override
    public void getWailaNBTData(net.minecraft.entity.player.EntityPlayerMP player,
        net.minecraft.tileentity.TileEntity tile, NBTTagCompound tag, net.minecraft.world.World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        NBTTagCompound fluid = new NBTTagCompound();
        fluids.getWailaNBTData(player, tile, fluid, world, x, y, z);
        fluid.setLong("buffered", fluids.getCachedAmount());
        tag.setTag("hnFluidView", fluid);
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public void getWailaBody(ItemStack item, List<String> lines, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(item, lines, accessor, config);
        NBTTagCompound fluid = accessor.getNBTData()
            .getCompoundTag("hnFluidView");
        lines.add(
            tr("fluids") + "  "
                + NumberFormatUtil.formatNumber(fluid.getLong("buffered"))
                + " / "
                + NumberFormatUtil.formatNumber(fluid.getLong("cacheCapacity"))
                + " mB");
        hn$fluidLines(lines, fluid, "stacks");
        if (fluid.getInteger("cacheCount") > 0) {
            lines.add(tr("cellContents"));
            hn$fluidLines(lines, fluid, "cacheStacks");
        }
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private static void hn$fluidLines(List<String> lines, NBTTagCompound data, String key) {
        NBTTagList list = data.getTagList(key, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound row = list.getCompoundTagAt(i);
            IAEFluidStack stack = AEFluidStack.loadFluidStackFromNBT(row);
            if (stack != null)
                lines.add(stack.getDisplayName() + "  " + NumberFormatUtil.formatNumber(row.getLong("Amount")) + " mB");
        }
    }

    @Override
    public String[] getInfoData() {
        List<String> lines = new ArrayList<>(java.util.Arrays.asList(super.getInfoData()));
        lines.add(tr("fluids") + ": " + fluids.getCachedAmount() + " / " + fluids.getCacheCapacity() + " mB");
        return lines.toArray(new String[0]);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new MixedOutputGui(this).build(data, sync, settings);
    }

    private final class FluidEnvironment implements MTEHatchOutputMEBase.Environment<IAEFluidStack> {

        public IGregTechTileEntity getBaseMetaTileEntity() {
            return MixedOutputBus.this.getBaseMetaTileEntity();
        }

        public IGridProxyable getIGridProxyable() {
            return MixedOutputBus.this;
        }

        public StorageChannel getChannel() {
            return StorageChannel.FLUIDS;
        }

        public ItemStack getCellStack() {
            return fluidCell();
        }

        public ISaveProvider getISaveProvider() {
            return MixedOutputBus.this;
        }

        public BaseActionSource getActionSource() {
            return MixedOutputBus.this.getActionSource();
        }

        public EntityPlayer getLastClickedPlayer() {
            return MixedOutputBus.this.getLastClickedPlayer();
        }

        public IMEInventory<IAEFluidStack> getNetworkInvtory() throws GridAccessException {
            return getProxy().getStorage()
                .getFluidInventory();
        }

        public NBTTagCompound saveStackToNBT(IAEFluidStack stack) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag(
                "fluid",
                stack.getFluidStack()
                    .writeToNBT(new NBTTagCompound()));
            tag.setLong("size", stack.getStackSize());
            return tag;
        }

        public IAEFluidStack loadStackFromNBT(NBTTagCompound tag) {
            IAEFluidStack stack = AEFluidStack.create(FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("fluid")));
            return stack == null ? null : stack.setStackSize(tag.getLong("size"));
        }

        public byte getColor() {
            return MixedOutputBus.this.getColor();
        }

        public String getCopiedDataIdentifier(EntityPlayer player) {
            return "modernization.mixed_output.fluids";
        }

        public ItemStack getVisual() {
            return MixedOutputBus.this.getVisual();
        }

        public void dispatchMarkDirty() {
            MixedOutputBus.this.markDirty();
        }

        public void notifyOutputSpaceChanged() {
            MixedOutputBus.this.notifyOutputSpaceChanged();
        }

        public MTEHatchOutputMEBase<IAEFluidStack> getProvider() {
            return fluids;
        }

        public String getEnableKey() {
            return "GT5U.hatch.fluid.filter.enable";
        }

        public String getDisableKey() {
            return "GT5U.hatch.fluid.filter.disable";
        }

        public long getDefaultCapacity() {
            return MTEHatchOutputME.DEFAULT_CAPACITY;
        }
    }
}
