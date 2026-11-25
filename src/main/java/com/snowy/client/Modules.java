package com.snowy.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** The module list, plus saving on/off, positions and ClickGUI column spots to config/snowys.txt. */
public final class Modules {
    private Modules() { }

    public static final List<Module> ALL = new ArrayList<>();
    /** ClickGUI column positions: HUD, VISUAL, COSMETIC, PANELS, CLIENT. */
    public static final int[][] COLS = {{10, 10}, {125, 10}, {240, 10}, {355, 10}, {470, 10}};

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("snowys.txt");

    public static void init() {
        HudModules.add(ALL);
        VisualModules.add(ALL);
        TweakModules.add(ALL);
        CosmeticModules.add(ALL);
        PanelModules.add(ALL);
        ClientModules.add(ALL);
        for (Module m : ALL) if (m.hud) m.addHudSettings();
        try { if (Files.exists(FILE)) apply(Files.readAllLines(FILE)); } catch (Exception ignored) { }
    }

    public static Module get(String name) {
        for (Module m : ALL) if (m.name.equals(name)) return m;
        return null;
    }

    public static void tick(Minecraft mc) {
        for (Module m : ALL) m.tick(mc);
    }

    /** Draws every HUD module that is on. */
    public static void renderHud(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.screen instanceof HudEditor) return;   // the editor draws them itself
        renderAll(g, mc);
    }

    static void renderAll(GuiGraphicsExtractor g, Minecraft mc) {
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        for (Module m : ALL) {
            if (!m.on || !m.hud) continue;
            m.x = Math.max(0, Math.min(sw - m.sw(), m.x));   // keep on screen
            m.y = Math.max(0, Math.min(sh - m.sh(), m.y));
            float s = (float) (m.scale.num / 100);
            g.pose().pushMatrix();
            g.pose().translate(m.x, m.y);
            g.pose().scale(s, s);
            g.pose().translate(-m.x, -m.y);
            m.render(g, mc);
            g.pose().popMatrix();
        }
    }

    public static void save() {
        try { Files.writeString(FILE, serialize()); } catch (Exception ignored) { }
    }

    /** Everything (modules, settings, column spots) as text. Also used by Profiles. */
    static String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Module m : ALL) {
            sb.append("m,").append(m.name).append(',').append(m.on ? 1 : 0).append(',').append(m.x).append(',').append(m.y).append('\n');
            for (Setting st : m.settings) sb.append("s,").append(m.name).append(',').append(st.name).append(',').append(st.save()).append('\n');
        }
        for (int i = 0; i < COLS.length; i++) sb.append("col,").append(i).append(',').append(COLS[i][0]).append(',').append(COLS[i][1]).append('\n');
        return sb.toString();
    }

    /** Loads saved text; modules that end up off get their disable() call. */
    static void apply(List<String> lines) {
        boolean[] was = new boolean[ALL.size()];
        for (int i = 0; i < was.length; i++) was[i] = ALL.get(i).on;
        try {
            for (String line : lines) {
                try {
                    String[] p = line.split(",");
                    if (p[0].equals("m")) {
                        Module m = get(p[1]);
                        if (m == null) continue;
                        m.on = p[2].equals("1");
                        m.x = Integer.parseInt(p[3]);
                        m.y = Integer.parseInt(p[4]);
                    } else if (p[0].equals("s")) {
                        Module m = get(p[1]);
                        if (m == null) continue;
                        for (Setting st : m.settings) if (st.name.equals(p[2])) st.load(p[3]);
                    } else if (p[0].equals("col")) {
                        int i = Integer.parseInt(p[1]);
                        COLS[i][0] = Integer.parseInt(p[2]);
                        COLS[i][1] = Integer.parseInt(p[3]);
                    }
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        for (int i = 0; i < was.length; i++) if (was[i] && !ALL.get(i).on) ALL.get(i).disable();
    }
}
