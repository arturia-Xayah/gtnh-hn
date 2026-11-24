package dev.structureterminal.output;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import appeng.core.localization.GuiText;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.tileentities.machines.outputme.base.MTEHatchOutputMEBase;

/** Native GT controls, with a separate row for each storage channel. */
final class MixedOutputGui extends MTEHatchBaseGui<MixedOutputBus> {

    MixedOutputGui(MixedOutputBus bus) {
        super(bus);
    }

    @Override
    protected int getBasePanelHeight() {
        return 234;
    }

    @Override
    protected int getBasePanelWidth() {
        return 216;
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager sync) {
        ParentWidget<?> area = getEmptyContent();
        cellSlot(area, MixedOutputBus.ITEM_SLOT, "itemCell", 0);
        cellSlot(area, MixedOutputBus.FLUID_SLOT, "fluidCell", 98);
        row(area, machine.getProvider(), "items", 22);
        row(area, machine.fluids, "fluids", 62);
        return area;
    }

    private void cellSlot(ParentWidget<?> area, int slot, String label, int x) {
        String name = MixedOutputBus.tr(label);
        area.child(
            new ItemSlot().pos(x, 0)
                .slot(new ModularSlot(machine.inventoryHandler, slot).singletonSlotGroup())
                .addTooltipLine(name));
        area.child(
            new TextWidget<>(IKey.str(name)).pos(x + 20, 0)
                .size(76, 18));
    }

    private void row(ParentWidget<?> area, MTEHatchOutputMEBase<?> provider, String channel, int y) {
        area.child(
            new TextWidget<>(IKey.str(MixedOutputBus.tr(channel))).pos(0, y)
                .size(190, 12));
        BooleanSyncValue cache = new BooleanSyncValue(provider::getCacheMode, provider::setCacheMode).allowC2S();
        BooleanSyncValue check = new BooleanSyncValue(provider::getCheckMode, provider::setCheckMode).allowC2S();
        IntSyncValue priority = new IntSyncValue(provider::getPriority, provider::setPriority).allowC2S();
        IntSyncValue refresh = new IntSyncValue(
            () -> provider.getRefreshTime() / 20,
            seconds -> provider.setRefreshTime(Math.max(2, Math.min(seconds, Integer.MAX_VALUE / 20)) * 20)).allowC2S();
        area.child(
            new ToggleButton().pos(0, y + 14)
                .size(18)
                .value(cache)
                .overlay(GuiTextures.FOLDER)
                .addTooltipLine(StatCollector.translateToLocal("GT5U.hatch.outputme.toggle_caching")));
        area.child(
            new TextFieldWidget().pos(22, y + 16)
                .size(62, 14)
                .formatAsInteger(true)
                .value(priority)
                .numbersInt(Integer.MIN_VALUE, Integer.MAX_VALUE)
                .setMaxLength(11)
                .tooltip(t -> t.addLine(GuiText.Priority.getLocal()))
                .setEnabledIf(w -> cache.getBoolValue()));
        area.child(
            new ToggleButton().pos(90, y + 14)
                .size(18)
                .value(check)
                .overlay(GuiTextures.SEARCH)
                .addTooltipLine(StatCollector.translateToLocal("GT5U.hatch.outputme.toggle_checking")));
        area.child(
            new TextFieldWidget().pos(114, y + 16)
                .size(70, 14)
                .formatAsInteger(true)
                .value(refresh)
                .numbersInt(2, Integer.MAX_VALUE / 20)
                .setMaxLength(9)
                .tooltip(t -> t.addLine(StatCollector.translateToLocal("GT5U.hatch.outputme.refresh_time"))));
    }
}
