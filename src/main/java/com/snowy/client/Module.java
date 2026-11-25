package com.snowy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** One thing you can turn on/off in the ClickGUI. HUD modules also have a position you can drag. */
public abstract class Module {
    public enum Cat { HUD, VISUAL, COSMETIC, PANELS, CLIENT }

    public final String name;
    public final Cat cat;
    public final boolean hud;   // drawn on screen and movable in the HUD editor
    public final List<Setting> settings = new ArrayList<>();
    public boolean on, expanded;
    public int x, y, w = 40, h = 12;
    // added to every HUD module by Modules.init()
    Setting scale, ownColors, ownText, ownBox;

    protected Module(String name, Cat cat, boolean hud, int x, int y) {
        this.name = name;
        this.cat = cat;
        this.hud = hud;
        this.x = x;
        this.y = y;
    }

    /** Size on screen after Scale. */
    public int sw() { return scale == null ? w : (int) Math.ceil(w * scale.num / 100); }
    public int sh() { return scale == null ? h : (int) Math.ceil(h * scale.num / 100); }

    void addHudSettings() {
        scale = add(Setting.num("Scale", 100, 50, 200, 5, "%"));
        ownColors = add(Setting.bool("Own colors", false));
        ownText = add(Setting.color("Text color", 0f, 1f));
        ownBox = add(Setting.color("Box color", 0f, 0f));
    }

    protected Setting add(Setting s) {
        settings.add(s);
        return s;
    }

    public void toggle() {
        on = !on;
        if (!on) disable();
        Modules.save();
    }

    /** Called when turned off. */
    public void disable() { }

    /** Called every client tick (on or off). */
    public void tick(Minecraft mc) { }

    /** Draws the module at (x, y) and sets w and h. Only called while on. */
    public void render(GuiGraphicsExtractor g, Minecraft mc) { }

    // ---- shared drawing (colors come from HUD Style, or this module's own colors) ----
    public static final int GRAY = 0xFFAAAAAA, GREEN = 0xFF59F58C, RED = 0xFFF14A7B;

    public int text() { return own() ? ownText.rgb() : ClientModules.hudText.rgb(); }

    public int bg() {
        int a = (int) Math.round(ClientModules.hudOpacity.num / 100 * 255);
        return (a << 24) | ((own() ? ownBox.rgb() : ClientModules.hudBox.rgb()) & 0xFFFFFF);
    }

    private boolean own() { return ownColors != null && ownColors.bool; }

    public static boolean shadow() { return ClientModules.hudShadow.bool; }

    public static int accent() { return ClientModules.accent.rgb(); }

    /** A box with one line of text per entry. */
    protected void box(GuiGraphicsExtractor g, Minecraft mc, String... lines) {
        int width = 0;
        for (String s : lines) width = Math.max(width, mc.font.width(s));
        w = width + 8;
        h = lines.length * 10 + 4;
        g.fill(x, y, x + w, y + h, bg());
        for (int i = 0; i < lines.length; i++) g.text(mc.font, lines[i], x + 4, y + 3 + i * 10, text(), shadow());
    }

    public static String money(double v) {
        double a = Math.abs(v);
        String s;
        if (a >= 1e12) s = String.format(java.util.Locale.US, "%.2fT", a / 1e12);
        else if (a >= 1e9) s = String.format(java.util.Locale.US, "%.2fB", a / 1e9);
        else if (a >= 1e6) s = String.format(java.util.Locale.US, "%.2fM", a / 1e6);
        else if (a >= 1e3) s = String.format(java.util.Locale.US, "%.2fK", a / 1e3);
        else s = String.valueOf((long) a);
        return (v < 0 ? "-$" : "$") + s;
    }
}
