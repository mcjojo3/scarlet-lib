package mc.sayda.scarlet_lib.effect;

import mc.sayda.scarlet_lib.attribute.Attribute;
import mc.sayda.scarlet_lib.attribute.AttributeModifier;
import mc.sayda.scarlet_lib.attribute.Attributes;
import mc.sayda.scarlet_lib.damage.Damage;
import mc.sayda.scarlet_lib.damage.DamageTypes;
import net.minecraft.entity.LivingEntity;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.function.Consumer;

/**
 * What the vanilla effects do. Ticking ones are behaviours here; the stat ones are attribute
 * modifiers ({@link #modifiers}); the rest live where vanilla checks them: resistance, fire
 * resistance and absorption in the damage pipeline, jump boost in jumping, levitation, slow
 * falling and dolphin's grace in movement, haste and mining fatigue in mining speed, night
 * vision, darkness and blindness in rendering, invisibility in Visibility. Nausea and glowing
 * are registered (they show and save) but do nothing yet. Internal: used by the mixins.
 */
public final class VanillaEffects {
    private static final Identifier SPEED = id("speed"), SLOWNESS = id("slowness"), STRENGTH = id("strength"),
            WEAKNESS = id("weakness"), HEALTH_BOOST = id("health_boost");

    private VanillaEffects() {
    }

    private static Identifier id(String path) {
        return Identifier.of("minecraft:" + path);
    }

    static EffectBehavior behavior(String path) {
        return switch (path) {
            case "absorption" -> new EffectBehavior() {
                @Override
                public void onAdded(AmplifiedEffect effect, boolean appliedNow) {
                    effect.setPool(4.0 * (effect.getAmplifier() + 1));
                }
            };
            case "regeneration" -> ticking((living, effect) -> {
                int interval = Math.max(1, 50 >> effect.getAmplifier());
                if (effect.getAge() % interval == 0 && living.health < Attributes.maxHealth(living)) Damage.heal(living, 1);
            });
            case "poison" -> ticking((living, effect) -> {
                int interval = Math.max(1, 25 >> effect.getAmplifier());
                if (effect.getAge() % interval == 0 && living.health > 1) Damage.deal(living, null, 1, DamageTypes.MAGIC);
            });
            case "wither" -> ticking((living, effect) -> {
                int interval = Math.max(1, 40 >> effect.getAmplifier());
                if (effect.getAge() % interval == 0) Damage.deal(living, null, 1, DamageTypes.WITHER);
            });
            case "instant_health" -> ticking((living, effect) -> {
                if (effect.getAge() == 1) Damage.heal(living, 4 << effect.getAmplifier());
            });
            case "instant_damage" -> ticking((living, effect) -> {
                if (effect.getAge() == 1) Damage.deal(living, null, 6 << effect.getAmplifier(), DamageTypes.MAGIC);
            });
            case "water_breathing", "conduit_power" -> ticking((living, effect) -> living.air = 300);
            case "fire_resistance" -> ticking((living, effect) -> {
                if (living.fireTicks > 0) living.fireTicks = 0;
            });
            default -> EffectBehavior.NONE;
        };
    }

    private interface Tick {
        void run(LivingEntity living, AmplifiedEffect effect);
    }

    /** Ticks on the authoritative side, for living entities. */
    private static EffectBehavior ticking(Tick tick) {
        return new EffectBehavior() {
            @Override
            public void onTick(AmplifiedEffect effect) {
                if (effect.getEntity() instanceof LivingEntity living && living.world != null && !living.world.isRemote) {
                    tick.run(living, effect);
                }
            }
        };
    }

    /** The attribute modifiers of the active vanilla effects (registered as an attribute provider). */
    public static void modifiers(LivingEntity entity, Attribute attribute, Consumer<AttributeModifier> sink) {
        if (attribute == Attributes.MOVEMENT_SPEED) {
            int speed = Effects.amplifier(entity, "minecraft:speed");
            if (speed >= 0) sink.accept(new AttributeModifier(SPEED, 0.2 * (speed + 1), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            int slowness = Effects.amplifier(entity, "minecraft:slowness");
            if (slowness >= 0) {
                sink.accept(new AttributeModifier(SLOWNESS, -Math.min(1.0, 0.15 * (slowness + 1)),
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else if (attribute == Attributes.ATTACK_DAMAGE) {
            int strength = Effects.amplifier(entity, "minecraft:strength");
            if (strength >= 0) sink.accept(new AttributeModifier(STRENGTH, 3 * (strength + 1), AttributeModifier.Operation.ADD_VALUE));
            int weakness = Effects.amplifier(entity, "minecraft:weakness");
            if (weakness >= 0) sink.accept(new AttributeModifier(WEAKNESS, -4 * (weakness + 1), AttributeModifier.Operation.ADD_VALUE));
        } else if (attribute == Attributes.MAX_HEALTH) {
            int boost = Effects.amplifier(entity, "minecraft:health_boost");
            if (boost >= 0) sink.accept(new AttributeModifier(HEALTH_BOOST, 4 * (boost + 1), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** 1.21's resistance: 20% less damage per level. */
    public static float resistance(LivingEntity entity, float amount) {
        int resistance = Effects.amplifier(entity, "minecraft:resistance");
        return resistance < 0 ? amount : amount * (float) Math.max(0, 1 - 0.2 * (resistance + 1));
    }
}
