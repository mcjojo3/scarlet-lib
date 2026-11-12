package mc.sayda.scarlet_lib.mixin.damage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.scarlet_lib.damage.Damage;
import mc.sayda.scarlet_lib.damage.DamageTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Snowball and egg hits are "minecraft:thrown". */
@Mixin({ SnowballEntity.class, EggEntity.class })
abstract class ThrownEntityMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
    private boolean scarlet_lib$damageType(Entity target, Entity source, int amount, Operation<Boolean> original) {
        return Damage.withType(DamageTypes.THROWN, () -> original.call(target, source, amount));
    }
}
