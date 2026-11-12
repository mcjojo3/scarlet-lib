package mc.sayda.scarlet_lib.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws players at their body size (the model is scaled about the feet), name tag included. */
@Mixin(PlayerEntityRenderer.class)
abstract class PlayerEntityRendererMixin {
    @Inject(method = "applyScale(Lnet/minecraft/entity/player/PlayerEntity;F)V", at = @At("RETURN"))
    private void scarlet_lib$bodySize(PlayerEntity player, float tickDelta, CallbackInfo ci) {
        if (BodySize.guiPreview) return; // the inventory preview shows full size, as 1.21 does
        BodySize.Size size = BodySize.current(player);
        if (size.renderWidth() != 1 || size.renderHeight() != 1) {
            GL11.glScalef(size.renderWidth(), size.renderHeight(), size.renderWidth());
        }
    }

    /** The name tag floats 2.3 above the feet; keep it the same height above a resized head. */
    @ModifyVariable(method = "renderNameTag(Lnet/minecraft/entity/player/PlayerEntity;DDD)V", at = @At("HEAD"),
            argsOnly = true, ordinal = 1)
    private double scarlet_lib$nameTagHeight(double feetY, @Local(argsOnly = true) PlayerEntity player) {
        return feetY + 2.3 * (BodySize.current(player).renderHeight() - 1);
    }
}
