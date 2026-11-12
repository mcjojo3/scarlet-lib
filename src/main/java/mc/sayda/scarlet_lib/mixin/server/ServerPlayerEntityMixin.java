package mc.sayda.scarlet_lib.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A server player's y is at their feet, and beta hard-codes their eye height as 1.62. Suffocation
 * (the block at eye level) and line of sight (mobs seeing the player) both use it, so use the
 * body's real eye height.
 */
@Mixin(ServerPlayerEntity.class)
abstract class ServerPlayerEntityMixin {
    @ModifyReturnValue(method = "getEyeHeight", at = @At("RETURN"))
    private float scarlet_lib$scaledEyeHeight(float vanilla) {
        return BodySize.eyeHeight((ServerPlayerEntity) (Object) this);
    }
}
