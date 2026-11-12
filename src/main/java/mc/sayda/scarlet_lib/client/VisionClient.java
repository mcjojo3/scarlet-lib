package mc.sayda.scarlet_lib.client;

import mc.sayda.scarlet_lib.effect.Effects;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

/**
 * The client side of the vision effects. Night vision lifts every brightness the renderer reads
 * (terrain brightness is baked into chunk meshes, so they are rebuilt when it starts or ends).
 * Darkness closes pulsing black fog in around the camera, blindness a steady dense one.
 */
public final class VisionClient {
    private static boolean nightVision;
    private static World lastWorld;

    private VisionClient() {
    }

    /** The brightness to draw with: night vision raises it most of the way to full. */
    public static float lift(float brightness) {
        return nightVision ? 1F - (1F - brightness) * 0.2F : brightness;
    }

    /**
     * How far the fog closes in: 0 = not at all, 1 = fully. Darkness pulses (one pulse every four
     * seconds, as 1.21's); blindness holds steady.
     */
    public static float fogDarkness(Minecraft mc, float tickDelta) {
        if (mc.camera == null || mc.world == null) return 0F;
        if (Effects.has(mc.camera, "minecraft:blindness")) return 0.9F;
        if (!Effects.has(mc.camera, "minecraft:darkness")) return 0F;
        float t = (mc.world.getTime() % 400L + tickDelta) / 20F;
        return 0.8F + 0.2F * (float) Math.sin(t * Math.PI / 2);
    }

    static void tick(Minecraft mc) {
        World world = mc.world;
        if (world != lastWorld) {
            BodySize.clearRemote();
            lastWorld = world;
        }
        boolean nv = world != null && mc.player != null && Effects.has(mc.player, "minecraft:night_vision");
        if (nv != nightVision) {
            nightVision = nv;
            if (mc.worldRenderer != null && world != null) mc.worldRenderer.reload(); // rebake terrain brightness
        }
    }
}
