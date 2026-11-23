package dev.aeoptimizer.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.config.SecurityPermissions;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerInterface;
import appeng.container.sync.ActionHandler;
import appeng.container.sync.StreamCodec;
import appeng.container.sync.handlers.BooleanSyncHandler;
import appeng.container.sync.handlers.LongSyncHandler;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.util.Platform;
import dev.aeoptimizer.dispatch.BatchLimitSettings;
import io.netty.buffer.ByteBuf;

@Mixin(value = ContainerInterface.class, remap = false)
public abstract class BatchLimitContainerMixin extends AEBaseContainer implements BatchLimitSettings {

    @Shadow
    @Final
    private DualityInterface myDuality;
    @Unique
    private LongSyncHandler modernization$items, modernization$fluids;
    @Unique
    private BooleanSyncHandler modernization$card, modernization$blocked;
    @Unique
    private ActionHandler<long[]> modernization$applyLimits;

    private BatchLimitContainerMixin(InventoryPlayer ip, Object anchor) {
        super(ip, anchor);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void modernization$init(InventoryPlayer ip, IInterfaceHost host, CallbackInfo ci) {
        modernization$items = syncRegistrar().longS2C("modernizationBatchItems");
        modernization$fluids = syncRegistrar().longS2C("modernizationBatchFluids");
        modernization$card = syncRegistrar().booleanS2C("modernizationBatchCard");
        modernization$blocked = syncRegistrar().booleanS2C("modernizationBatchBlocked");
        modernization$applyLimits = syncRegistrar().actionC2S("modernizationBatchApply", new StreamCodec<long[]>() {

            public void write(ByteBuf buf, long[] values) {
                buf.writeLong(values[0]);
                buf.writeLong(values[1]);
            }

            public long[] read(ByteBuf buf) {
                return new long[] { buf.readLong(), buf.readLong() };
            }

            public String getTypeKey() {
                return "modernization:batch-limits-v1";
            }
        })
            .onServerAction(values -> modernization$apply(values[0], values[1]));
    }

    @Unique
    private void modernization$apply(long items, long fluids) {
        EntityPlayer player = getInventoryPlayer().player;
        TileEntity tile = myDuality.getHost()
            .getTile();
        if (items < 0 || fluids < 0
            || !((BatchLimitSettings) myDuality).hasBatchLimitCard()
            || player.openContainer != (Object) this
            || !canInteractWith(player)
            || !hasAccess(SecurityPermissions.BUILD, false)
            || tile == null
            || tile.isInvalid()
            || tile.getWorldObj() != player.worldObj
            || player.getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5) > 64) return;
        ((BatchLimitSettings) myDuality).setBatchLimits(items, fluids);
    }

    @Inject(method = { "detectAndSendChanges", "func_75142_b" }, at = @At("HEAD"))
    private void modernization$sync(CallbackInfo ci) {
        if (!Platform.isServer()) return;
        BatchLimitSettings settings = (BatchLimitSettings) myDuality;
        modernization$items.set(settings.batchItemLimit());
        modernization$fluids.set(settings.batchFluidLimit());
        modernization$card.set(settings.hasBatchLimitCard());
        modernization$blocked.set(settings.hasBatchLimitCard() && settings.batchLimitBlocked());
    }

    public long batchItemLimit() {
        return modernization$items.get();
    }

    public long batchFluidLimit() {
        return modernization$fluids.get();
    }

    public boolean hasBatchLimitCard() {
        return modernization$card.get();
    }

    public boolean batchLimitBlocked() {
        return modernization$blocked.get();
    }

    public void setBatchLimitBlocked(boolean blocked) { /* Server-owned diagnostic state. */ }

    public void setBatchLimits(long items, long fluids) {
        if (Platform.isServer()) modernization$apply(items, fluids);
        else modernization$applyLimits.send(new long[] { items, fluids });
    }
}
