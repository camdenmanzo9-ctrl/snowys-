package com.snowy.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.util.List;

/** Particle Trails, Cape, Hit Color, Block Outline. Only you can see these. */
public final class CosmeticModules {
    private CosmeticModules() { }

    static Module trails, cape, hitColor, outline;

    /** True while a trail particle is being spawned, so Particle Changer doesn't hide it. */
    public static boolean spawningTrail;

    static void add(List<Module> list) {
        // ---------- Particle Trails ----------
        trails = new Module("Particle Trails", Module.Cat.COSMETIC, false, 0, 0) {
            final Setting particle = add(Setting.mode("Particle", 0, "Dust", "Flame", "Soul Flame", "Heart", "Snowflake",
                    "End Rod", "Cherry", "Totem", "Note", "Witch", "Glow", "Spark"));
            final Setting shape = add(Setting.mode("Shape", 0, "Feet", "Spiral", "Halo", "Helix"));
            final Setting color = add(Setting.color("Dust color", 0.61f, 0.6f));
            final Setting size = add(Setting.num("Dust size", 1, 0.5, 3, 0.5, ""));
            final Setting density = add(Setting.num("Density", 2, 1, 10, 1, ""));
            final Setting onlyMoving = add(Setting.bool("Only when moving", true));
            int t;

            @Override public void tick(Minecraft mc) {
                if (!on || mc.player == null || mc.level == null || mc.isPaused()) return;
                var p = mc.player;
                double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
                if (onlyMoving.bool && dx * dx + dz * dz < 0.0004) return;
                t++;
                ParticleOptions opt = options();
                spawningTrail = true;
                try {
                    for (int i = 0; i < density.i(); i++) {
                        double a = (t + i / (double) density.i()) * 0.35, px = p.getX(), py = p.getY(), pz = p.getZ();
                        switch (shape.mode) {
                            case 1 -> { px += Math.cos(a) * 0.6; pz += Math.sin(a) * 0.6; py += (t * 0.05 + i * 0.1) % 2; }   // spiral
                            case 2 -> { px += Math.cos(a * 2) * 0.35; pz += Math.sin(a * 2) * 0.35; py += 2.15; }             // halo
                            case 3 -> {                                                                                        // helix
                                double s = i % 2 == 0 ? 0 : Math.PI;
                                px += Math.cos(a + s) * 0.5; pz += Math.sin(a + s) * 0.5; py += (t * 0.08) % 2;
                            }
                            default -> { px += (Math.random() - 0.5) * 0.4; pz += (Math.random() - 0.5) * 0.4; py += 0.05; }  // feet
                        }
                        mc.level.addParticle(opt, px, py, pz, 0, 0.01, 0);
                    }
                } finally {
                    spawningTrail = false;
                }
            }

            ParticleOptions options() {
                return switch (particle.mode) {
                    case 1 -> ParticleTypes.FLAME;
                    case 2 -> ParticleTypes.SOUL_FIRE_FLAME;
                    case 3 -> ParticleTypes.HEART;
                    case 4 -> ParticleTypes.SNOWFLAKE;
                    case 5 -> ParticleTypes.END_ROD;
                    case 6 -> ParticleTypes.CHERRY_LEAVES;
                    case 7 -> ParticleTypes.TOTEM_OF_UNDYING;
                    case 8 -> ParticleTypes.NOTE;
                    case 9 -> ParticleTypes.WITCH;
                    case 10 -> ParticleTypes.GLOW;
                    case 11 -> ParticleTypes.ELECTRIC_SPARK;
                    default -> new DustParticleOptions(color.rgb() & 0xFFFFFF, (float) size.num);
                };
            }
        };
        list.add(trails);

        // ---------- Cape (drawn by mixin/AbstractClientPlayerMixin) ----------
        cape = new Module("Cape", Module.Cat.COSMETIC, false, 0, 0) {
            final Setting top = add(Setting.color("Top color", 0.61f, 0.35f));
            final Setting bottom = add(Setting.color("Bottom color", 0.75f, 0.2f));
            int lastTop, lastBottom;

            @Override public void tick(Minecraft mc) {
                if (!on) return;
                if (capeTex == null) {
                    capeTex = new DynamicTexture("Snowy's cape", 64, 32, true);
                    mc.getTextureManager().register(CAPE_ID, capeTex);
                    lastTop = lastBottom = 0;
                }
                int a = top.rgb(), b = bottom.rgb();
                if (a == lastTop && b == lastBottom) return;
                lastTop = a;
                lastBottom = b;
                NativeImage img = capeTex.getPixels();
                for (int y = 0; y < 32; y++) {
                    int c = mix(a, b, Math.min(1f, y / 16f));
                    for (int x = 0; x < 64; x++) img.setPixel(x, y, c);
                }
                capeTex.upload();
            }
        };
        list.add(cape);

        // ---------- Hit Color (recolors Minecraft's red hurt overlay) ----------
        hitColor = new Module("Hit Color", Module.Cat.COSMETIC, false, 0, 0) {
            final Setting color = add(Setting.color("Color", 0f, 0.5f));
            final Setting opacity = add(Setting.num("Opacity", 70, 10, 100, 5, "%"));
            int last;

            @Override public void tick(Minecraft mc) {
                if (!on) return;
                int a = (int) Math.round(opacity.num / 100 * 255);
                int c = (a << 24) | (color.rgb() & 0xFFFFFF);
                if (c != last && setHurtOverlay(mc, c)) last = c;
            }

            @Override public void disable() {
                setHurtOverlay(Minecraft.getInstance(), VANILLA_HURT);
                last = 0;
            }
        };
        list.add(hitColor);

        // ---------- Block Outline ----------
        outline = new Module("Block Outline", Module.Cat.COSMETIC, false, 0, 0) { };
        Setting oColor = outline.add(Setting.color("Color", 0.61f, 0.78f));
        Setting oWidth = outline.add(Setting.num("Width", 2, 1, 10, 0.5, ""));
        Setting oFill = outline.add(Setting.bool("Fill", false));
        Setting oFillColor = outline.add(Setting.color("Fill color", 0.61f, 0.78f));
        Setting oFillAlpha = outline.add(Setting.num("Fill opacity", 20, 5, 80, 5, "%"));
        list.add(outline);

        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((ctx, state) -> {
            if (!outline.on) return true;
            Minecraft mc = Minecraft.getInstance();
            Vec3 cam = mc.gameRenderer.getMainCamera().position();
            BlockPos pos = state.pos();
            double x = pos.getX() - cam.x, y = pos.getY() - cam.y, z = pos.getZ() - cam.z;
            PoseStack ps = ctx.poseStack();
            if (oFill.bool) {
                int fc = ((int) Math.round(oFillAlpha.num / 100 * 255) << 24) | (oFillColor.rgb() & 0xFFFFFF);
                VertexConsumer q = ctx.bufferSource().getBuffer(RenderTypes.debugQuads());
                for (AABB box : state.shape().toAabbs()) fillBox(ps.last().pose(), q, box.move(x, y, z).inflate(0.002), fc);
            }
            VertexConsumer lines = ctx.bufferSource().getBuffer(RenderTypes.lines());
            ShapeRenderer.renderShape(ps, lines, state.shape(), x, y, z, oColor.rgb(), (float) oWidth.num);
            ctx.bufferSource().endLastBatch();
            return false;   // skip the vanilla outline
        });
    }

