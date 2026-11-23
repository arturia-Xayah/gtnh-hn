package dev.aeoptimizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEStack;
import appeng.me.cache.NetworkMonitor;
import dev.aeoptimizer.emitter.ChannelTotal;

@Mixin(value = NetworkMonitor.class, remap = false)
public abstract class EmitterMonitorMixin implements ChannelTotal.Monitor {

    @Unique
    private ChannelTotal modernization$total;

    public ChannelTotal modernization$total() {
        if (modernization$total == null) modernization$total = new ChannelTotal();
        return modernization$total;
    }

    // Hook after AE's nested-operation guards. Cell-provider removals use add=false and positive amounts.
    @Inject(
        method = "postChange",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/me/cache/NetworkMonitor;notifyListenersOfChange(Ljava/lang/Iterable;Lappeng/api/networking/security/BaseActionSource;)V"))
    private void modernization$delta(boolean add, Iterable<IAEStack<?>> changes, BaseActionSource source,
        CallbackInfo ci) {
        if (modernization$total != null) modernization$total.change(add, changes);
    }

    @Inject(method = "forceUpdate", at = @At("HEAD"))
    private void modernization$invalidate(CallbackInfo ci) {
        if (modernization$total != null) modernization$total.invalidate();
    }

    @Inject(method = "onTick", at = @At("RETURN"))
    private void modernization$flush(CallbackInfo ci) {
        if (modernization$total != null) modernization$total.flush((IMEMonitor<?>) (Object) this);
    }
}
