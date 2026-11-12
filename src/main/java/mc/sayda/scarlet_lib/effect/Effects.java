package mc.sayda.scarlet_lib.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.modificationstation.stationapi.api.effect.EntityEffect;
import net.modificationstation.stationapi.api.effect.EntityEffectType;
import net.modificationstation.stationapi.api.entity.StationEffectsEntity;
import net.modificationstation.stationapi.api.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Applying and querying amplified effects on any entity. */
public final class Effects {
    private static final List<ApplyFilter> FILTERS = new ArrayList<>();

    /** Can veto an effect (an immunity); runs on the authoritative side before it is added. */
    @FunctionalInterface
    public interface ApplyFilter {
        boolean allow(Entity entity, Identifier effect, int duration, int amplifier);
    }

    private Effects() {
    }

    public static void addApplyFilter(ApplyFilter filter) {
        FILTERS.add(filter);
    }

    private static StationEffectsEntity fx(Entity entity) {
        return (StationEffectsEntity) entity;
    }

    /**
     * Adds (or replaces) an effect: duration in ticks, negative for infinite; amplifier 0 is
     * level I. Acts on the authoritative side only (StationAPI syncs to clients). Returns false if
     * the effect is unknown, vetoed, or the entity is dead.
     */
    public static boolean apply(Entity entity, @Nullable Identifier id, int duration, int amplifier) {
        EntityEffectType<AmplifiedEffect> type = EffectTypes.get(id);
        if (type == null || duration == 0 || entity.world == null || entity.world.isRemote) return false;
        if (entity instanceof LivingEntity living && !living.isAlive()) return false;
        for (ApplyFilter filter : FILTERS) {
            if (!filter.allow(entity, id, duration, amplifier)) return false;
        }
        AmplifiedEffect.PENDING_AMPLIFIER.set(Math.max(0, amplifier));
        try {
            fx(entity).addEffect(type, duration < 0 ? EntityEffect.INFINITY_TICKS : duration);
        } finally {
            AmplifiedEffect.PENDING_AMPLIFIER.set(0);
        }
        return true;
    }

    @Nullable
    public static AmplifiedEffect get(Entity entity, @Nullable Identifier id) {
        EntityEffectType<AmplifiedEffect> type = EffectTypes.get(id);
        return type == null || !fx(entity).hasEffect(type) ? null : fx(entity).getEffect(type);
    }

    public static boolean has(Entity entity, @Nullable Identifier id) {
        EntityEffectType<AmplifiedEffect> type = EffectTypes.get(id);
        return type != null && fx(entity).hasEffect(type);
    }

    /** "speed" or "minecraft:speed". */
    public static boolean has(Entity entity, String id) {
        EntityEffectType<AmplifiedEffect> type = EffectTypes.get(id);
        return type != null && fx(entity).hasEffect(type);
    }

    /** The amplifier (0 = level I), or -1 when the effect is absent. */
    public static int amplifier(Entity entity, String id) {
        EntityEffectType<AmplifiedEffect> type = EffectTypes.get(id);
        if (type == null || !fx(entity).hasEffect(type)) return -1;
        AmplifiedEffect effect = fx(entity).getEffect(type);
        return effect == null ? -1 : effect.getAmplifier();
    }

    public static void remove(Entity entity, @Nullable Identifier id) {
        EntityEffectType<AmplifiedEffect> type = EffectTypes.get(id);
        if (type != null && entity.world != null && !entity.world.isRemote && fx(entity).hasEffect(type)) {
            fx(entity).removeEffect(type);
        }
    }

    /** All amplified effects on the entity. */
    public static List<AmplifiedEffect> all(Entity entity) {
        List<AmplifiedEffect> result = new ArrayList<>();
        for (EntityEffect<?> e : fx(entity).getEffects()) {
            if (e instanceof AmplifiedEffect effect) result.add(effect);
        }
        return result;
    }

    public static void removeAll(Entity entity) {
        if (entity.world == null || entity.world.isRemote) return;
        for (AmplifiedEffect effect : all(entity)) fx(entity).removeEffect(effect.getType());
    }
}
