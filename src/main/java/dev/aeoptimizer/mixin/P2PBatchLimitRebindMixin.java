package dev.aeoptimizer.mixin;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.parts.p2p.PartP2PInterface;
import appeng.parts.p2p.PartP2PTunnel;
import dev.aeoptimizer.dispatch.BatchLimitSettings;
import dev.aeoptimizer.dispatch.TunnelPolicy;

@Mixin(value = PartP2PInterface.class, remap = false)
public abstract class P2PBatchLimitRebindMixin implements TunnelPolicy.Host {

    @Unique
    private final TunnelPolicy hn$route = new TunnelPolicy();

    public TunnelPolicy hn$policy() {
        return hn$route;
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void hn$save(NBTTagCompound n, CallbackInfo ci) {
        hn$route.write(n);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void hn$load(NBTTagCompound n, CallbackInfo ci) {
        hn$route.read(n);
    }

    // AE replaces the local part on a memory-card rebind. copyContents gets the old local
    // endpoint, whereas copySettings can receive the remote input shared by every output.
    @Inject(method = "copyContents", at = @At("RETURN"))
    private void modernization$keepLocalLimits(PartP2PTunnel<?> previous, CallbackInfo ci) {
        if (!(previous instanceof PartP2PInterface)) return;
        hn$route.setMode(((TunnelPolicy.Host) previous).hn$policy().mode);
        BatchLimitSettings oldLimits = (BatchLimitSettings) ((PartP2PInterface) previous).getInterfaceDuality();
        BatchLimitSettings newLimits = (BatchLimitSettings) ((PartP2PInterface) (Object) this).getInterfaceDuality();
        if (oldLimits.batchItemLimit() != 0 || oldLimits.batchFluidLimit() != 0)
            newLimits.setBatchLimits(oldLimits.batchItemLimit(), oldLimits.batchFluidLimit());
    }
}
