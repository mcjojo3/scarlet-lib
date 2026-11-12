package mc.sayda.scarlet_lib.effect;

/**
 * What an effect does while active. All methods run on both sides; check
 * {@code effect.getEntity().world.isRemote} for server-only work.
 */
public interface EffectBehavior {
    EffectBehavior NONE = new EffectBehavior() {
    };

    default void onAdded(AmplifiedEffect effect, boolean appliedNow) {
    }

    default void onTick(AmplifiedEffect effect) {
    }

    default void onRemoved(AmplifiedEffect effect) {
    }

    /**
     * For an effect with a damage pool ({@link AmplifiedEffect#setPool}): whether the pool soaks
     * damage of this type. Every type by default.
     */
    default boolean absorbs(AmplifiedEffect effect, String damageType) {
        return true;
    }

    /** The effect's damage pool has just been used up (server side). */
    default void onPoolEmptied(AmplifiedEffect effect) {
    }
}
