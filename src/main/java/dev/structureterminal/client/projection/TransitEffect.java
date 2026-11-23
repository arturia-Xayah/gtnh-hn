package dev.structureterminal.client.projection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Short, translucent arrival pulse in the portal's two colors. */
public final class TransitEffect {

    private static long started;
    private static int primary, secondary;
    private static net.minecraft.world.World world;

    public static void begin(NBTTagCompound tag) {
        started = System.nanoTime();
        primary = tag.getInteger("primary") & 0xffffff;
        secondary = tag.getInteger("secondary") & 0xffffff;
        world = Minecraft.getMinecraft().theWorld;
        Minecraft mc = Minecraft.getMinecraft();
        if (world != null && mc.thePlayer != null) for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            world.spawnParticle(
                "portal",
                mc.thePlayer.posX + Math.cos(angle),
                mc.thePlayer.posY + .5,
                mc.thePlayer.posZ + Math.sin(angle),
                -.15 * Math.cos(angle),
                .1,
                -.15 * Math.sin(angle));
        }
    }

    @SubscribeEvent
    public void draw(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || world != Minecraft.getMinecraft().theWorld) return;
        double t = (System.nanoTime() - started) / 750_000_000.0;
        if (t < 0 || t >= 1) return;
        int w = event.resolution.getScaledWidth(), h = event.resolution.getScaledHeight();
        double fade = (1 - t) * (1 - t);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            Gui.drawRect(0, 0, w, h, ((int) (35 * fade) << 24) | primary);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(
                (secondary >> 16 & 255) / 255f,
                (secondary >> 8 & 255) / 255f,
                (secondary & 255) / 255f,
                (float) (.3 * fade));
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            double radius = .1 + 1.1 * t;
            for (int i = 0; i <= 96; i++) {
                double a = Math.PI * 2 * i / 96;
                GL11.glVertex2d(w * (.5 + Math.cos(a) * radius), h * (.5 + Math.sin(a) * radius));
                GL11.glVertex2d(w * (.5 + Math.cos(a) * (radius + .035)), h * (.5 + Math.sin(a) * (radius + .035)));
            }
            GL11.glEnd();
        } finally {
            GL11.glPopAttrib();
        }
    }
}
