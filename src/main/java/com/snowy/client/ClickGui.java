package com.snowy.client;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Classic ClickGUI: one column per category. Drag a column by its title.
 * Left-click a module to turn it on/off, right-click it to open its settings.
 * (CLIENT modules only hold settings, so any click opens them.)
 */
public class ClickGui extends Screen {
    private static final int COL_W = 110, HEAD_H = 14, ROW_H = 12, BAR_H = 8;
    private static final int HEAD = 0xF0181C28, ROW = 0xD0101018, SET_BG = 0xE008080E, HOVER = 0x30FFFFFF;

    private enum Kind { HEAD, MOD, SET, COLOR_LABEL, COLOR_BAR }

    private record Row(Kind kind, int col, Module m, Setting s, int x, int y, int w, int h) { }

    private int dragCol = -1, offX, offY;
    private Setting dragSet;
    private int dragPart, barX, barW;   // part: 0 slider, 1 hue, 2 shade

    public ClickGui() {
        super(Component.literal("Snowy's"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private String status = "";
    private long statusAt;

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Edit HUD layout"), b -> minecraft.setScreen(new HudEditor(this)))
                .bounds(width / 2 - 60, height - 28, 120, 20).build());

        // profile bar (bottom left)
        addRenderableWidget(Button.builder(Component.literal("Profile: " + Profiles.NAMES[Profiles.current]), b -> {
            Profiles.current = (Profiles.current + 1) % Profiles.NAMES.length;
            rebuildWidgets();
        }).bounds(4, height - 28, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), b ->
                say(Profiles.save(Profiles.current) ? "Saved " + Profiles.NAMES[Profiles.current] : "Could not save")
        ).bounds(118, height - 28, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Load"), b -> {
            if (!Profiles.exists(Profiles.current)) say(Profiles.NAMES[Profiles.current] + " is empty - press Save first");
            else say(Profiles.load(Profiles.current) ? "Loaded " + Profiles.NAMES[Profiles.current] : "Could not load");
        }).bounds(162, height - 28, 40, 20).build());
    }

    private void say(String s) {
        status = s;
        statusAt = System.currentTimeMillis();
    }

    @Override
    public void onClose() {
        Modules.save();
        super.onClose();
    }

    private List<Row> rows() {
        List<Row> out = new ArrayList<>();
        Module.Cat[] cats = Module.Cat.values();
        for (int i = 0; i < cats.length; i++) {
            int x = Modules.COLS[i][0], y = Modules.COLS[i][1];
            out.add(new Row(Kind.HEAD, i, null, null, x, y, COL_W, HEAD_H));
            y += HEAD_H;
            for (Module m : Modules.ALL) {
                if (m.cat != cats[i]) continue;
                out.add(new Row(Kind.MOD, i, m, null, x, y, COL_W, ROW_H));
                y += ROW_H;
                if (!m.expanded) continue;
                for (Setting s : m.settings) {
                    if (s.type == Setting.Type.COLOR) {
                        out.add(new Row(Kind.COLOR_LABEL, i, m, s, x, y, COL_W, ROW_H));
                        y += ROW_H;
                        out.add(new Row(Kind.COLOR_BAR, i, m, s, x, y, COL_W, BAR_H + 3));
                        y += BAR_H + 3;
                    } else {
                        out.add(new Row(Kind.SET, i, m, s, x, y, COL_W, ROW_H));
                        y += ROW_H;
                    }
                }
            }
        }
        return out;
    }

    // hue bar takes 60% of the bar row, shade bar the rest
    private static int hueX(Row r) { return r.x + 8; }
    private static int hueW(Row r) { return (r.w - 20) * 6 / 10; }
    private static int shadeX(Row r) { return hueX(r) + hueW(r) + 4; }
    private static int shadeW(Row r) { return r.w - 20 - hueW(r); }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x60000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int accent = Module.accent();
        Module.Cat[] cats = Module.Cat.values();
        for (Row r : rows()) {
            boolean hover = in(mouseX, mouseY, r.x, r.y, r.w, r.h);
            switch (r.kind) {
                case HEAD -> {
                    g.fill(r.x, r.y, r.x + r.w, r.y + r.h, HEAD);
                    g.fill(r.x, r.y + r.h - 1, r.x + r.w, r.y + r.h, accent);
                    g.text(font, cats[r.col].name(), r.x + 4, r.y + 3, accent, true);
                }
                case MOD -> {
                    boolean lit = r.m.on && r.m.cat != Module.Cat.CLIENT;
                    g.fill(r.x, r.y, r.x + r.w, r.y + r.h, lit ? (0x80000000 | (accent & 0xFFFFFF)) : ROW);
                    if (hover) g.fill(r.x, r.y, r.x + r.w, r.y + r.h, HOVER);
                    g.text(font, r.m.name, r.x + 4, r.y + 2, lit || r.m.cat == Module.Cat.CLIENT ? 0xFFFFFFFF : Module.GRAY, true);
                    if (!r.m.settings.isEmpty()) {
                        String arrow = r.m.expanded ? "-" : "+";
                        g.text(font, arrow, r.x + r.w - 4 - font.width(arrow), r.y + 2, Module.GRAY, true);
                    }
                }
                case SET -> {
                    g.fill(r.x, r.y, r.x + r.w, r.y + r.h, SET_BG);
                    if (r.s.type == Setting.Type.NUM) {
                        int fw = (int) ((r.w - 8) * r.s.fraction());
                        g.fill(r.x + 4, r.y + 1, r.x + 4 + fw, r.y + r.h - 1, 0x60000000 | (accent & 0xFFFFFF));
                    }
                    if (hover) g.fill(r.x, r.y, r.x + r.w, r.y + r.h, HOVER);
                    boolean bright = r.s.type != Setting.Type.BOOL || r.s.bool;
                    g.text(font, r.s.label(), r.x + 8, r.y + 2, bright ? 0xFFFFFFFF : Module.GRAY, true);
                }
                case COLOR_LABEL -> {
                    g.fill(r.x, r.y, r.x + r.w, r.y + r.h, SET_BG);
                    g.text(font, r.s.label(), r.x + 8, r.y + 2, 0xFFFFFFFF, true);
                    int sx = r.x + r.w - 14;
                    g.fill(sx - 1, r.y + 1, sx + 9, r.y + 11, 0xFF000000);
                    g.fill(sx, r.y + 2, sx + 8, r.y + 10, r.s.rgb());
                }
                case COLOR_BAR -> {
                    g.fill(r.x, r.y, r.x + r.w, r.y + r.h, SET_BG);
                    int hx = hueX(r), hw = hueW(r), sx = shadeX(r), sw = shadeW(r), by = r.y + 1;
                    for (int i = 0; i < hw; i++) g.fill(hx + i, by, hx + i + 1, by + BAR_H, Setting.color((float) i / hw, 0.5f));
                    for (int i = 0; i < sw; i++) g.fill(sx + i, by, sx + i + 1, by + BAR_H, Setting.color(r.s.hue, (float) i / (sw - 1)));
                    marker(g, hx + (int) (r.s.hue * (hw - 1)), by);
                    marker(g, sx + (int) (r.s.shade * (sw - 1)), by);
                }
            }
        }
        String hint = "Left click: on/off   Right click: settings / rainbow   Right Shift: close";
        g.text(font, hint, width - font.width(hint) - 4, height - 12, Module.GRAY, true);
        if (System.currentTimeMillis() - statusAt < 3000) g.text(font, status, 4, height - 40, 0xFFFFFFFF, true);
    }

    private static void marker(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 2, y + BAR_H + 1, 0xFF000000);
        g.fill(x, y - 1, x + 1, y + BAR_H + 1, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
        if (e.key() == KeyMappingHelper.getBoundKeyOf(SnowysClient.openKey).getValue()) {
            onClose();
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (super.mouseClicked(e, doubleClick)) return true;
        int mx = (int) e.x(), my = (int) e.y(), btn = e.button();
        if (btn != 0 && btn != 1) return false;
        for (Row r : rows()) {
            if (!in(mx, my, r.x, r.y, r.w, r.h)) continue;
            switch (r.kind) {
                case HEAD -> {
                    if (btn == 0) {
                        dragCol = r.col;
                        offX = mx - r.x;
                        offY = my - r.y;
                    }
                }
                case MOD -> {
                    if (btn == 1 || r.m.cat == Module.Cat.CLIENT) r.m.expanded = !r.m.expanded && !r.m.settings.isEmpty();
                    else r.m.toggle();
                }
                case SET -> {
                    switch (r.s.type) {
                        case BOOL -> r.s.bool = !r.s.bool;
                        case MODE -> r.s.mode = Math.floorMod(r.s.mode + (btn == 0 ? 1 : -1), r.s.modes.length);
                        case NUM -> startDrag(r.s, 0, r.x + 4, r.w - 8, mx);
                        default -> { }
                    }
                    Modules.save();
                }
                case COLOR_LABEL -> {
                    if (btn == 1) {
                        r.s.chroma = !r.s.chroma;
                        Modules.save();
                    }
                }
                case COLOR_BAR -> {
                    if (mx < shadeX(r) - 2) startDrag(r.s, 1, hueX(r), hueW(r), mx);
                    else startDrag(r.s, 2, shadeX(r), shadeW(r), mx);
                }
                default -> { }
            }
            return true;
        }
        return false;
    }

    private void startDrag(Setting s, int part, int x, int w, int mx) {
        dragSet = s;
        dragPart = part;
        barX = x;
        barW = w;
        applyDrag(mx);
    }

    private void applyDrag(int mx) {
        float f = Math.max(0f, Math.min(1f, (float) (mx - barX) / Math.max(1, barW - 1)));
        switch (dragPart) {
            case 0 -> dragSet.setFraction(f);
            case 1 -> dragSet.hue = f;
            default -> dragSet.shade = f;
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragSet != null) {
            applyDrag((int) e.x());
            return true;
        }
        if (dragCol < 0) return super.mouseDragged(e, dx, dy);
        Modules.COLS[dragCol][0] = Math.max(0, Math.min(width - COL_W, (int) e.x() - offX));
        Modules.COLS[dragCol][1] = Math.max(0, Math.min(height - HEAD_H, (int) e.y() - offY));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (dragCol >= 0 || dragSet != null) {
            dragCol = -1;
            dragSet = null;
            Modules.save();
            return true;
        }
        return super.mouseReleased(e);
    }

    static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
