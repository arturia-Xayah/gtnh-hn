package dev.aeoptimizer.mixin.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.client.gui.AEBaseGui;
import dev.aeoptimizer.dispatch.BatchLimitSettings;
import dev.aeoptimizer.ui.client.BatchLimitPanel;

@Mixin(value = AEBaseGui.class, remap = false)
public abstract class BatchLimitScreenMixin extends GuiContainer implements BatchLimitPanel.Host {

    @Unique
    private BatchLimitPanel modernization$panel;

    private BatchLimitScreenMixin(Container container) {
        super(container);
    }

    @Unique
    private boolean modernization$open() {
        return modernization$panel != null && modernization$panel.open;
    }

    public void hn$openLimits() {
        modernization$panel = new BatchLimitPanel((BatchLimitSettings) inventorySlots, width, height);
    }

    @Inject(method = { "drawScreen", "func_73863_a" }, at = @At("RETURN"))
    private void modernization$draw(int mx, int my, float partial, CallbackInfo ci) {
        if (modernization$open()) modernization$panel.draw(mx, my, width, height);
    }

    @Inject(method = { "mouseClicked", "func_73864_a" }, at = @At("HEAD"), cancellable = true)
    private void modernization$click(int mx, int my, int button, CallbackInfo ci) {
        if (modernization$open()) {
            modernization$panel.click(mx, my, button);
            ci.cancel();
            return;
        }
    }

    @Inject(method = { "keyTyped", "func_73869_a" }, at = @At("HEAD"), cancellable = true)
    private void modernization$key(char c, int key, CallbackInfo ci) {
        if (modernization$open()) {
            modernization$panel.key(c, key);
            ci.cancel();
        }
    }

    @Inject(method = { "updateScreen", "func_73876_c" }, at = @At("RETURN"))
    private void modernization$tick(CallbackInfo ci) {
        if (modernization$open()) modernization$panel.tick();
    }

    @Inject(method = { "mouseMovedOrUp", "func_146286_b" }, at = @At("HEAD"), cancellable = true)
    private void modernization$release(int mx, int my, int button, CallbackInfo ci) {
        if (modernization$open()) ci.cancel();
    }

    @Inject(method = { "handleMouseInput", "func_146274_d" }, at = @At("HEAD"), cancellable = true)
    private void modernization$wheel(CallbackInfo ci) {
        if (modernization$open() && Mouse.getEventDWheel() != 0) ci.cancel();
    }

    @Inject(method = "handleDragNDrop", at = @At("HEAD"), cancellable = true)
    private void modernization$drag(GuiContainer gui, int mx, int my, ItemStack stack, int button,
        CallbackInfoReturnable<Boolean> cir) {
        if (modernization$open()) cir.setReturnValue(true);
    }
}
