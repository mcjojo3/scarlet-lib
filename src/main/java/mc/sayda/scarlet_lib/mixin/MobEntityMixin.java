package mc.sayda.scarlet_lib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.scarlet_lib.vision.Visibility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mobs do not pick, or keep, targets they cannot see (see Visibility). */
@Mixin(MobEntity.class)
abstract class MobEntityMixin {
    @Shadow
    protected Entity target;

    @Inject(method = "tickLiving", at = @At("HEAD"))
    private void scarlet_lib$forgetHidden(CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (!self.world.isRemote && target != null && !Visibility.canSee(self, target)) target = null;
    }

    @WrapOperation(method = "tickLiving", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/MobEntity;getTargetInRange()Lnet/minecraft/entity/Entity;"))
    private Entity scarlet_lib$skipHidden(MobEntity self, Operation<Entity> original) {
        Entity found = original.call(self);
        return found != null && !Visibility.canSee(self, found) ? null : found;
    }
}
