package dev.aeoptimizer.ui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import dev.aeoptimizer.dispatch.BatchLimitMath;
import dev.aeoptimizer.dispatch.BatchLimitSettings;

/** A modal editor inside the existing AE container; no inventory or network is recreated. */
public final class BatchLimitPanel extends Gui {

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
        apply = new GuiButton(0, x + 12, y + 177, 106, 20, tr("apply"));
        cancel = new GuiButton(1, x + 126, y + 177, 106, 20, tr("cancel"));
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
        drawRect(x, y, x + 244, y + 208, 0xFF292B35);
        drawRect(x + 1, y + 1, x + 243, y + 23, 0xFF4A5265);
        mc.fontRenderer.drawString(tr("title"), x + 12, y + 8, 0xFFFFFF);
        mc.fontRenderer.drawString(tr("items"), x + 12, y + 29, 0xDDE7FF);
        mc.fontRenderer.drawString(tr("fluids"), x + 12, y + 73, 0xDDE7FF);
        items.drawTextBox();
        fluids.drawTextBox();
        mc.fontRenderer.drawSplitString(tr("hint"), x + 12, y + 113, 220, 0xB4BCCB);
        if (invalid || settings.batchLimitBlocked())
            mc.fontRenderer.drawSplitString(tr(invalid ? "invalid" : "blocked"), x + 12, y + 154, 220, 0xFF9999);
        apply.drawButton(mc, mouseX, mouseY);
        cancel.drawButton(mc, mouseX, mouseY);
        GL11.glPopAttrib();
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
