package mc.sayda.scarlet_lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mc.sayda.scarlet_lib.attribute.Attributes;
import mc.sayda.scarlet_lib.damage.Damage;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Body size and max health each tick, melee attributes, and pickup/bed reach that follow size. */
@Mixin(PlayerEntity.class)
abstract class PlayerEntityMixin {
    /**
     * A client player's y is already at their eyes, and their "eye height" is the 0.12 above that
     * where checks like suffocation look, still inside a vanilla head. Scale it with the body so it
     * stays inside a small one. (Server players have their own, see ServerPlayerEntityMixin.)
     */
    @ModifyReturnValue(method = "getEyeHeight", at = @At("RETURN"))
    private float scarlet_lib$scaledEyeOffset(float offset) {
        return offset * BodySize.heightScale(scarlet_lib$self());
    }

    /** A rider sits 0.5 down into the seat; scaled, so a small body is not buried in the boat. */
    @ModifyExpressionValue(method = "getStandingEyeHeight", at = @At(value = "CONSTANT", args = "floatValue=0.5"))
    private float scarlet_lib$seatDepth(float depth) {
        return depth * BodySize.heightScale(scarlet_lib$self());
    }

    @Unique
    private int scarlet_lib$meleeDamage;

    @Unique
    private PlayerEntity scarlet_lib$self() {
        return (PlayerEntity) (Object) this;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void scarlet_lib$tick(CallbackInfo ci) {
        PlayerEntity self = scarlet_lib$self();
        BodySize.apply(self);
        Attributes.syncIfChanged(self);
        if (!self.world.isRemote && self.health > 0) {
            int max = Attributes.maxHealth(self);
            if (self.health > max) self.health = max;
        }
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void scarlet_lib$attackStart(Entity target, CallbackInfo ci) {
        scarlet_lib$meleeDamage = 0;
    }

    /** The attack damage attribute, starting from the held item's damage. */
    @ModifyExpressionValue(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerInventory;getAttackDamage(Lnet/minecraft/entity/Entity;)I"))
    private int scarlet_lib$attackDamage(int vanilla) {
        int damage = (int) Math.max(0, Math.round(Attributes.value(scarlet_lib$self(), Attributes.ATTACK_DAMAGE, vanilla)));
        scarlet_lib$meleeDamage = damage;
        return damage;
    }

    /** Attack knockback, then the attack listeners. */
    @Inject(method = "attack", at = @At("TAIL"))
    private void scarlet_lib$attackEnd(Entity target, CallbackInfo ci) {
        PlayerEntity self = scarlet_lib$self();
        if (self.world.isRemote || !(target instanceof LivingEntity living) || scarlet_lib$meleeDamage <= 0) return;
        double knockback = Attributes.value(self, Attributes.ATTACK_KNOCKBACK);
        if (knockback > 0) {
            double dx = target.x - self.x, dz = target.z - self.z;
            double len = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
            target.velocityX += dx / len * knockback * 0.5;
            target.velocityY += 0.1;
            target.velocityZ += dz / len * knockback * 0.5;
            target.velocityModified = true;
        }
        Damage.runAttackListeners(self, living, scarlet_lib$meleeDamage);
    }

    /**
     * Beta collects items in a box 1 wider than the player and no taller, so a small player misses
     * items lying a little above its feet. Smaller players get 1.21's half block of vertical reach.
     */
    @ModifyArg(method = "tickMovement", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/math/Box;expand(DDD)Lnet/minecraft/util/math/Box;"), index = 1)
    private double scarlet_lib$pickupHeight(double vertical) {
        return BodySize.heightScale(scarlet_lib$self()) < 0.995F ? Math.max(vertical, 0.5) : vertical;
    }

    /**
     * The bed reach check compares the bed with the player's y, which is at the eyes for client
     * players: a taller eye (3.24 at double size) would always be "too far away". Measure from a
     * normal eye height above the feet instead. Server players (y at the feet) are unchanged.
     */
    @ModifyExpressionValue(method = "trySleep", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;y:D"))
    private double scarlet_lib$bedReachFromNormalEyes(double y) {
        float eye = scarlet_lib$self().standingEyeHeight;
        return eye > 0 ? y - eye + BodySize.PLAYER_EYE : y;
    }
}
