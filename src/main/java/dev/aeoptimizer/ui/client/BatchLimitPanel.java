package dev.aeoptimizer.ui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import appeng.client.gui.ScreenColor;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.client.texture.ExtraBlockTextures;
import appeng.core.localization.ColorUtils;
import dev.aeoptimizer.dispatch.BatchLimitMath;
import dev.aeoptimizer.dispatch.BatchLimitSettings;

/** A modal editor inside the existing AE container; no inventory or network is recreated. */
public final class BatchLimitPanel extends Gui {

    public interface Host {

        void hn$openLimits();
    }

    private final BatchLimitSettings settings;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final GuiTextField items, fluids;
    private final GuiButton apply, cancel;
    private final int x, y;
    public boolean open = true;
    private boolean invalid;

    public BatchLimitPanel(BatchLimitSettings settings, int width, int height) {
        this.settings = settings;
        x = (width - 244) / 2;
        y = (height - 208) / 2;
        items = new GuiTextField(mc.fontRenderer, x + 12, y + 43, 220, 18);
        fluids = new GuiTextField(mc.fontRenderer, x + 12, y + 87, 220, 18);
        items.setMaxStringLength(40);
        fluids.setMaxStringLength(40);
        items.setText(Long.toString(settings.batchItemLimit()));
        fluids.setText(Long.toString(settings.batchFluidLimit()));
        items.setFocused(true);
        apply = new GuiAeButton(0, x + 12, y + 177, 106, 20, tr("apply"), "");
        cancel = new GuiAeButton(1, x + 126, y + 177, 106, 20, tr("cancel"), "");
    }

    public static String tr(String key) {
        return StatCollector.translateToLocal("aeoptimizer.batch." + key);
    }

    public void draw(int mouseX, int mouseY, int width, int height) {
        if (!settings.hasBatchLimitCard()) {
            open = false;
            return;
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        drawRect(0, 0, width, height, 0xA0000000);
        drawPanelFrame();
        int text = ColorUtils.guiTextColorGray.getColor();
        mc.fontRenderer.drawString(tr("title"), x + 12, y + 8, text);
        mc.fontRenderer.drawString(tr("items"), x + 12, y + 29, text);
        mc.fontRenderer.drawString(tr("fluids"), x + 12, y + 73, text);
        items.drawTextBox();
        fluids.drawTextBox();
        mc.fontRenderer.drawSplitString(tr("hint"), x + 12, y + 113, 220, text);
        if (invalid || settings.batchLimitBlocked())
            mc.fontRenderer.drawSplitString(tr(invalid ? "invalid" : "blocked"), x + 12, y + 154, 220, 0x962C26);
        apply.drawButton(mc, mouseX, mouseY);
        cancel.drawButton(mc, mouseX, mouseY);
        GL11.glPopAttrib();
    }

    // Sample the four-pixel bevel and the empty interior of AE's amount dialog.
    // The center sample avoids the quantity fields printed in the original texture.
    private static final int[] U = { 0, 4, 172 }, V = { 0, 4, 103 }, SAMPLE = { 4, 1, 4 };
    private static final int[] LEFT = { 0, 4, 240 }, TOP = { 0, 4, 204 }, WIDTH = { 4, 236, 4 }, HEIGHT = { 4, 200, 4 };

    private void drawPanelFrame() {
        mc.getTextureManager()
            .bindTexture(ExtraBlockTextures.GuiTexture("guis/craftAmt.png"));
        ScreenColor.setGuiColor();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            func_152125_a(
                x + LEFT[col],
                y + TOP[row],
                U[col],
                V[row],
                SAMPLE[col],
                SAMPLE[row],
                WIDTH[col],
                HEIGHT[row],
                256,
                256);
        }
        ScreenColor.resetGuiColor();
    }

    private void apply() {
        try {
            long itemLimit = BatchLimitMath.parse(items.getText()), fluidLimit = BatchLimitMath.parse(fluids.getText());
            settings.setBatchLimits(itemLimit, fluidLimit);
            open = false;
        } catch (IllegalArgumentException | ArithmeticException error) {
            invalid = true;
        }
    }

    public void click(int mx, int my, int button) {
        items.mouseClicked(mx, my, button);
        fluids.mouseClicked(mx, my, button);
        if (button == 0 && apply.mousePressed(mc, mx, my)) apply();
        if (button == 0 && cancel.mousePressed(mc, mx, my)) open = false;
    }

    public void key(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            open = false;
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            apply();
            return;
        }
        if (key == Keyboard.KEY_TAB) {
            boolean next = !items.isFocused();
            items.setFocused(next);
            fluids.setFocused(!next);
            return;
        }
        items.textboxKeyTyped(c, key);
        fluids.textboxKeyTyped(c, key);
        invalid = false;
    }

    public void tick() {
        items.updateCursorCounter();
        fluids.updateCursorCounter();
    }
}
