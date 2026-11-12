package mc.sayda.scarlet_lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A player's shadow is as wide as their body. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {
    @ModifyExpressionValue(method = "renderShadow", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/render/entity/EntityRenderer;shadowRadius:F"))
    private float scarlet_lib$bodyShadow(float radius, @Local(argsOnly = true) Entity entity) {
        if (!(entity instanceof PlayerEntity player) || BodySize.guiPreview) return radius;
        return radius * BodySize.current(player).renderWidth();
    }
}
