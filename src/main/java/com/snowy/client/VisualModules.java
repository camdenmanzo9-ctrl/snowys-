package com.snowy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.List;

/** Zoom, Fullbright, Custom Crosshair, Toggle Sprint. */
final class VisualModules {
    private VisualModules() { }

    static Module crosshair;
    static Setting chColor, chSize, chGap, chThick;

    static void add(List<Module> list) {
        // Hold C (change in Controls) to zoom while Zoom is on.
        KeyMapping zoomKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.snowys.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, SnowysClient.CATEGORY));

        list.add(new Module("Zoom", Module.Cat.VISUAL, false, 0, 0) {
            final Setting fov = add(Setting.num("Zoom FOV", 20, 5, 50, 1, ""));
            final Setting smooth = add(Setting.bool("Smooth camera", true));
            boolean zooming, oldSmooth;
            int oldFov;

            @Override public void tick(Minecraft mc) {
                boolean want = on && zoomKey.isDown() && mc.screen == null;
                if (want && !zooming) {
                    zooming = true;
                    oldFov = mc.options.fov().get();
                    oldSmooth = mc.options.smoothCamera;
                    force(mc.options.fov(), fov.i());
                    if (smooth.bool) mc.options.smoothCamera = true;
                } else if (!want && zooming) {
                    stop(mc);
                }
            }

            @Override public void disable() {
                if (zooming) stop(Minecraft.getInstance());
            }

            void stop(Minecraft mc) {
                zooming = false;
                force(mc.options.fov(), oldFov);
                mc.options.smoothCamera = oldSmooth;
            }
        });

        list.add(new Module("Fullbright", Module.Cat.VISUAL, false, 0, 0) {
            boolean applied;
            double oldGamma;

            @Override public void tick(Minecraft mc) {
                if (on && !applied) {
                    oldGamma = mc.options.gamma().get();
                    force(mc.options.gamma(), 15.0);
                    applied = true;
                }
            }

            @Override public void disable() {
                if (!applied) return;
                force(Minecraft.getInstance().options.gamma(), oldGamma);
                applied = false;
            }
        });

        crosshair = new Module("Custom Crosshair", Module.Cat.VISUAL, false, 0, 0) { };
        chColor = crosshair.add(Setting.color("Color", 0f, 1f));
        chSize = crosshair.add(Setting.num("Size", 4, 1, 10, 1, ""));
        chGap = crosshair.add(Setting.num("Gap", 2, 0, 8, 1, ""));
        chThick = crosshair.add(Setting.num("Thickness", 1, 1, 3, 1, ""));
        list.add(crosshair);

        list.add(new Module("Toggle Sprint", Module.Cat.VISUAL, false, 0, 0) {
            @Override public void tick(Minecraft mc) {
                if (on && mc.player != null) mc.options.keySprint.setDown(true);
            }

            @Override public void disable() {
                Minecraft.getInstance().options.keySprint.setDown(false);
            }
        });
    }

    /** A thin plus with a small gap in the middle. */
    static void drawCrosshair(GuiGraphicsExtractor g, Minecraft mc) {
        int t = chThick.i(), gap = chGap.i(), len = chSize.i(), c = chColor.rgb();
        int cx = mc.getWindow().getGuiScaledWidth() / 2 - t / 2, cy = mc.getWindow().getGuiScaledHeight() / 2 - t / 2;
        g.fill(cx - gap - len, cy, cx - gap, cy + t, c);              // left
        g.fill(cx + t + gap, cy, cx + t + gap + len, cy + t, c);      // right
        g.fill(cx, cy - gap - len, cx + t, cy - gap, c);              // top
        g.fill(cx, cy + t + gap, cx + t, cy + t + gap + len, c);      // bottom
    }

    /** Sets an option past its normal slider limits (used for zoom FOV and fullbright gamma). */
    static <T> void force(OptionInstance<T> opt, T value) {
        try {
            Field f = OptionInstance.class.getDeclaredField("value");
            f.setAccessible(true);
            f.set(opt, value);
        } catch (Exception ignored) { }
    }
}
