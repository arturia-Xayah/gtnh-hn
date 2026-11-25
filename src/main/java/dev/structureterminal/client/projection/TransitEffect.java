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
            // Concentric crests follow the entry flash; use screen pixels to keep them circular on ultrawide displays.
            double scale = Math.hypot(w, h) * .5;
            for (int crest = 0; crest < 3; crest++) {
                double age = t - crest * .085;
                if (age <= 0) continue;
                double radius = scale * (.04 + 1.20 * age);
                double band = scale * (.012 + .012 * age);
                int rgb = crest == 1 ? primary : secondary;
                GL11.glColor4f(
                    (rgb >> 16 & 255) / 255f,
                    (rgb >> 8 & 255) / 255f,
                    (rgb & 255) / 255f,
                    (float) (.32 * fade / (1 + crest * .4)));
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                for (int i = 0; i <= 96; i++) {
                    double angle = Math.PI * 2 * i / 96, dx = Math.cos(angle), dy = Math.sin(angle);
                    GL11.glVertex2d(w * .5 + dx * radius, h * .5 + dy * radius);
                    GL11.glVertex2d(w * .5 + dx * (radius + band), h * .5 + dy * (radius + band));
                }
                GL11.glEnd();
            }
        } finally {
            GL11.glPopAttrib();
        }
    }
}
