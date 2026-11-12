package mc.sayda.scarlet_lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mc.sayda.scarlet_lib.client.VisionClient;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Night vision on block entities (chests, signs...). */
@Mixin(BlockEntityRenderDispatcher.class)
abstract class BlockEntityRenderDispatcherMixin {
    @ModifyExpressionValue(method = "render(Lnet/minecraft/block/entity/BlockEntity;F)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;method_1782(III)F"))
    private float scarlet_lib$brightness(float brightness) {
        return VisionClient.lift(brightness);
    }
}
