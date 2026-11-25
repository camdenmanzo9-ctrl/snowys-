package com.snowy.client;

import java.util.Locale;

/** One option of a module: on/off, number slider, mode list, or color (hue + shade sliders). */
public class Setting {
    public enum Type { BOOL, NUM, MODE, COLOR }

    public final String name;
    public final Type type;
    public boolean bool;
    public double num, min, max, step;
    public String unit = "";
    public String[] modes;
    public int mode;
    public float hue, shade;   // COLOR: shade 0 = black, 0.5 = full color, 1 = white
    public boolean chroma;     // COLOR: hue cycles through the rainbow

    private Setting(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public static Setting bool(String name, boolean def) {
        Setting s = new Setting(name, Type.BOOL);
        s.bool = def;
        return s;
    }

    public static Setting num(String name, double def, double min, double max, double step, String unit) {
        Setting s = new Setting(name, Type.NUM);
        s.num = def;
        s.min = min;
        s.max = max;
        s.step = step;
        s.unit = unit;
        return s;
    }

    public static Setting mode(String name, int def, String... modes) {
        Setting s = new Setting(name, Type.MODE);
        s.modes = modes;
        s.mode = def;
        return s;
    }

    public static Setting color(String name, float hue, float shade) {
        Setting s = new Setting(name, Type.COLOR);
        s.hue = hue;
        s.shade = shade;
        return s;
    }

    public int i() { return (int) Math.round(num); }

    public String modeName() { return modes[mode]; }

    /** Sets a slider from 0..1 (snapped to step). */
    public void setFraction(double f) {
        f = Math.max(0, Math.min(1, f));
        num = Math.max(min, Math.min(max, Math.round((min + f * (max - min)) / step) * step));
    }

    public double fraction() { return (num - min) / (max - min); }

    /** Opaque color, 0xFFRRGGBB. */
    public int rgb() { return color(chroma ? (System.currentTimeMillis() % 4000) / 4000f : hue, shade); }

    public static int color(float hue, float shade) {
        // full-saturation hue
        float h = (hue - (float) Math.floor(hue)) * 6f, x = 1 - Math.abs(h % 2 - 1);
        float[] rgb = h < 1 ? new float[]{1, x, 0} : h < 2 ? new float[]{x, 1, 0} : h < 3 ? new float[]{0, 1, x}
                : h < 4 ? new float[]{0, x, 1} : h < 5 ? new float[]{x, 0, 1} : new float[]{1, 0, x};
        int r = (int) (rgb[0] * 255), g = (int) (rgb[1] * 255), b = (int) (rgb[2] * 255);
        if (shade <= 0.5f) {
            float f = shade * 2;
            r = (int) (r * f); g = (int) (g * f); b = (int) (b * f);
        } else {
            float f = (shade - 0.5f) * 2;
            r += (int) ((255 - r) * f); g += (int) ((255 - g) * f); b += (int) ((255 - b) * f);
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Text shown in the ClickGUI row. */
    public String label() {
        return switch (type) {
            case BOOL -> name + ": " + (bool ? "On" : "Off");
            case NUM -> name + ": " + (step >= 1 ? String.valueOf(i()) : String.format(Locale.US, "%.1f", num)) + unit;
            case MODE -> name + ": " + modeName();
            case COLOR -> name + (chroma ? " (Rainbow)" : "");
        };
    }

    String save() {
        return switch (type) {
            case BOOL -> bool ? "1" : "0";
            case NUM -> String.valueOf(num);
            case MODE -> String.valueOf(mode);
            case COLOR -> hue + ";" + shade + ";" + (chroma ? 1 : 0);
        };
    }

    void load(String v) {
        try {
            switch (type) {
                case BOOL -> bool = v.equals("1");
                case NUM -> num = Math.max(min, Math.min(max, Double.parseDouble(v)));
                case MODE -> mode = Math.max(0, Math.min(modes.length - 1, Integer.parseInt(v)));
                case COLOR -> {
                    String[] p = v.split(";");
                    hue = Float.parseFloat(p[0]);
                    shade = Float.parseFloat(p[1]);
                    chroma = p.length > 2 && p[2].equals("1");
                }
            }
        } catch (Exception ignored) { }
    }
}
