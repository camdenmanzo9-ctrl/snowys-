package com.snowy.client;

import net.minecraft.client.Minecraft;

import java.util.List;

/** Particle Changer, Camera Tweaks, Time & Weather. Everything here only changes what YOU see. */
public final class TweakModules {
    private TweakModules() { }

    static Module particles, camera, timeWeather;
    public static Setting critMult, explosions, others;
    static Setting hurtCam, bobbing, lowFire;
    static Setting time, clearWeather;

    static void add(List<Module> list) {
        // read by mixin/ParticleEngineMixin
        particles = new Module("Particle Changer", Module.Cat.VISUAL, false, 0, 0) { };
        critMult = particles.add(Setting.num("Crit particles", 1, 0, 5, 1, "x"));
        explosions = particles.add(Setting.bool("Explosions", true));
        others = particles.add(Setting.bool("Other particles", true));
        list.add(particles);

        camera = new Module("Camera Tweaks", Module.Cat.VISUAL, false, 0, 0) {
            boolean applied;
            double oldTilt;

            @Override public void tick(Minecraft mc) {
                if (!on) return;
                if (!applied) {
                    oldTilt = mc.options.damageTiltStrength().get();
                    applied = true;
                }
                VisualModules.force(mc.options.damageTiltStrength(), hurtCam.num / 100);
            }

            @Override public void disable() {
                if (!applied) return;
                VisualModules.force(Minecraft.getInstance().options.damageTiltStrength(), oldTilt);
                applied = false;
            }
        };
        hurtCam = camera.add(Setting.num("Hurt cam shake", 0, 0, 100, 5, "%"));
        bobbing = camera.add(Setting.num("View bobbing", 50, 0, 100, 5, "%"));
        lowFire = camera.add(Setting.bool("Low fire", true));
        list.add(camera);

        timeWeather = new Module("Time & Weather", Module.Cat.VISUAL, false, 0, 0) {
            @Override public void tick(Minecraft mc) {
                if (on && clearWeather.bool && mc.level != null) {
                    mc.level.setRainLevel(0);
                    mc.level.setThunderLevel(0);
                }
            }
        };
        time = timeWeather.add(Setting.num("Time", 6000, 0, 23500, 500, ""));
        clearWeather = timeWeather.add(Setting.bool("Clear weather", true));
        list.add(timeWeather);
    }

    // ---- used by mixins ----

    public static boolean particlesOn() { return particles != null && particles.on; }

    /** 1 = normal view bobbing, 0 = none. */
    public static float bobFactor() { return camera != null && camera.on ? (float) (bobbing.num / 100) : 1f; }

    public static boolean lowFire() { return camera != null && camera.on && lowFire.bool; }

    /** Chosen time of day (0..23999), or -1 when Time & Weather is off. */
    public static long fixedTime() { return timeWeather != null && timeWeather.on ? (long) time.num : -1; }
}
