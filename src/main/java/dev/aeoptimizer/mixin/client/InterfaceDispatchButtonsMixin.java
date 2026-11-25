package dev.aeoptimizer.mixin.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.client.gui.implementations.GuiInterface;
import appeng.client.gui.widgets.GuiSimpleImgButton;
import dev.aeoptimizer.dispatch.BatchLimitSettings;
import dev.aeoptimizer.dispatch.TunnelPolicy;
import dev.aeoptimizer.ui.client.BatchLimitPanel;

@Mixin(value = GuiInterface.class, remap = false)
public abstract class InterfaceDispatchButtonsMixin extends GuiContainer {

    // AE states atlas: sort by amount, blocking, default scheduling, divide, round robin, ascending order.
    @Unique
    private static final int hn$amount = 65, hn$blocked = 21;
    @Unique
    private static final int[] hn$routeIcons = { 240, 73, 241, 48 };
    @Unique
    private GuiSimpleImgButton hn$limit, hn$route;

    private InterfaceDispatchButtonsMixin(Container container) {
        super(container);
    }

    @Inject(method = "addButtons", at = @At("RETURN"))
    private void hn$addButtons(CallbackInfo ci) {
        hn$limit = new GuiSimpleImgButton(guiLeft - 36, guiTop + 26, hn$amount, "");
        hn$route = new GuiSimpleImgButton(guiLeft - 36, guiTop + 26, hn$routeIcons[0], "");
        hn$updateButtons();
        buttonList.add(hn$limit);
        buttonList.add(hn$route);
    }

    @Inject(method = "drawFG", at = @At("HEAD"))
    private void hn$syncButtons(int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        if (hn$limit != null) hn$updateButtons();
    }

    @Unique
    private void hn$updateButtons() {
        BatchLimitSettings settings = (BatchLimitSettings) inventorySlots;
        boolean limited = settings.hasBatchLimitCard();
        hn$limit.setVisibility(limited);
        if (limited) {
            boolean blocked = settings.batchLimitBlocked();
            int icon = blocked ? hn$blocked : hn$amount;
            if (hn$limit.getIconIndex() != icon || hn$limit.getTooltip()
                .isEmpty()) {
                hn$limit.setIconIndex(icon);
                String title = BatchLimitPanel.tr("title");
                hn$limit.setTooltip(blocked ? title + "\n" + BatchLimitPanel.tr("blocked") : title);
            }
        }
        int mode = ((TunnelPolicy.Menu) inventorySlots).hn$mode();
        boolean routed = mode >= 0 && mode < hn$routeIcons.length;
        hn$route.setVisibility(routed);
        if (routed && (hn$route.getIconIndex() != hn$routeIcons[mode] || hn$route.getTooltip()
            .isEmpty())) {
            hn$route.setIconIndex(hn$routeIcons[mode]);
            String key = "p2p." + mode;
            hn$route.setTooltip(BatchLimitPanel.tr(key) + "\n" + BatchLimitPanel.tr(key + ".hint"));
        }
    }

    @Inject(method = { "actionPerformed", "func_146284_a" }, at = @At("HEAD"), cancellable = true)
    private void hn$dispatchAction(GuiButton button, CallbackInfo ci) {
        if (button == hn$limit && ((BatchLimitSettings) inventorySlots).hasBatchLimitCard()) {
            ((BatchLimitPanel.Host) (Object) this).hn$openLimits();
            ci.cancel();
        } else if (button == hn$route && ((TunnelPolicy.Menu) inventorySlots).hn$mode() >= 0) {
            ((TunnelPolicy.Menu) inventorySlots).hn$cycle();
            ci.cancel();
        }
    }
}
