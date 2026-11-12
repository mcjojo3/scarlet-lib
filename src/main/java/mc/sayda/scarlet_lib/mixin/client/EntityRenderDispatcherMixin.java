package mc.sayda.scarlet_lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mc.sayda.scarlet_lib.client.ScarletLibClient;
import mc.sayda.scarlet_lib.client.VisionClient;
import mc.sayda.scarlet_lib.vision.Visibility;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entities in the world (not GUI previews such as the inventory player, which use the other
 * render method): hidden ones are skipped whole, with their shadow and name tag, and night vision
 * lights the rest.
 */
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {
    @Inject(method = "render(Lnet/minecraft/entity/Entity;F)V", at = @At("HEAD"), cancellable = true)
    private void scarlet_lib$hide(Entity entity, float tickDelta, CallbackInfo ci) {
        if (!Visibility.canSee(ScarletLibClient.minecraft().player, entity)) ci.cancel();
    }

    @ModifyExpressionValue(method = "render(Lnet/minecraft/entity/Entity;F)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getBrightnessAtEyes(F)F"))
    private float scarlet_lib$nightVision(float brightness) {
        return VisionClient.lift(brightness);
    }
}
