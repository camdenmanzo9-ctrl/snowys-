package com.snowy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Snowy's client: Right Shift opens the ClickGUI. Never sends commands or chat. */
public class SnowysClient implements ClientModInitializer {
    static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("snowys", "main"));
    static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        // Default key: Right Shift. Change it in Options > Controls > Key Binds.
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.snowys.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));

        Modules.init();
        SnowysTitleScreen.register();

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("snowys", "hud"), (g, dt) -> Modules.renderHud(g));

        // Custom Crosshair: draw ours instead of the vanilla one while the module is on
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (g, dt) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!VisualModules.crosshair.on) vanilla.extractRenderState(g, dt);
            else if (mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui) VisualModules.drawCrosshair(g, mc);
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new ClickGui());
            }
            Modules.tick(mc);
        });
    }
}
