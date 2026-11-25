package com.snowy.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Balance, Rakeback and Payments panels. They only READ the files your other mods save:
 *  - config/balancehud.txt        (Balance HUD)
 *  - config/snowys-rake.txt       (Balance HUD, rakeback numbers)
 *  - config/bettors-dashboard.json (Snowy's Dashboard)
 * Nothing is ever written to those files.
 */
final class PanelModules {
    private PanelModules() { }

    private static final Path DIR = FabricLoader.getInstance().getConfigDir();

    /** Re-reads a file at most once a second, and only when it changed. */
    private static final class Watched {
        final Path path;
        long checkedAt, stamp = -1;
        List<String> lines;   // null = file missing

        Watched(String name) { path = DIR.resolve(name); }

        /** Returns true when new content was loaded. */
        boolean refresh() {
            long now = System.currentTimeMillis();
            if (now - checkedAt < 1000) return false;
            checkedAt = now;
            try {
                long m = Files.getLastModifiedTime(path).toMillis();
                if (m == stamp) return false;
                stamp = m;
                lines = Files.readAllLines(path);
            } catch (Exception e) {
                lines = null;
                stamp = -1;
            }
            return true;
        }
    }

    static void add(List<Module> list) {
        list.add(new Module("Balance", Module.Cat.PANELS, true, 120, 4) {
            final Watched file = new Watched("balancehud.txt");
            String[] text = {"Balance: ..."};

            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                if (file.refresh()) {
                    if (file.lines == null) {
                        text = new String[]{"Balance HUD data not found"};
                    } else {
                        double bal = -1, in = 0, out = 0, base = 0;
                        for (String line : file.lines) {
                            String[] p = line.split(",");
                            try {
                                switch (p[0]) {
                                    case "balance" -> bal = Double.parseDouble(p[1]);
                                    case "tot" -> { in = Double.parseDouble(p[1]); out = Double.parseDouble(p[2]); }
                                    case "base" -> base = Double.parseDouble(p[1]);
                                    default -> { }
                                }
                            } catch (Exception ignored) { }
                        }
                        double profit = in - out - base;
                        text = new String[]{
                                "Balance: " + (bal < 0 ? "type /bal" : money(bal)),
                                "Profit: " + (profit >= 0 ? "+" : "") + money(profit)};
                    }
                }
                box(g, mc, text);
            }
        });

        list.add(new Module("Rakeback", Module.Cat.PANELS, true, 120, 32) {
            final Watched file = new Watched("snowys-rake.txt");
            final Setting count = add(Setting.num("Players listed", 5, 1, 15, 1, ""));
            String[] text = {"Rakeback: ..."};
            List<String> all = new ArrayList<>();

            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                if (file.refresh()) {
                    all = new ArrayList<>();
                    if (file.lines == null) {
                        text = new String[]{"Rakeback data not found"};
                    } else {
                        double rate = 10, min = 1e9;
                        List<String[]> players = new ArrayList<>();
                        for (String line : file.lines) {
                            try {
                                if (line.startsWith("rate=")) rate = Double.parseDouble(line.substring(5));
                                else if (line.startsWith("min=")) min = Double.parseDouble(line.substring(4));
                                else if (line.startsWith("p,")) players.add(line.split(","));
                            } catch (Exception ignored) { }
                        }
                        text = new String[]{"Rakeback (" + trim(rate) + "% over " + money(min) + ")"};
                        for (String[] p : players) {
                            try {
                                double loss = Double.parseDouble(p[2]);
                                all.add(p[1] + ": " + (loss >= min ? "owed " + money(loss * rate / 100) : money(loss) + " / " + money(min)));
                            } catch (Exception ignored) { }
                        }
                        if (all.isEmpty()) all.add("No losses yet");
                    }
                }
                box(g, mc, join(text, all, count.i()));
            }
        });

        list.add(new Module("Payments", Module.Cat.PANELS, true, 120, 100) {
            final Watched file = new Watched("bettors-dashboard.json");
            final Setting count = add(Setting.num("Players listed", 5, 1, 15, 1, ""));
            String[] text = {"Payments: ..."};
            List<String> all = new ArrayList<>();
            boolean active;
            long start;
            String[] rows = new String[0];

            @Override public void render(GuiGraphicsExtractor g, Minecraft mc) {
                if (file.refresh()) read();
                if (file.lines == null) {
                    text = new String[]{"Snowy's Dashboard data not found"};
                } else {
                    String head = active ? "Session " + time(System.currentTimeMillis() - start) : "No session running";
                    text = new String[rows.length + 1];
                    text[0] = head;
                    System.arraycopy(rows, 0, text, 1, rows.length);
                }
                box(g, mc, file.lines == null ? text : join(text, all, count.i()));
            }

            void read() {
                if (file.lines == null) return;
                try {
                    JsonObject o = JsonParser.parseString(String.join("\n", file.lines)).getAsJsonObject();
                    active = o.has("sessionActive") && o.get("sessionActive").getAsBoolean();
                    start = o.has("sessionStart") ? o.get("sessionStart").getAsLong() : 0;
                    long profit = 0;
                    List<String> out = new ArrayList<>();
                    all = new ArrayList<>();
                    List<long[]> nets = new ArrayList<>();
                    List<String> names = new ArrayList<>();
                    if (o.has("session")) {
                        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("session").entrySet()) {
                            JsonObject p = e.getValue().getAsJsonObject();
                            long net = p.get("paidYou").getAsLong() - p.get("youPaid").getAsLong();   // + = you're up on them
                            profit += net;
                            names.add(e.getKey());
                            nets.add(new long[]{net, names.size() - 1});
                        }
                    }
                    out.add("Profit: " + (profit >= 0 ? "+" : "") + money(profit) + "  Players: " + names.size());
                    nets.sort((a, b) -> Long.compare(Math.abs(b[0]), Math.abs(a[0])));
                    for (int i = 0; i < nets.size(); i++) {
                        long net = nets.get(i)[0];
                        all.add(names.get((int) nets.get(i)[1]) + ": " + (net >= 0 ? "+" : "") + money(net));
                    }
                    rows = out.toArray(new String[0]);
                } catch (Exception e) {
                    rows = new String[]{"Could not read dashboard data"};
                    all = new ArrayList<>();
                }
            }
        });
    }

    /** First lines, then up to max entries of the list. */
    static String[] join(String[] head, List<String> list, int max) {
        List<String> out = new ArrayList<>(List.of(head));
        out.addAll(list.subList(0, Math.min(max, list.size())));
        return out.toArray(new String[0]);
    }

    static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    static String time(long ms) {
        long m = ms / 60000;
        return m >= 60 ? (m / 60) + "h " + (m % 60) + "m" : m + "m";
    }
}
