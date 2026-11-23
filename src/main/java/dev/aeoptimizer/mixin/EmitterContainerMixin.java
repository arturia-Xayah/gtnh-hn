package dev.aeoptimizer.mixin;

import net.minecraft.entity.player.InventoryPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.parts.IAdvancedLevelEmitter;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerAdvancedLevelEmitter;
import appeng.container.sync.handlers.IntSyncHandler;
import appeng.util.Platform;
import dev.aeoptimizer.emitter.EmitterTargets;

@Mixin(value = ContainerAdvancedLevelEmitter.class, remap = false)
public abstract class EmitterContainerMixin extends AEBaseContainer implements EmitterTargets {

    @Shadow
    @Final
    private IAdvancedLevelEmitter lvlEmitter;
    @Unique
    private final IntSyncHandler[] modernization$targets = new IntSyncHandler[IAdvancedLevelEmitter.SLOT_COUNT];

    private EmitterContainerMixin(InventoryPlayer ip, Object anchor) {
        super(ip, anchor);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void modernization$init(InventoryPlayer ip, IAdvancedLevelEmitter emitter, CallbackInfo ci) {
        for (int i = 0; i < modernization$targets.length; i++) {
            final int slot = i;
            modernization$targets[i] = syncRegistrar().intSync("modernizationTarget" + i)
                .onServerChange((old, value) -> ((EmitterTargets) emitter).setTarget(slot, value));
        }
    }

    @Inject(method = { "detectAndSendChanges", "func_75142_b" }, at = @At("HEAD"))
    private void modernization$sync(CallbackInfo ci) {
        if (Platform.isServer()) for (int i = 0; i < modernization$targets.length; i++)
            modernization$targets[i].set(((EmitterTargets) lvlEmitter).target(i));
    }

    public int target(int slot) {
        return modernization$targets[slot].get();
    }

    public void setTarget(int slot, int mode) {
        modernization$targets[slot].set(mode);
    }
}
