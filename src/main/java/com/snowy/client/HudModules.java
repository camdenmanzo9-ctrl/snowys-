package com.snowy.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** FPS, CPS, Keystrokes, Coords, Armor Status, Ping, Potion Effects, Server & Time, Compass, Item Counter. */
final class HudModules {
    private HudModules() { }

    static void add(List<Module> list) {
        list.add(new Module("FPS", Module.Cat.HUD, true, 4, 4) {
            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                box(g, mc, "FPS: " + mc.getFps());
            }
        });

        list.add(new Module("CPS", Module.Cat.HUD, true, 4, 20) {
            final ArrayDeque<Long> left = new ArrayDeque<>(), right = new ArrayDeque<>();
            final Setting showRight = add(Setting.bool("Show right click", true));
            boolean wasL, wasR;

            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                long now = System.currentTimeMillis();
                boolean l = mc.options.keyAttack.isDown(), r = mc.options.keyUse.isDown();
                if (l && !wasL) left.add(now);
                if (r && !wasR) right.add(now);
                wasL = l;
                wasR = r;
                while (!left.isEmpty() && now - left.peek() > 1000) left.poll();
                while (!right.isEmpty() && now - right.peek() > 1000) right.poll();
                box(g, mc, "CPS: " + left.size() + (showRight.bool ? " | " + right.size() : ""));
            }
        });

        list.add(new Module("Keystrokes", Module.Cat.HUD, true, 4, 36) {
            final Setting mouse = add(Setting.bool("Show mouse buttons", true));
            final Setting space = add(Setting.bool("Show space bar", true));

            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                int s = 18, gap = 2, full = s * 3 + gap * 2;
                key(g, mc, mc.options.keyUp, "W", x + s + gap, y, s, s);
                key(g, mc, mc.options.keyLeft, "A", x, y + s + gap, s, s);
                key(g, mc, mc.options.keyDown, "S", x + s + gap, y + s + gap, s, s);
                key(g, mc, mc.options.keyRight, "D", x + (s + gap) * 2, y + s + gap, s, s);
                int half = (full - gap) / 2, ry = y + (s + gap) * 2;
                if (mouse.bool) {
                    key(g, mc, mc.options.keyAttack, "LMB", x, ry, half, s);
                    key(g, mc, mc.options.keyUse, "RMB", x + half + gap, ry, half, s);
                    ry += s + gap;
                }
                if (space.bool) {
                    key(g, mc, mc.options.keyJump, "___", x, ry, full, 10);
                    ry += 10 + gap;
                }
                w = full;
                h = ry - y - gap;
            }

            void key(GuiGraphicsExtractor g, Minecraft mc, KeyMapping k, String label, int kx, int ky, int kw, int kh) {
                boolean down = k.isDown();
                g.fill(kx, ky, kx + kw, ky + kh, down ? 0xC0FFFFFF : bg());
                int tx = kx + (kw - mc.font.width(label)) / 2, ty = ky + (kh - 8) / 2;
                g.text(mc.font, label, tx, ty, down ? 0xFF000000 : text(), !down && shadow());
            }
        });

        list.add(new Module("Coords", Module.Cat.HUD, true, 4, 112) {
            final Setting facing = add(Setting.bool("Show facing", true));

            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                var p = mc.player;
                String xyz = String.format("XYZ: %d %d %d", (int) Math.floor(p.getX()), (int) Math.floor(p.getY()), (int) Math.floor(p.getZ()));
                if (facing.bool) box(g, mc, xyz, "Facing: " + p.getDirection().getName());
                else box(g, mc, xyz);
            }
        });

        list.add(new Module("Armor Status", Module.Cat.HUD, true, 4, 140) {
            final EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                int width = 16;
                for (int i = 0; i < slots.length; i++) {
                    ItemStack st = mc.player.getItemBySlot(slots[i]);
                    int ry = y + i * 17;
                    if (st.isEmpty()) continue;
                    g.item(st, x, ry);
                    if (st.isDamageableItem()) {
                        int left = st.getMaxDamage() - st.getDamageValue();
                        String t = String.valueOf(left);
                        int color = left * 4 < st.getMaxDamage() ? RED : text();
                        g.text(mc.font, t, x + 19, ry + 4, color, shadow());
                        width = Math.max(width, 19 + mc.font.width(t));
                    }
                }
                w = width;
                h = slots.length * 17;
            }
        });

        list.add(new Module("Ping", Module.Cat.HUD, true, 4, 212) {
            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                var conn = mc.getConnection();
                var info = conn == null ? null : conn.getPlayerInfo(mc.player.getUUID());
                box(g, mc, "Ping: " + (info == null ? "?" : info.getLatency() + "ms"));
            }
        });

        list.add(new Module("Potion Effects", Module.Cat.HUD, true, 4, 230) {
            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                List<String> lines = new ArrayList<>();
                for (MobEffectInstance e : mc.player.getActiveEffects()) {
                    String name = e.getEffect().value().getDisplayName().getString();
                    if (e.getAmplifier() > 0) name += " " + roman(e.getAmplifier() + 1);
                    String time = e.isInfiniteDuration() ? "**:**" : String.format("%d:%02d", e.getDuration() / 1200, e.getDuration() / 20 % 60);
                    lines.add(name + "  " + time);
                }
                if (lines.isEmpty()) lines.add("No effects");
                box(g, mc, lines.toArray(new String[0]));
            }
        });

        list.add(new Module("Server & Time", Module.Cat.HUD, true, 4, 250) {
            final Setting server = add(Setting.bool("Show server", true));
            final Setting clock = add(Setting.bool("Show clock", true));
            final Setting h24 = add(Setting.bool("24-hour clock", false));
            final Setting session = add(Setting.bool("Show session time", true));
            long joined;

            @Override public void tick(Minecraft mc) {
                if (mc.player == null) joined = 0;
                else if (joined == 0) joined = System.currentTimeMillis();
            }

            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                List<String> lines = new ArrayList<>();
                if (server.bool) {
                    var sd = mc.getCurrentServer();
                    lines.add(sd != null ? sd.ip : mc.hasSingleplayerServer() ? "Singleplayer" : "?");
                }
                if (clock.bool) lines.add(LocalTime.now().format(DateTimeFormatter.ofPattern(h24.bool ? "HH:mm" : "h:mm a")));
                if (session.bool && joined > 0) {
                    long sec = (System.currentTimeMillis() - joined) / 1000;
                    lines.add("Session " + String.format("%d:%02d:%02d", sec / 3600, sec / 60 % 60, sec % 60));
                }
                if (lines.isEmpty()) lines.add("Server & Time");
                box(g, mc, lines.toArray(new String[0]));
            }
        });

        list.add(new Module("Compass", Module.Cat.HUD, true, 200, 4) {
            final String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};   // yaw 0 = south

            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                w = 140;
                h = 14;
                int cx = x + w / 2;
                float yaw = mc.player.getYRot();
                g.fill(x, y, x + w, y + h, bg());
                for (int i = 0; i < 8; i++) {
                    float off = wrap(i * 45 - yaw);
                    if (Math.abs(off) > 65) continue;
                    int px = cx + Math.round(off);
                    String n = names[i];
                    g.text(mc.font, n, px - mc.font.width(n) / 2, y + 3, n.length() == 1 ? text() : GRAY, shadow());
                }
                g.fill(cx, y + h - 2, cx + 1, y + h, accent());
            }

            float wrap(float a) {
                a %= 360;
                if (a >= 180) a -= 360;
                if (a < -180) a += 360;
                return a;
            }
        });

        list.add(new Module("Item Counter", Module.Cat.HUD, true, 4, 270) {
            final Item[] items = {Items.TOTEM_OF_UNDYING, Items.ENDER_PEARL, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
                    Items.ARROW, Items.FIREWORK_ROCKET, Items.EXPERIENCE_BOTTLE, Items.END_CRYSTAL, Items.OBSIDIAN, Items.COBWEB};
            final Setting item = add(Setting.mode("Item", 0, "Totems", "Ender Pearls", "Golden Apples", "God Apples",
                    "Arrows", "Fireworks", "XP Bottles", "End Crystals", "Obsidian", "Cobwebs"));

            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                Item it = items[item.mode];
                var inv = mc.player.getInventory();
                int count = 0;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack st = inv.getItem(i);
                    if (st.is(it)) count += st.getCount();
                }
                String t = String.valueOf(count);
                w = 22 + mc.font.width(t);
                h = 18;
                g.fill(x, y, x + w, y + h, bg());
                g.item(new ItemStack(it), x + 1, y + 1);
                g.text(mc.font, t, x + 19, y + 5, count == 0 ? RED : text(), shadow());
            }
        });
    }

    static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n < r.length ? r[n] : String.valueOf(n);
    }
}
