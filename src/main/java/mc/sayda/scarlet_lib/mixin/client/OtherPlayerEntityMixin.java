package mc.sayda.scarlet_lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.client.network.OtherPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Other players' walking animation (computed in their own tick on the client), paced by their stride. */
@Mixin(OtherPlayerEntity.class)
abstract class OtherPlayerEntityMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "CONSTANT", args = "floatValue=4.0"))
    private float scarlet_lib$walkAnimationStride(float perBlock) {
        return perBlock / BodySize.stride((OtherPlayerEntity) (Object) this);
    }
}
