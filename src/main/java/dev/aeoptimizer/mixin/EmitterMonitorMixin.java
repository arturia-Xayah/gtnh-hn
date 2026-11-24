package dev.aeoptimizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    // Observe the keys AE actually processes, including repeated and re-forwarded notifications.
    @ModifyArg(
        method = "postChange",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/me/helpers/GenericInterestManager;containsKey(Lappeng/api/storage/data/IAEStack;)Z"),
        index = 0)
    private IAEStack<?> modernization$changedKey(IAEStack<?> key) {
        if (modernization$total != null) modernization$total.changedKey(key);
        return key;
    }

    @Inject(method = "forceUpdate", at = @At("HEAD"))
    private void modernization$invalidate(CallbackInfo ci) {
        if (modernization$total != null) modernization$total.invalidate();
    }

    @Inject(method = "onTick", at = @At("RETURN"))
    private void modernization$flush(CallbackInfo ci) {
        if (modernization$total != null) modernization$total.flush((NetworkMonitor<?>) (Object) this);
    }
}