    // ---------- cape texture ----------
    static final Identifier CAPE_ID = Identifier.fromNamespaceAndPath("snowys", "cape");
    static DynamicTexture capeTex;
    private static final ClientAsset.Texture CAPE_ASSET = new ClientAsset.DownloadedTexture(CAPE_ID, "");

    /** The cape to put on your skin, or null when the Cape module is off. */
    public static ClientAsset.Texture capeAsset() {
        return cape != null && cape.on && capeTex != null ? CAPE_ASSET : null;
    }

    // ---------- hurt overlay ----------
    private static final int VANILLA_HURT = 0xB2FF0000;

    private static boolean setHurtOverlay(Minecraft mc, int argb) {
        try {
            OverlayTexture ov = mc.gameRenderer.overlayTexture();
            Field f = OverlayTexture.class.getDeclaredField("texture");
            f.setAccessible(true);
            DynamicTexture tex = (DynamicTexture) f.get(ov);
            NativeImage img = tex.getPixels();
            for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setPixel(x, y, argb);
            tex.upload();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- helpers ----------
    private static void fillBox(Matrix4f m, VertexConsumer q, AABB b, int c) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ, x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        quad(m, q, c, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);   // bottom
        quad(m, q, c, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);   // top
        quad(m, q, c, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);   // north
        quad(m, q, c, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);   // south
        quad(m, q, c, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);   // west
        quad(m, q, c, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);   // east
    }

    private static void quad(Matrix4f m, VertexConsumer q, int c, float... v) {
        for (int i = 0; i < 12; i += 3) q.addVertex(m, v[i], v[i + 1], v[i + 2]).setColor(c);
    }

    static int mix(int a, int b, float f) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * f);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * f);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
