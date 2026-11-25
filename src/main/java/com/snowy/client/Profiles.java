package com.snowy.client;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Saved setups in config/snowys-profiles/<name>.txt (same format as config/snowys.txt). */
final class Profiles {
    private Profiles() { }

    static final String[] NAMES = {"Default", "PvP", "Streaming", "Custom 1", "Custom 2"};
    static int current = 0;

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("snowys-profiles");

    private static Path file(int i) { return DIR.resolve(NAMES[i] + ".txt"); }

    static boolean exists(int i) { return Files.exists(file(i)); }

    static boolean save(int i) {
        try {
            Files.createDirectories(DIR);
            Files.writeString(file(i), Modules.serialize());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean load(int i) {
        try {
            Modules.apply(Files.readAllLines(file(i)));
            Modules.save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
