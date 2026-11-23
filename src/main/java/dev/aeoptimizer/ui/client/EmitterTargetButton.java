package dev.aeoptimizer.ui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.StatCollector;

import appeng.client.gui.widgets.ITooltip;
import appeng.container.implementations.ContainerAdvancedLevelEmitter;
import dev.aeoptimizer.emitter.EmitterTargets;

public final class EmitterTargetButton extends GuiButton implements ITooltip {

    private final ContainerAdvancedLevelEmitter container;
    private final int slot;

    public EmitterTargetButton(ContainerAdvancedLevelEmitter container, int slot, int x, int y) {
        super(510 + slot, x, y, 70, 17, "");
        this.container = container;
        this.slot = slot;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        displayString = StatCollector
            .translateToLocal("aeorderoptimizer.emitter.target." + ((EmitterTargets) container).target(slot));
        super.drawButton(mc, mouseX, mouseY);
    }

    public String getMessage() {
        int mode = ((EmitterTargets) container).target(slot);
        String help = StatCollector.translateToLocal("aeorderoptimizer.emitter.switch");
        if (mode == EmitterTargets.EXACT || !container.isSlotActive(slot)) return help;
        return StatCollector
            .translateToLocalFormatted("aeorderoptimizer.emitter.count." + mode, container.getStoredAmount(slot)) + "\n"
            + help;
    }

    public int xPos() {
        return xPosition;
    }

    public int yPos() {
        return yPosition;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isVisible() {
        return visible;
    }
}
