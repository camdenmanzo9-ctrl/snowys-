package com.snowy.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Restyles the vanilla title screen: background (Snowfall / Gradient / Vanilla), "Snowy's" title, dark buttons.
 * The vanilla buttons stay underneath and still handle every click, so all of them keep working
 * (Singleplayer, Multiplayer, Realms, Mods, Options, Quit, language and accessibility icons).
 */
final class SnowysTitleScreen {
    private SnowysTitleScreen() { }

    private static final int FLAKES = 140;

    static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof TitleScreen)) return;
            // over the panorama, before buttons and logo are drawn
            ScreenEvents.afterBackground(screen).register((s, g, mx, my, d) -> {
                if (!vanilla()) background(g, 0, 0, s.width, s.height, s.height);
            });
            // after everything: cover the Minecraft logo + splash, then draw the title and our buttons
            ScreenEvents.afterExtract(screen).register((s, g, mx, my, d) -> overlay(s, g, mx, my));
        });
    }

    private static int logoBottom(Screen s) {
        return s.height / 4 + 44;   // vanilla buttons start at height / 4 + 48
    }

    private static void overlay(Screen s, GuiGraphicsExtractor g, int mx, int my) {
        Font font = Screens.getFont(s);
        int cover = logoBottom(s);
        if (!vanilla()) titleText(s, g, font, cover);   // Vanilla background keeps the Minecraft logo

        drawButtons(s, g, font, mx, my);
    }

    private static boolean vanilla() {
        return ClientModules.background.mode == 2;
    }

    private static void titleText(Screen s, GuiGraphicsExtractor g, Font font, int cover) {
        background(g, 0, 0, s.width, cover, s.height);
        // title
        String title = "Snowy's";
        float scale = 4f;
        int tx = s.width / 2, ty = cover / 2 - 18;
        g.pose().pushMatrix();
        g.pose().translate(tx, ty);
        g.pose().scale(scale, scale);
        g.text(font, title, -font.width(title) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
        String sub = "client";
        g.text(font, sub, tx - font.width(sub) / 2, ty + 38, Module.accent(), true);
    }

    private static void drawButtons(Screen s, GuiGraphicsExtractor g, Font font, int mx, int my) {
        int btn = 0xD0000000 | (ClientModules.buttonColor.rgb() & 0xFFFFFF);
        int btnHover = 0xE0000000 | (Setting.color(ClientModules.buttonColor.hue, Math.min(1f, ClientModules.buttonColor.shade + 0.12f)) & 0xFFFFFF);
        // buttons (icon buttons and the bottom copyright text are left as they are)
        for (AbstractWidget wdg : Screens.getWidgets(s)) {
            if (!(wdg instanceof Button b) || !b.visible) continue;
            if (b.getWidth() <= 20 || b.getY() > s.height - 15) continue;
            int x = b.getX(), y = b.getY(), w = b.getWidth(), h = b.getHeight();
            boolean hover = b.active && mx >= x && mx < x + w && my >= y && my < y + h;
            if (hover) roundRect(g, x - 1, y - 1, w + 2, h + 2, Module.accent());
            roundRect(g, x, y, w, h, hover ? btnHover : btn);
            String label = b.getMessage().getString();
            int color = b.active ? 0xFFFFFFFF : 0xFF707888;
            g.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, color, false);
        }
    }

    /** Box with the four corner pixels cut off so it looks rounded. */
    private static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x + 1, y, x + w - 1, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    /** Night gradient with falling snow, drawn only inside the given rectangle. */
    private static void background(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int screenH) {
        int top = ClientModules.gradTop.rgb(), bottom = ClientModules.gradBottom.rgb();
        for (int y = y0; y < y1; y += 2) g.fill(x0, y, x1, Math.min(y + 2, y1), mix(top, bottom, (float) y / screenH));
        if (ClientModules.background.mode != 0) return;   // Gradient only: no snow
        double t = System.currentTimeMillis() / 1000.0;
        int screenW = x1 - x0;
        for (int i = 0; i < FLAKES; i++) {
            double r1 = rand(i, 1), r2 = rand(i, 2), r3 = rand(i, 3);
            int size = r3 < 0.7 ? 1 : 2;
            double speed = 8 + r3 * 22;                                   // pixels per second
            double fy = (r2 * (screenH + 10) + t * speed) % (screenH + 10) - 5;
            double fx = r1 * screenW + Math.sin(t * 0.6 + i) * 6;
            int px = x0 + (int) fx, py = (int) fy;
            if (py < y0 || py + size > y1 || px < x0 || px + size > x1) continue;
            int alpha = 0x80 + (int) (r3 * 0x7F);
            g.fill(px, py, px + size, py + size, (alpha << 24) | 0xFFFFFF);
        }
    }

    private static double rand(int i, int salt) {
        long h = (i * 0x9E3779B97F4A7C15L) ^ (salt * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 53);
    }

    private static int mix(int a, int b, float f) {
        f = Math.max(0, Math.min(1, f));
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * f);
        int gr = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * f);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * f);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }
}
