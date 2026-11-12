package mc.sayda.scarlet_lib.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/**
 * Typed and fractional damage for beta, where damage is a whole number from an entity with no
 * type. The type of the damage being dealt travels alongside it ({@link #currentType}); fractions
 * carry over per entity, so 0.2 damage every half second still adds up.
 *
 * <p>Every hit on a living entity goes through the pipeline: the type is settled, fire resistance
 * applies, the registered {@link Modifier}s run in order, then resistance and absorption (and any
 * other effect with an absorption pool), and after the hit lands the {@link Listener}s run.
 */
public final class Damage {
    private static final ThreadLocal<String> TYPE = new ThreadLocal<>();
    /** Fractions carried over to the next hit, the next {@link #heal} and the next scaled heal. */
    private static final Map<Entity, Double> REMAINDERS = new WeakHashMap<>();
    private static final Map<Entity, Double> HEAL_FRACTIONS = new WeakHashMap<>();
    private static final Map<Entity, Double> HEAL_REMAINDERS = new WeakHashMap<>();
    private static final List<Modifier> MODIFIERS = new ArrayList<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();
    private static final List<HealModifier> HEAL_MODIFIERS = new ArrayList<>();

    /** Changes (or, returning 0 or less, cancels) the damage about to be dealt. */
    @FunctionalInterface
    public interface Modifier {
        float modify(LivingEntity victim, @Nullable Entity source, String type, float amount);
    }

    /** Runs after damage was dealt; {@code dealt} is what got through. */
    @FunctionalInterface
    public interface Listener {
        void onDamaged(LivingEntity victim, @Nullable Entity source, String type, int dealt);
    }

    /** Changes a heal (a multiplier from attributes, an effect, ...). */
    @FunctionalInterface
    public interface HealModifier {
        double modify(LivingEntity entity, double amount);
    }

    /**
     * A player's melee hit, after it landed (server side), with the final damage from the
     * attack damage attribute; for life steal, on-hit effects and the like.
     */
    @FunctionalInterface
    public interface AttackListener {
        void onMeleeHit(net.minecraft.entity.player.PlayerEntity attacker, LivingEntity target, int damage);
    }

    private static final List<AttackListener> ATTACK_LISTENERS = new ArrayList<>();

    public static void addAttackListener(AttackListener listener) {
        ATTACK_LISTENERS.add(listener);
    }

    public static void runAttackListeners(net.minecraft.entity.player.PlayerEntity attacker, LivingEntity target, int damage) {
        for (AttackListener listener : ATTACK_LISTENERS) listener.onMeleeHit(attacker, target, damage);
    }

    private Damage() {
    }

    public static void addModifier(Modifier modifier) {
        MODIFIERS.add(modifier);
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    public static void addHealModifier(HealModifier modifier) {
        HEAL_MODIFIERS.add(modifier);
    }

    /** The type of the damage being dealt right now, or null outside a typed hit. */
    @Nullable
    public static String currentType() {
        return TYPE.get();
    }

    /** Runs {@code body} (which deals vanilla damage) with {@code type} as its damage type. */
    public static boolean withType(@Nullable String type, BooleanSupplier body) {
        String previous = TYPE.get();
        TYPE.set(type);
        try {
            return body.getAsBoolean();
        } finally {
            TYPE.set(previous);
        }
    }

    /** Deals {@code amount} (half-hearts, may be fractional) of {@code type} on the authoritative side. */
    public static boolean deal(LivingEntity victim, @Nullable Entity source, double amount, @Nullable String type) {
        if (amount <= 0 || victim.world == null || victim.world.isRemote || !victim.isAlive()) return false;
        double total = amount + REMAINDERS.getOrDefault(victim, 0.0);
        int whole = (int) Math.floor(total);
        REMAINDERS.put(victim, total - whole);
        if (whole <= 0) return false;
        return withType(type, () -> victim.damage(source, whole));
    }

    /** Heals (half-hearts, may be fractional) through vanilla heal, so max health still applies. */
    public static void heal(LivingEntity entity, double amount) {
        if (amount <= 0 || entity.world == null || entity.world.isRemote) return;
        double total = amount + HEAL_FRACTIONS.getOrDefault(entity, 0.0);
        int whole = (int) Math.floor(total);
        HEAL_FRACTIONS.put(entity, total - whole);
        if (whole > 0) entity.heal(whole);
    }

    // ------------------------------------------------------------------ pipeline (used by the mixins)

    /** The type of an untyped vanilla hit, from its source. */
    public static String typeOf(@Nullable Entity source) {
        String type = TYPE.get();
        if (type != null) return type;
        if (source == null) return DamageTypes.GENERIC;
        if (source instanceof net.minecraft.entity.player.PlayerEntity) return DamageTypes.PLAYER_ATTACK;
        if (source instanceof net.minecraft.entity.projectile.ArrowEntity) return DamageTypes.ARROW;
        return DamageTypes.MOB_ATTACK;
    }

    public static float runModifiers(LivingEntity victim, @Nullable Entity source, String type, float amount) {
        for (Modifier modifier : MODIFIERS) {
            amount = modifier.modify(victim, source, type, amount);
            if (amount <= 0) return 0;
        }
        return amount;
    }

    public static void runListeners(LivingEntity victim, @Nullable Entity source, String type, int dealt) {
        for (Listener listener : LISTENERS) listener.onDamaged(victim, source, type, dealt);
    }

    /** Applies the heal modifiers; fractions carry over to the next heal. */
    public static int scaleHeal(LivingEntity entity, int amount) {
        if (amount <= 0 || HEAL_MODIFIERS.isEmpty()) return amount;
        double scaled = amount;
        for (HealModifier modifier : HEAL_MODIFIERS) scaled = modifier.modify(entity, scaled);
        if (Math.abs(scaled - amount) < 1.0E-6) return amount;
        double total = Math.max(0, scaled) + HEAL_REMAINDERS.getOrDefault(entity, 0.0);
        int whole = (int) Math.floor(total);
        HEAL_REMAINDERS.put(entity, total - whole);
        return whole;
    }
}
