package mc.sayda.scarlet_lib.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * StationAPI's effect list background stretches the middle of a box from /achievement/bg.png in
 * one piece, but that middle strip is only 133 pixels wide in the texture: a panel for an effect
 * with a long name or description ran past it and picked up the light achievement frame beside
 * it (a white block at the right end). The same drawing, with the middle tiled.
 */
@Mixin(targets = "net.modificationstation.stationapi.impl.effect.EffectsRenderer", remap = false)
abstract class EffectsRendererMixin {
    /** The usable width of the middle strip (u 109 to 242 in the texture). */
    @Unique
    private static final int SCARLET_LIB$TILE = 128;

    @Inject(method = "renderEffectBack", at = @At("HEAD"), cancellable = true, require = 0)
    private void scarlet_lib$tiledBack(Minecraft minecraft, int y, int width, CallbackInfo ci) {
        DrawContext draw = (DrawContext) (Object) this;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/achievement/bg.png"));
        int right = width - 12;
        int bottom = y + 13;
        draw.drawTexture(2, y, 96, 202, 14, 14);
        draw.drawTexture(2, bottom, 96, 220, 14, 14);
        if (width > 26) {
            int middle = width - 26 + 1;
            for (int dx = 0; dx < middle; dx += SCARLET_LIB$TILE) {
                int w = Math.min(SCARLET_LIB$TILE, middle - dx);
                draw.drawTexture(15 + dx, y, 109, 202, w, 14);
                draw.drawTexture(15 + dx, bottom, 109, 220, w, 14);
            }
        }
        draw.drawTexture(right, y, 242, 202, 14, 14);
        draw.drawTexture(right, bottom, 242, 220, 14, 14);
        ci.cancel();
    }
}
