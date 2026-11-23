package dev.aeoptimizer.mixin.client;

import java.util.Collections;

import net.minecraft.client.gui.GuiButton;
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
public abstract class BatchLimitScreenMixin extends GuiContainer {

    @Unique
    private BatchLimitPanel modernization$panel;

    private BatchLimitScreenMixin(Container container) {
        super(container);
    }

    @Unique
    private boolean modernization$open() {
        return modernization$panel != null && modernization$panel.open;
    }

    @Inject(method = { "drawScreen", "func_73863_a" }, at = @At("RETURN"))
    private void modernization$draw(int mx, int my, float partial, CallbackInfo ci) {
        if (!(inventorySlots instanceof BatchLimitSettings)) return;
        BatchLimitSettings settings = (BatchLimitSettings) inventorySlots;
        if (!settings.hasBatchLimitCard()) {
            modernization$panel = null;
            return;
        }
        if (modernization$open()) {
            modernization$panel.draw(mx, my, width, height);
            return;
        }
        GuiButton button = new GuiButton(0, guiLeft - 36, guiTop + 26, 16, 16, "≤");
        button.drawButton(mc, mx, my);
        if (button.mousePressed(mc, mx, my)) drawHoveringText(
            Collections.singletonList(BatchLimitPanel.tr(settings.batchLimitBlocked() ? "blocked" : "title")),
            mx,
            my,
            fontRendererObj);
    }

    @Inject(method = { "mouseClicked", "func_73864_a" }, at = @At("HEAD"), cancellable = true)
    private void modernization$click(int mx, int my, int button, CallbackInfo ci) {
        if (modernization$open()) {
            modernization$panel.click(mx, my, button);
            ci.cancel();
            return;
        }
        if (button == 0 && inventorySlots instanceof BatchLimitSettings
            && ((BatchLimitSettings) inventorySlots).hasBatchLimitCard()
            && mx >= guiLeft - 36
            && mx < guiLeft - 20
            && my >= guiTop + 26
            && my < guiTop + 42) {
            modernization$panel = new BatchLimitPanel((BatchLimitSettings) inventorySlots, width, height);
            ci.cancel();
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
