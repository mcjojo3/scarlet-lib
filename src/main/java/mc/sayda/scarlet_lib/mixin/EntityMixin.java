package mc.sayda.scarlet_lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.scarlet_lib.damage.Damage;
import mc.sayda.scarlet_lib.damage.DamageTypes;
import mc.sayda.scarlet_lib.data.EntityData;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Per-entity data, fire and lava damage types, and fluid detection that follows body size. */
@Mixin(Entity.class)
abstract class EntityMixin implements EntityData.Holder {
    @Unique
    private NbtCompound scarlet_lib$data = new NbtCompound();

    @Override
    public NbtCompound scarlet_lib$getData() {
        return scarlet_lib$data;
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void scarlet_lib$writeData(NbtCompound nbt, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity && self.standingEyeHeight > 0) {
            // The saved y is the eye position; remember the body it belongs to (see readData).
            scarlet_lib$data.putFloat("scarlet_lib:eye", self.standingEyeHeight);
            scarlet_lib$data.putFloat("scarlet_lib:width", self.width);
            scarlet_lib$data.putFloat("scarlet_lib:height", self.height);
        }
        if (!scarlet_lib$data.values().isEmpty()) nbt.put("ScarletLibData", scarlet_lib$data);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void scarlet_lib$readData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("ScarletLibData")) scarlet_lib$data = nbt.getCompound("ScarletLibData");
        // A singleplayer player's saved y is their eye position, but loading puts the feet 1.62
        // below it (a vanilla body). Restore the body it was saved with; the next tick's resize
        // then keeps the feet where they are.
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity && self.standingEyeHeight > 0 && scarlet_lib$data.contains("scarlet_lib:eye")) {
            float eye = scarlet_lib$data.getFloat("scarlet_lib:eye");
            if (eye > 0 && Math.abs(eye - self.standingEyeHeight) > 1.0E-3) {
                ((EntityInvoker) self).scarlet_lib$setBoundingBoxSpacing(scarlet_lib$data.getFloat("scarlet_lib:width"),
                        scarlet_lib$data.getFloat("scarlet_lib:height"));
                self.standingEyeHeight = eye;
                self.setPosition(self.x, self.y, self.z);
            }
        }
    }

    // Tag vanilla's fire damage with its modern damage type.

    @WrapOperation(method = "baseTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
    private boolean scarlet_lib$onFireType(Entity instance, Entity source, int amount, Operation<Boolean> original) {
        return Damage.withType(DamageTypes.ON_FIRE, () -> original.call(instance, source, amount));
    }

    @WrapOperation(method = "setOnFire", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
    private boolean scarlet_lib$lavaType(Entity instance, Entity source, int amount, Operation<Boolean> original) {
        return Damage.withType(DamageTypes.LAVA, () -> original.call(instance, source, amount));
    }

    /** Standing in fire. (Lightning goes through here too, already typed, see below.) */
    @WrapOperation(method = "damage(I)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
    private boolean scarlet_lib$inFireType(Entity instance, Entity source, int amount, Operation<Boolean> original) {
        if (Damage.currentType() != null) return original.call(instance, source, amount);
        return Damage.withType(DamageTypes.IN_FIRE, () -> original.call(instance, source, amount));
    }

    @WrapOperation(method = "onStruckByLightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(I)V"))
    private void scarlet_lib$lightningType(Entity instance, int amount, Operation<Void> original) {
        Damage.withType(DamageTypes.LIGHTNING_BOLT, () -> {
            original.call(instance, amount);
            return true;
        });
    }

    /**
     * Beta finds water and lava with the hitbox shrunk 0.4 at the top and bottom. A small player
     * (a quarter-size one is 0.45 tall) would shrink to nothing and never count as in water, so the
     * inset follows the player's height.
     */
    @ModifyConstant(method = { "checkWaterCollisions", "isTouchingLava" }, constant = @Constant(doubleValue = -0.4000000059604645))
    private double scarlet_lib$scaledFluidInset(double inset) {
        return (Object) this instanceof PlayerEntity player ? inset * BodySize.heightScale(player) : inset;
    }

    /**
     * The suffocation check samples 0.05 above and below the eyes. That stays inside a vanilla head
     * but reaches above a small one, so touching a ceiling would suffocate; scale the spread with
     * the body.
     */
    @ModifyExpressionValue(method = "isInsideWall", at = @At(value = "CONSTANT", args = "floatValue=0.1"))
    private float scarlet_lib$eyeSampleSpread(float spread) {
        return (Object) this instanceof PlayerEntity player ? spread * BodySize.heightScale(player) : spread;
    }

    /**
     * Sneaking stops at an edge: a step is refused when nothing is within a block below the body.
     * A small player standing on something thinner than that (a micro block over air) found nothing
     * in every direction and could not move at all, so the drop it looks for follows their height.
     */
    @ModifyConstant(method = "move", constant = @Constant(doubleValue = -1.0))
    private double scarlet_lib$sneakEdgeDrop(double drop) {
        return (Object) this instanceof PlayerEntity player ? drop * BodySize.heightScale(player) : drop;
    }

    /** Footsteps and the view-bob rhythm are paced by the body's stride, like the walking animation. */
    @ModifyExpressionValue(method = "move", at = @At(value = "CONSTANT", args = "doubleValue=0.6"))
    private double scarlet_lib$footstepStride(double perBlock) {
        return (Object) this instanceof PlayerEntity player ? perBlock / BodySize.stride(player) : perBlock;
    }
}
