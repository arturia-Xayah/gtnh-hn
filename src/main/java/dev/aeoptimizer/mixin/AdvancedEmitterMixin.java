package dev.aeoptimizer.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.config.BooleanOperation;
import appeng.api.parts.IAdvancedLevelEmitter;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.parts.automation.PartAdvancedLevelEmitter;
import appeng.parts.automation.PartUpgradeable;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import dev.aeoptimizer.emitter.ChannelTotal;
import dev.aeoptimizer.emitter.EmitterTargets;
import dev.aeoptimizer.emitter.EmitterUpdates;

@Mixin(value = PartAdvancedLevelEmitter.class, remap = false)
public abstract class AdvancedEmitterMixin extends PartUpgradeable
    implements EmitterTargets, ChannelTotal.Listener, EmitterUpdates.Output {

    private AdvancedEmitterMixin(ItemStack is) {
        super(is);
    }

    @Shadow
    @Final
    private boolean[] slotActive, slotInverted;
    @Shadow
    @Final
    private long[] lastReportedValue, amount;
    @Shadow
    @Final
    private IAEStackInventory config;

    @Shadow
    public abstract BooleanOperation getLogicMode();

    @Shadow
    private void updateState() {
        throw new AssertionError();
    }

    @Shadow
    private void configureWatchers() {
        throw new AssertionError();
    }

    @Unique
    private final byte[] modernization$modes = new byte[IAdvancedLevelEmitter.SLOT_COUNT];
    @Unique
    private final ChannelTotal[] modernization$channels = new ChannelTotal[3];
    @Unique
    private final boolean[] modernization$ready = new boolean[3];
    @Unique
    private final long[] modernization$values = new long[3];

    public int target(int slot) {
        return modernization$modes[slot];
    }

    public void setTarget(int slot, int mode) {
        if (slot < 0 || slot >= modernization$modes.length || mode < EXACT || mode > FLUIDS || mode == target(slot))
            return;
        modernization$modes[slot] = (byte) mode;
        getHost().markForSave();
        configureWatchers();
    }

    @Unique
    private void modernization$bind() {
        if (Platform.isClient()) return;
        for (int mode = ITEMS; mode <= FLUIDS; mode++) {
            boolean wanted = false;
            for (int slot = 0; slot < modernization$modes.length; slot++) {
                if (slotActive[slot] && target(slot) == mode) {
                    wanted = true;
                    break;
                }
            }
            ChannelTotal next = null;
            if (wanted) {
                try {
                    IMEMonitor<?> monitor = mode == ITEMS ? getProxy().getStorage()
                        .getItemInventory()
                        : getProxy().getStorage()
                            .getFluidInventory();
                    if (monitor instanceof ChannelTotal.Monitor)
                        next = ((ChannelTotal.Monitor) monitor).modernization$total();
                } catch (GridAccessException ignored) {}
            }
            modernization$attach(mode, next);
        }
    }

    @Unique
    private void modernization$attach(int mode, ChannelTotal next) {
        ChannelTotal old = modernization$channels[mode];
        if (old == next) return;
        if (old != null) old.unsubscribe(this);
        modernization$channels[mode] = next;
        modernization$ready[mode] = false;
        modernization$values[mode] = 0;
        if (next != null) next.subscribe(this);
    }

    @Override
    public void removeFromWorld() {
        modernization$attach(ITEMS, null);
        modernization$attach(FLUIDS, null);
        EmitterUpdates.INSTANCE.remove(this);
        super.removeFromWorld();
    }

    @Inject(method = "configureWatchers", at = @At("HEAD"))
    private void modernization$bind(CallbackInfo ci) {
        modernization$bind();
    }

    @Inject(
        method = "setSlotActive",
        at = @At(value = "INVOKE", target = "Lappeng/parts/automation/PartAdvancedLevelEmitter;updateState()V"))
    private void modernization$activation(int slot, boolean active, CallbackInfo ci) {
        modernization$bind();
        int mode = target(slot);
        if (mode != EXACT) lastReportedValue[slot] = modernization$values[mode];
    }

    public void modernization$totalChanged(ChannelTotal channel, long value) {
        for (int mode = ITEMS; mode <= FLUIDS; mode++) if (modernization$channels[mode] == channel) {
            modernization$ready[mode] = true;
            modernization$values[mode] = value;
            for (int slot = 0; slot < modernization$modes.length; slot++)
                if (target(slot) == mode) lastReportedValue[slot] = value;
            EmitterUpdates.INSTANCE.schedule(this);
        }
    }

    public void modernization$publish() {
        updateState();
    }

    @Redirect(
        method = { "configureWatchers", "onStackChange" },
        at = @At(
            value = "INVOKE",
            target = "Lappeng/tile/inventory/IAEStackInventory;getAEStackInSlot(I)Lappeng/api/storage/data/IAEStack;"))
    private IAEStack<?> modernization$exact(IAEStackInventory inventory, int slot) {
        return target(slot) == EXACT ? inventory.getAEStackInSlot(slot) : null;
    }

    @Inject(method = "updateReportingValueForSlot", at = @At("HEAD"), cancellable = true)
    private void modernization$stored(int slot, CallbackInfo ci) {
        int mode = target(slot);
        if (mode != EXACT) {
            lastReportedValue[slot] = modernization$values[mode];
            ci.cancel();
        }
    }

    @Inject(method = "isLevelEmitterOn", at = @At("HEAD"), cancellable = true)
    private void modernization$signal(CallbackInfoReturnable<Boolean> cir) {
        if (Platform.isClient()) return;
        boolean hasTotal = false;
        for (int i = 0; i < modernization$modes.length; i++) if (slotActive[i] && target(i) != EXACT) {
            hasTotal = true;
            break;
        }
        if (!hasTotal) return;
        boolean and = getLogicMode() == BooleanOperation.AND;
        boolean result = and;
        for (int i = 0; i < modernization$modes.length; i++) {
            int mode = target(i);
            if (!slotActive[i] || (mode == EXACT && config.getAEStackInSlot(i) == null)) continue;
            boolean hit = (mode == EXACT || modernization$ready[mode])
                && (slotInverted[i] ? lastReportedValue[i] < amount[i] : lastReportedValue[i] >= amount[i]);
            result = and ? result && hit : result || hit;
        }
        cir.setReturnValue(getProxy().isActive() && result);
    }

    @Unique
    private void modernization$load(NBTTagCompound n) {
        byte[] raw = n.getByteArray("modernizationEmitterTargets");
        for (int i = 0; i < modernization$modes.length; i++)
            modernization$modes[i] = i < raw.length && raw[i] >= EXACT && raw[i] <= FLUIDS ? raw[i] : EXACT;
    }

    @Inject(method = "readFromNBT", at = @At("HEAD"))
    private void modernization$read(NBTTagCompound n, CallbackInfo ci) {
        modernization$load(n);
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void modernization$write(NBTTagCompound n, CallbackInfo ci) {
        n.setByteArray("modernizationEmitterTargets", modernization$modes.clone());
    }

    @Inject(method = "uploadSettings", at = @At("HEAD"))
    private void modernization$paste(SettingsFrom from, NBTTagCompound n, CallbackInfo ci) {
        modernization$load(n);
    }

    @Inject(method = "downloadSettings", at = @At("RETURN"))
    private void modernization$copy(SettingsFrom to, CallbackInfoReturnable<NBTTagCompound> cir) {
        cir.getReturnValue()
            .setByteArray("modernizationEmitterTargets", modernization$modes.clone());
    }
}
