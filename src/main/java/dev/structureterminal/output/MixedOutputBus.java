package dev.structureterminal.output;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.glodblock.github.common.item.FCBaseItemCell;
import com.glodblock.github.common.item.ItemFluidVoidStorageCell;

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
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;
import gregtech.common.tileentities.machines.outputme.base.MTEHatchOutputMEBase;

/** One physical output bus and AE node, with independent native item/fluid cache providers. */
public final class MixedOutputBus extends MTEHatchOutputBusME {

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
        super(id, "modernization.mixed_output", "ME Dual Output Bus");
    }

    private MixedOutputBus(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, tier, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MixedOutputBus(mName, mTier, mDescriptionArray, mTextures);
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
        return fluidCell(mInventory[0]) ? mInventory[0] : null;
    }

    @Override
    public ItemStack getCellStack() {
        return fluidCell(mInventory[0]) ? null : mInventory[0];
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == 0 && stack != null
            && (fluidCell(stack) || stack.getItem() instanceof ItemBasicStorageCell
                || stack.getItem() instanceof ItemVoidStorageCell);
    }

    @Override
    public void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        if (fluids != null) fluids.onContentsChanged(slot);
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
