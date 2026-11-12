package mc.sayda.scarlet_lib.mixin.damage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.scarlet_lib.damage.Damage;
import mc.sayda.scarlet_lib.damage.DamageTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Explosions are "minecraft:explosion" ("minecraft:player_explosion" when a player set them off). */
@Mixin(Explosion.class)
abstract class ExplosionMixin {
    @WrapOperation(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
    private boolean scarlet_lib$damageType(Entity target, Entity source, int amount, Operation<Boolean> original) {
        String type = source instanceof PlayerEntity ? DamageTypes.PLAYER_EXPLOSION : DamageTypes.EXPLOSION;
        return Damage.withType(type, () -> original.call(target, source, amount));
    }
}
