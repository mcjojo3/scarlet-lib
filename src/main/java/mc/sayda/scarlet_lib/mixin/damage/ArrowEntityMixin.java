package mc.sayda.scarlet_lib.mixin.damage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.scarlet_lib.damage.Damage;
import mc.sayda.scarlet_lib.damage.DamageTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Arrow hits are "minecraft:arrow" (beta credits them to the archer, which would make them a melee hit). */
@Mixin(ArrowEntity.class)
abstract class ArrowEntityMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
    private boolean scarlet_lib$damageType(Entity target, Entity source, int amount, Operation<Boolean> original) {
        return Damage.withType(DamageTypes.ARROW, () -> original.call(target, source, amount));
    }
}
