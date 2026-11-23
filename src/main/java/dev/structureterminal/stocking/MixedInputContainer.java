package dev.structureterminal.stocking;

import net.minecraft.entity.player.EntityPlayer;

import appeng.api.config.SecurityPermissions;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.container.AEBaseContainer;
import appeng.container.interfaces.IVirtualSlotHolder;
import appeng.container.slot.SlotNormal;
import appeng.container.sync.handlers.BooleanSyncHandler;
import appeng.container.sync.handlers.IntSyncHandler;
import appeng.container.sync.handlers.LongSyncHandler;
import appeng.util.Platform;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public final class MixedInputContainer extends AEBaseContainer implements IVirtualSlotHolder {

    public final MixedInputBus bus;
    private final IAEStack<?>[] clientFilters = new IAEStack<?>[16];
    public final BooleanSyncHandler auto;
    public final IntSyncHandler minimum, interval;
    public final LongSyncHandler[] amounts = new LongSyncHandler[16];
    private int ticks;

    public MixedInputContainer(EntityPlayer player, MixedInputBus bus) {
        super(player.inventory, bus.getBaseMetaTileEntity());
        this.bus = bus;
        auto = syncRegistrar().booleanSync("auto")
            .onServerChange((old, val) -> { if (canInteractWith(player)) bus.setAutoPullItemList(val); });
        minimum = syncRegistrar().intSync("minimum")
            .onServerChange((old, val) -> {
                if (canInteractWith(player)) {
                    bus.setMinAutoPullStackSize(Math.max(1, val));
                    bus.markDirty();
                }
            });
        interval = syncRegistrar().intSync("interval")
            .onServerChange((old, val) -> {
                if (canInteractWith(player)) {
                    bus.setAutoPullRefreshTime(Math.max(1, val));
                    bus.markDirty();
                }
            });
        for (int i = 0; i < 16; i++) amounts[i] = syncRegistrar().longS2C("amount" + i);
        addSlotToContainer(
            new appeng.container.slot.SlotFake(bus.getBaseMetaTileEntity(), bus.getCircuitSlot(), 86, 41) {

                @Override
                public boolean isItemValid(net.minecraft.item.ItemStack stack) {
                    return gregtech.api.util.GTUtility.isAnyIntegratedCircuit(stack);
                }
            });
        addSlotToContainer(new SlotNormal(bus.getBaseMetaTileEntity(), bus.getManualSlot(), 86, 77));
        bindPlayerInventory(player.inventory, 0, 158);
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        IGregTechTileEntity t = bus.getBaseMetaTileEntity();
        return t != null && bus.isValid()
            && p.worldObj == t.getWorld()
            && p.getDistanceSq(t.getXCoord() + .5, t.getYCoord() + .5, t.getZCoord() + .5) <= 64
            && (p.worldObj.isRemote || bus.getProxy()
                .getNode() == null || hasAccess(SecurityPermissions.BUILD, false));
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            if (ticks++ % 10 == 0) bus.refreshDisplay();
            auto.set(bus.isAutoPullItemList());
            minimum.set(bus.getMinAutoPullStackSize());
            interval.set(bus.getAutoPullRefreshTime());
            updateVirtualSlots(StorageName.CONFIG, bus.filters, clientFilters);
            for (int i = 0; i < 16; i++) amounts[i].set(bus.displayedAmount(i));
        }
        super.detectAndSendChanges();
    }

    @Override
    public void receiveSlotStacks(StorageName name, Int2ObjectMap<IAEStack<?>> slots) {
        if (name != StorageName.CONFIG || slots == null || slots.size() > 16) return;
        if (Platform.isServer() && (!canInteractWith(getInventoryPlayer().player) || bus.isAutoPullItemList())) return;
        for (Int2ObjectMap.Entry<IAEStack<?>> entry : slots.int2ObjectEntrySet())
            bus.setFilter(entry.getIntKey(), entry.getValue());
    }
}
