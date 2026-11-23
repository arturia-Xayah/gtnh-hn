package dev.aeoptimizer.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.config.Upgrades;

/** Extend the enum before it escapes class initialization; no reflective EnumHelper or JVM config needed. */
@Mixin(value = Upgrades.class, remap = false)
public abstract class BatchLimitUpgradeMixin {

    @Shadow
    @Final
    @Mutable
    private static Upgrades[] $VALUES;

    @Invoker("<init>")
    private static Upgrades modernization$newUpgrade(String name, int ordinal, int tier) {
        throw new AssertionError();
    }

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void modernization$addUpgrade(CallbackInfo ci) {
        int count = $VALUES.length;
        Upgrades next = modernization$newUpgrade("MODERNIZATION_BATCH_LIMIT", count, 1);
        $VALUES = Arrays.copyOf($VALUES, count + 1);
        $VALUES[count] = next;
    }
}
