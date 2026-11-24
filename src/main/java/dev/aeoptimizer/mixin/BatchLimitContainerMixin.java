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
import appeng.parts.p2p.PartP2PInterface;
import appeng.util.Platform;
import dev.aeoptimizer.dispatch.BatchLimitSettings;
import dev.aeoptimizer.dispatch.TunnelPolicy;
import io.netty.buffer.ByteBuf;

@Mixin(value = ContainerInterface.class, remap = false)
public abstract class BatchLimitContainerMixin extends AEBaseContainer
    implements BatchLimitSettings, TunnelPolicy.Menu {

    @Shadow
    @Final
    private DualityInterface myDuality;
    @Unique
    private LongSyncHandler modernization$items, modernization$fluids;
    @Unique
    private BooleanSyncHandler modernization$card, modernization$blocked;
    @Unique
    private ActionHandler<long[]> modernization$applyLimits;

    @Unique
    private LongSyncHandler hn$routeMode;
    @Unique
    private ActionHandler<Void> hn$routeAction;

    @Unique
    private PartP2PInterface hn$input() {
        Object host = myDuality.getHost();
        if (host instanceof PartP2PInterface && !((PartP2PInterface) host).isOutput()) return (PartP2PInterface) host;
        return null;
    }

    public int hn$mode() {
        return (int) hn$routeMode.get() - 1;
    }

    public void hn$cycle() {
        if (!Platform.isServer()) {
            hn$routeAction.send(null);
            return;
        }
        PartP2PInterface part = hn$input();
        if (part == null || !modernization$allowed()) return;
        TunnelPolicy policy = ((TunnelPolicy.Host) part).hn$policy();
        policy.setMode((policy.mode + 1) % 3);
        part.getTileEntity()
            .markDirty();
    }

    private BatchLimitContainerMixin(InventoryPlayer ip, Object anchor) {
        super(ip, anchor);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void modernization$init(InventoryPlayer ip, IInterfaceHost host, CallbackInfo ci) {
        hn$routeMode = syncRegistrar().longS2C("hnP2PMode");
        hn$routeAction = syncRegistrar().actionC2S("hnP2PCycle")
            .onServerAction(ignored -> hn$cycle());
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
        if (items < 0 || fluids < 0
            || !((BatchLimitSettings) myDuality).hasBatchLimitCard()
            || !modernization$allowed()) return;
        ((BatchLimitSettings) myDuality).setBatchLimits(items, fluids);
    }

    @Unique
    private boolean modernization$allowed() {
        EntityPlayer player = getInventoryPlayer().player;
        TileEntity tile = myDuality.getHost()
            .getTile();
        return !(player.openContainer != (Object) this || !canInteractWith(player)
            || !hasAccess(SecurityPermissions.BUILD, false)
            || tile == null
            || tile.isInvalid()
            || tile.getWorldObj() != player.worldObj
            || player.getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5) > 64);
    }

    @Inject(method = { "detectAndSendChanges", "func_75142_b" }, at = @At("HEAD"))
    private void modernization$sync(CallbackInfo ci) {
        if (!Platform.isServer()) return;
        PartP2PInterface part = hn$input();
        hn$routeMode.set(part == null ? 0 : ((TunnelPolicy.Host) part).hn$policy().mode + 1);
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
