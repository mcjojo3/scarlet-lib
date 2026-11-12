package mc.sayda.scarlet_lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BoatItem;
import net.minecraft.item.BucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Buckets and boats aim from feet + 1.62 (written as y + 1.62 - eye height), which is not where a
 * resized player's eyes are. Aim from the player's own eyes.
 */
@Mixin({ BucketItem.class, BoatItem.class })
abstract class EyeLevelItemsMixin {
    @ModifyExpressionValue(method = "use", at = @At(value = "CONSTANT", args = "doubleValue=1.62"))
    private double scarlet_lib$ownEyeHeight(double vanillaEye, @Local(argsOnly = true) PlayerEntity user) {
        return BodySize.eyeHeight(user);
    }
}
