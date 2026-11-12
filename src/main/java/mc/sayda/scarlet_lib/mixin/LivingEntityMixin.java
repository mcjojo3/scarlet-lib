package mc.sayda.scarlet_lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.scarlet_lib.attribute.Attributes;
import mc.sayda.scarlet_lib.damage.Damage;
import mc.sayda.scarlet_lib.damage.DamageTypes;
import mc.sayda.scarlet_lib.effect.AmplifiedEffect;
import mc.sayda.scarlet_lib.effect.EffectBehavior;
import mc.sayda.scarlet_lib.effect.EffectTypes;
import mc.sayda.scarlet_lib.effect.Effects;
import mc.sayda.scarlet_lib.effect.VanillaEffects;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.modificationstation.stationapi.api.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The damage pipeline, healing, breathing, and the movement attributes and effects. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    /**
     * Swimming against a ledge, beta pushes up at 0.3 so you can climb out. With lower gravity the
     * same push lifts much higher; scaled by the square root of gravity, every body gets the
     * vanilla lift.
     */
    @ModifyExpressionValue(method = "travel", at = @At(value = "CONSTANT", args = "doubleValue=0.30000001192092896"))
    private double scarlet_lib$ledgeClimb(double push) {
        return push * Math.sqrt(Math.max(0, Attributes.ratio(scarlet_lib$self(), Attributes.GRAVITY)));
    }

    /** Walking animation paced by the body's stride: distance moved / stride (see BodySize.stride). */
    @ModifyExpressionValue(method = "travel", at = @At(value = "CONSTANT", args = "floatValue=4.0"))
    private float scarlet_lib$walkAnimationStride(float perBlock) {
        return scarlet_lib$self() instanceof PlayerEntity player ? perBlock / BodySize.stride(player) : perBlock;
    }

    @Unique
    private LivingEntity scarlet_lib$self() {
        return (LivingEntity) (Object) this;
    }

    @Unique
    private static final Identifier SCARLET_LIB$ABSORPTION = Identifier.of("minecraft:absorption");

    /** Takes damage out of the pools of absorption (or, with {@code absorption} false, every other effect). */
    @Unique
    private static float scarlet_lib$soak(LivingEntity self, String type, float amount, boolean absorption) {
        for (AmplifiedEffect effect : Effects.all(self)) {
            if (amount <= 0) break;
            if (effect.getPool() <= 0 || SCARLET_LIB$ABSORPTION.equals(EffectTypes.id(effect.getType())) != absorption) continue;
            EffectBehavior behavior = EffectTypes.behavior(effect.getType());
            if (!behavior.absorbs(effect, type)) continue;
            double soaked = Math.min(effect.getPool(), amount);
            effect.setPool(effect.getPool() - soaked);
            amount -= (float) soaked;
            if (effect.getPool() <= 0) behavior.onPoolEmptied(effect);
        }
        return amount;
    }

    // ------------------------------------------------------------------ damage

    @WrapMethod(method = "damage")
    private boolean scarlet_lib$damage(Entity source, int amount, Operation<Boolean> original) {
        LivingEntity self = scarlet_lib$self();
        if (self.world == null || self.world.isRemote) return original.call(source, amount);
        String type = Damage.typeOf(source);
        if (DamageTypes.isFire(type) && Effects.has(self, "minecraft:fire_resistance")) return false;
        float amt = Damage.runModifiers(self, source, type, amount);
        if (amt <= 0) return false;
        amt = VanillaEffects.resistance(self, amt);
        // Effects with a damage pool soak damage before health: other mods' shields first, then absorption.
        amt = scarlet_lib$soak(self, type, amt, false);
        amt = scarlet_lib$soak(self, type, amt, true);
        int dealt = Math.round(amt);
        if (dealt <= 0) return false;
        boolean result = Damage.withType(type, () -> original.call(source, dealt));
        if (result) Damage.runListeners(self, source, type, dealt);
        return result;
    }

    @WrapOperation(method = "baseTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/Entity;I)Z", ordinal = 0))
    private boolean scarlet_lib$inWallType(LivingEntity instance, Entity source, int amount, Operation<Boolean> original) {
        return Damage.withType(DamageTypes.IN_WALL, () -> original.call(instance, source, amount));
    }

    @WrapOperation(method = "baseTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/Entity;I)Z", ordinal = 1))
    private boolean scarlet_lib$drownType(LivingEntity instance, Entity source, int amount, Operation<Boolean> original) {
        return Damage.withType(DamageTypes.DROWN, () -> original.call(instance, source, amount));
    }

    @WrapOperation(method = "tickInVoid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/Entity;I)Z"))
    private boolean scarlet_lib$voidType(LivingEntity instance, Entity source, int amount, Operation<Boolean> original) {
        return Damage.withType(DamageTypes.OUT_OF_WORLD, () -> original.call(instance, source, amount));
    }

    /** Falls: the safe fall distance attribute replaces beta's 3 blocks... */
    @ModifyConstant(method = "onLanding", constant = @Constant(floatValue = 3.0F))
    private float scarlet_lib$safeFallDistance(float vanilla) {
        return (float) Attributes.value(scarlet_lib$self(), Attributes.SAFE_FALL_DISTANCE);
    }

    /** ...and the fall damage multiplier scales what is left. */
    @WrapOperation(method = "onLanding", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/Entity;I)Z"))
    private boolean scarlet_lib$fallType(LivingEntity instance, Entity source, int amount, Operation<Boolean> original) {
        int scaled = (int) Math.ceil(amount * Attributes.value(instance, Attributes.FALL_DAMAGE_MULTIPLIER));
        if (scaled <= 0) return false;
        return Damage.withType(DamageTypes.FALL, () -> original.call(instance, source, scaled));
    }

    /** Knockback resistance as a chance to ignore knockback, as vanilla did before 1.16. */
    @Inject(method = "applyKnockback", at = @At("HEAD"), cancellable = true)
    private void scarlet_lib$knockbackResistance(Entity attacker, int amount, double dx, double dz, CallbackInfo ci) {
        LivingEntity self = scarlet_lib$self();
        double resistance = Attributes.value(self, Attributes.KNOCKBACK_RESISTANCE);
        if (resistance > 0 && self.world.random.nextDouble() < resistance) ci.cancel();
    }

    // ------------------------------------------------------------------ health and breathing

    @WrapMethod(method = "heal")
    private void scarlet_lib$healModifiers(int amount, Operation<Void> original) {
        int scaled = Damage.scaleHeal(scarlet_lib$self(), amount);
        if (amount <= 0) original.call(amount);
        else if (scaled > 0) original.call(scaled);
    }

    /** Beta caps healing at 20; the max health attribute replaces that. */
    @ModifyConstant(method = "heal", constant = @Constant(intValue = 20))
    private int scarlet_lib$maxHealth(int vanillaMax) {
        return Attributes.maxHealth(scarlet_lib$self());
    }

    @Inject(method = "canBreatheInWater", at = @At("RETURN"), cancellable = true)
    private void scarlet_lib$waterBreathing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = scarlet_lib$self();
        if (Effects.has(self, "minecraft:water_breathing") || Effects.has(self, "minecraft:conduit_power")) {
            cir.setReturnValue(true);
        }
    }

    // ------------------------------------------------------------------ movement

    /** Movement speed: walking, swimming and air acceleration. */
    @ModifyArg(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;moveNonSolid(FFF)V"), index = 2)
    private float scarlet_lib$movementSpeed(float speed) {
        return speed * (float) Attributes.ratio(scarlet_lib$self(), Attributes.MOVEMENT_SPEED);
    }

    /** Gravity (0.08 in air, 0.02 in water and lava, scaled together). */
    @ModifyConstant(method = "travel", constant = { @Constant(doubleValue = 0.02), @Constant(doubleValue = 0.08) })
    private double scarlet_lib$gravity(double gravity) {
        return gravity * Attributes.ratio(scarlet_lib$self(), Attributes.GRAVITY);
    }

    /** Climbing out of water/lava checks for room 0.6 above; a smaller player needs less. */
    @ModifyConstant(method = "travel", constant = @Constant(doubleValue = 0.6000000238418579))
    private double scarlet_lib$fluidExit(double offset) {
        return scarlet_lib$self() instanceof PlayerEntity player ? offset * BodySize.heightScale(player) : offset;
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void scarlet_lib$levitation(float x, float z, CallbackInfo ci) {
        LivingEntity self = scarlet_lib$self();
        int levitation = Effects.amplifier(self, "minecraft:levitation");
        if (levitation >= 0) {
            self.velocityY += (0.05 * (levitation + 1) - self.velocityY) * 0.2;
            ((EntityAccessor) self).scarlet_lib$setFallDistance(0);
        } else if (Effects.has(self, "minecraft:slow_falling") && self.velocityY < -0.08) {
            self.velocityY = -0.08;
            ((EntityAccessor) self).scarlet_lib$setFallDistance(0);
        }
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void scarlet_lib$dolphinsGrace(float x, float z, CallbackInfo ci) {
        LivingEntity self = scarlet_lib$self();
        if (self.isSubmergedInWater() && Effects.has(self, "minecraft:dolphins_grace")) {
            // Water drag 0.96 instead of 0.8 horizontally, as in 1.21.
            self.velocityX *= 1.2;
            self.velocityZ *= 1.2;
        }
    }

    /** Jump strength replaces beta's 0.42 jump velocity. */
    @ModifyConstant(method = "jump", constant = @Constant(doubleValue = 0.41999998688697815))
    private double scarlet_lib$jumpStrength(double velocity) {
        return Attributes.value(scarlet_lib$self(), Attributes.JUMP_STRENGTH);
    }

    /** Jump boost adds 0.1 per level; a jump strength of 0 means no jumping at all. */
    @Inject(method = "jump", at = @At("TAIL"))
    private void scarlet_lib$jumpBoost(CallbackInfo ci) {
        LivingEntity self = scarlet_lib$self();
        if (Attributes.value(self, Attributes.JUMP_STRENGTH) <= 1.0E-5) {
            self.velocityY = 0;
            return;
        }
        int boost = Effects.amplifier(self, "minecraft:jump_boost");
        if (boost >= 0) self.velocityY += 0.1 * (boost + 1);
    }
}
