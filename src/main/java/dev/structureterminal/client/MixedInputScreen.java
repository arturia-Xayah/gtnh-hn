package dev.structureterminal.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.slots.VirtualMEPhantomSlot;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.util.item.AEFluidStackType;
import appeng.util.item.AEItemStackType;
import dev.structureterminal.stocking.MixedInputBus;
import dev.structureterminal.stocking.MixedInputContainer;

public final class MixedInputScreen extends AEBaseGui {

    private final MixedInputContainer menu;
    private GuiButton auto;
    private GuiTextField minimum, interval;

    public MixedInputScreen(EntityPlayer p, MixedInputBus bus) {
        super(new MixedInputContainer(p, bus));
        menu = (MixedInputContainer) inventorySlots;
        xSize = 194;
        ySize = 240;
    }

    private static String tr(String k) {
        return StatCollector.translateToLocal("mixed.input." + k);
    }

    @Override
    public void initGui() {
        super.initGui();
        for (int i = 0; i < 16; i++) {
            final int slot = i;
            registerVirtualSlots(
                new VirtualMEPhantomSlot(
                    8 + (i % 4) * 18,
                    28 + (i / 4) * 18,
                    menu.bus.filters,
                    i,
                    (virtualSlot, type, button) -> !menu.auto.get()
                        && (type == AEItemStackType.ITEM_STACK_TYPE || type == AEFluidStackType.FLUID_STACK_TYPE)));
            registerVirtualSlots(new VirtualMESlot(114 + (i % 4) * 18, 28 + (i / 4) * 18, i) {

                @Override
                public IAEStack<?> getAEStack() {
                    IAEStack<?> filter = menu.bus.filters.getAEStackInSlot(slot);
                    return filter == null ? null
                        : filter.copy()
                            .setStackSize(menu.amounts[slot].get());
                }
            });
        }
        auto = new GuiButton(0, guiLeft + 8, guiTop + 108, 178, 18, "");
        auto.enabled = menu.bus.autoPullAvailable;
        buttonList.add(auto);
        minimum = new GuiTextField(fontRendererObj, guiLeft + 48, guiTop + 130, 52, 16);
        interval = new GuiTextField(fontRendererObj, guiLeft + 142, guiTop + 130, 44, 16);
        minimum.setMaxStringLength(10);
        interval.setMaxStringLength(10);
        minimum.setText(Integer.toString(menu.minimum.get()));
        interval.setText(Integer.toString(menu.interval.get()));
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b == auto && auto.enabled) menu.auto.set(!menu.auto.get());
    }

    @Override
    public void drawFG(int x, int y, int mx, int my) {
        fontRendererObj.drawString(tr("title"), 8, 6, 0xe8f7ff);
        fontRendererObj.drawString(tr("filter"), 8, 17, 0xd0d8e0);
        fontRendererObj.drawString(tr("stock"), 114, 17, 0xd0d8e0);
        fontRendererObj.drawString(tr("minimum"), 8, 134, 0xd0d8e0);
        fontRendererObj.drawString(tr("interval"), 104, 134, 0xd0d8e0);
        auto.displayString = tr("auto") + (menu.auto.get() ? " ✓" : " □");
    }

    @Override
    public void drawBG(int x, int y, int mx, int my) {
        drawRect(x, y, x + xSize, y + ySize, 0xff27313d);
        for (int i = 0; i < 16; i++) {
            slot(x + 7 + i % 4 * 18, y + 27 + i / 4 * 18);
            slot(x + 113 + i % 4 * 18, y + 27 + i / 4 * 18);
        }
        slot(x + 85, y + 40);
        slot(x + 85, y + 76);
        for (int i = 0; i < 27; i++) slot(x + 7 + i % 9 * 18, y + 157 + i / 9 * 18);
        for (int i = 0; i < 9; i++) slot(x + 7 + i * 18, y + 215);
        if (!minimum.isFocused()) minimum.setText(Integer.toString(menu.minimum.get()));
        if (!interval.isFocused()) interval.setText(Integer.toString(menu.interval.get()));
        minimum.drawTextBox();
        interval.drawTextBox();
    }

    private static void slot(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xff17212a);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xff697882);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        if (menu.bus.autoPullAvailable) {
            minimum.mouseClicked(x, y, button);
            interval.mouseClicked(x, y, button);
        }
    }

    @Override
    protected void keyTyped(char c, int key) {
        boolean handled = minimum.textboxKeyTyped(c, key) || interval.textboxKeyTyped(c, key);
        if (handled) {
            try {
                menu.minimum.set(Math.max(1, Integer.parseInt(minimum.getText())));
            } catch (NumberFormatException ignored) {}
            try {
                menu.interval.set(Math.max(1, Integer.parseInt(interval.getText())));
            } catch (NumberFormatException ignored) {}
        } else super.keyTyped(c, key);
    }
}
