package com.snowy.client;

import java.util.List;

/** CLIENT column: Title Screen, HUD Style, ClickGUI. These only hold settings (click to open them). */
final class ClientModules {
    private ClientModules() { }

    static Setting background, gradTop, gradBottom, buttonColor;
    static Setting hudText, hudBox, hudOpacity, hudShadow;
    static Setting accent;

    static void add(List<Module> list) {
        Module title = new Module("Title Screen", Module.Cat.CLIENT, false, 0, 0) { };
        background = title.add(Setting.mode("Background", 0, "Snowfall", "Gradient", "Vanilla"));
        gradTop = title.add(Setting.color("Top color", 0.61f, 0.10f));
        gradBottom = title.add(Setting.color("Bottom color", 0.61f, 0.22f));
        buttonColor = title.add(Setting.color("Button color", 0.61f, 0.08f));
        list.add(title);

        Module style = new Module("HUD Style", Module.Cat.CLIENT, false, 0, 0) { };
        hudText = style.add(Setting.color("Text color", 0f, 1f));
        hudBox = style.add(Setting.color("Box color", 0f, 0f));
        hudOpacity = style.add(Setting.num("Box opacity", 56, 0, 100, 1, "%"));
        hudShadow = style.add(Setting.bool("Text shadow", true));
        list.add(style);

        Module gui = new Module("ClickGUI", Module.Cat.CLIENT, false, 0, 0) { };
        accent = gui.add(Setting.color("Accent color", 0.61f, 0.78f));
        list.add(gui);
    }
}
