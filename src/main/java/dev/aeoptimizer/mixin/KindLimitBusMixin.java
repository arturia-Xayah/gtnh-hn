package dev.aeoptimizer.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import appeng.api.config.AccessRestriction;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEStackType;
import appeng.me.storage.StorageBusInventoryHandler;
import appeng.parts.misc.PartStorageBus;
import appeng.util.target.TileTarget;
import dev.aeoptimizer.storage.KindLimitedHandler;

@Mixin(value = PartStorageBus.class, remap = false)
public abstract class KindLimitBusMixin {

    @Redirect(
        method = "getInternalHandler",
        at = @At(value = "NEW", target = "appeng/me/storage/StorageBusInventoryHandler"))
    private StorageBusInventoryHandler hn$limit(IMEInventory inv, IAEStackType type,
        Supplier<AccessRestriction> reshuffle, TileTarget target) {
        return KindLimitedHandler.create((PartStorageBus) (Object) this, inv, type, reshuffle, target);
    }
}
