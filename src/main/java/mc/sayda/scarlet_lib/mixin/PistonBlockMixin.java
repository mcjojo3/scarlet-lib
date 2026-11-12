package mc.sayda.scarlet_lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A piston placed close by faces up or down depending on whether the player's eyes are above or
 * below it, which beta takes as feet + 1.82 (eyes plus 0.2). Use the player's real eyes.
 */
@Mixin(PistonBlock.class)
abstract class PistonBlockMixin {
    @ModifyExpressionValue(method = "getFacingForPlacement", at = @At(value = "CONSTANT", args = "doubleValue=1.82"))
    private static double scarlet_lib$ownEyeHeight(double vanillaEye, @Local(argsOnly = true) PlayerEntity player) {
        return BodySize.eyeHeight(player) + 0.2;
    }
}
