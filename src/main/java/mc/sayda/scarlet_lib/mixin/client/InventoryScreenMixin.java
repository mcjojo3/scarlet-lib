package mc.sayda.scarlet_lib.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The inventory's player preview shows the player at full size whatever their body size, as 1.21
 * does (its preview divides out the scale attribute), instead of a tiny model at the bottom.
 */
@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin {
    @WrapOperation(method = "drawBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFF)V"))
    private void scarlet_lib$fullSizePreview(EntityRenderDispatcher dispatcher, Entity entity, double x, double y, double z,
            float yaw, float tickDelta, Operation<Void> original) {
        BodySize.guiPreview = true;
        try {
            original.call(dispatcher, entity, x, y, z, yaw, tickDelta);
        } finally {
            BodySize.guiPreview = false;
        }
    }
}
