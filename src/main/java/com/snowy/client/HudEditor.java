package com.snowy.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag the HUD modules that are turned on to wherever you want them. */
public class HudEditor extends Screen {
    private final Screen parent;
    private Module dragging;
    private int offX, offY;

    public HudEditor(Screen parent) {
        super(Component.literal("Edit HUD layout"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width / 2 - 40, height - 28, 80, 20).build());
    }

    @Override
    public void onClose() {
        Modules.save();
        minecraft.setScreen(parent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x30000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        if (minecraft.player == null) {
            String t = "Join a world to move HUD modules";
            g.text(font, t, (width - font.width(t)) / 2, height / 2, 0xFFFFFFFF, true);
            return;
        }
        Modules.renderAll(g, minecraft);
        for (Module m : Modules.ALL) {
            if (!m.on || !m.hud) continue;
            int c = m == dragging || ClickGui.in(mouseX, mouseY, m.x, m.y, m.sw(), m.sh()) ? Module.accent() : 0x80FFFFFF;
            g.fill(m.x - 1, m.y - 1, m.x + m.sw() + 1, m.y, c);
            g.fill(m.x - 1, m.y + m.sh(), m.x + m.sw() + 1, m.y + m.sh() + 1, c);
            g.fill(m.x - 1, m.y, m.x, m.y + m.sh(), c);
            g.fill(m.x + m.sw(), m.y, m.x + m.sw() + 1, m.y + m.sh(), c);
        }
        String t = "Drag the boxes, then press Done";
        g.text(font, t, (width - font.width(t)) / 2, 6, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (super.mouseClicked(e, doubleClick)) return true;
        if (e.button() != 0) return false;
        int mx = (int) e.x(), my = (int) e.y();
        for (int i = Modules.ALL.size() - 1; i >= 0; i--) {   // topmost first
            Module m = Modules.ALL.get(i);
            if (m.on && m.hud && ClickGui.in(mx, my, m.x, m.y, m.sw(), m.sh())) {
                dragging = m;
                offX = mx - m.x;
                offY = my - m.y;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(e, dx, dy);
        dragging.x = (int) e.x() - offX;
        dragging.y = (int) e.y() - offY;
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (dragging != null) {
            dragging = null;
            Modules.save();
            return true;
        }
        return super.mouseReleased(e);
    }
}
