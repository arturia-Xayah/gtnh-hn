package dev.aeoptimizer.mixin.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.parts.IAdvancedLevelEmitter;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiAdvancedLevelEmitter;
import appeng.container.implementations.ContainerAdvancedLevelEmitter;
import dev.aeoptimizer.emitter.EmitterTargets;
import dev.aeoptimizer.ui.client.EmitterTargetButton;

@Mixin(value = GuiAdvancedLevelEmitter.class, remap = false)
public abstract class EmitterScreenMixin extends AEBaseGui {

    @Shadow
    @Final
    private ContainerAdvancedLevelEmitter container;
    @Unique
    private final GuiButton[] modernization$buttons = new GuiButton[IAdvancedLevelEmitter.SLOT_COUNT];

    private EmitterScreenMixin(Container c) {
        super(c);
    }

    @Inject(method = { "initGui", "func_73866_w_" }, at = @At("RETURN"))
    private void modernization$init(CallbackInfo ci) {
        for (int i = 0; i < modernization$buttons.length; i++) {
            GuiButton b = new EmitterTargetButton(container, i, guiLeft + 170, guiTop + 23 + i * 19);
            modernization$buttons[i] = b;
            buttonList.add(b);
        }
    }

    @Inject(method = { "actionPerformed", "func_146284_a" }, at = @At("HEAD"), cancellable = true)
    private void modernization$select(GuiButton button, CallbackInfo ci) {
        EmitterTargets targets = (EmitterTargets) container;
        for (int i = 0; i < modernization$buttons.length; i++) if (button == modernization$buttons[i]) {
            targets.setTarget(i, (targets.target(i) + 1) % 3);
            ci.cancel();
            return;
        }
    }
}
