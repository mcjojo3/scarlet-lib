package mc.sayda.scarlet_lib.effect;

import mc.sayda.scarlet_lib.ScarletLib;
import net.modificationstation.stationapi.api.effect.EntityEffectType;
import net.modificationstation.stationapi.api.effect.EntityEffectTypeRegistry;
import net.modificationstation.stationapi.api.registry.Registry;
import net.modificationstation.stationapi.api.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of amplified effects. The vanilla ones are registered under their own ids
 * (minecraft:speed, ...). Other mods register theirs with {@link #register} from an
 * EntityEffectTypeRegistryEvent listener; icons go in
 * assets/&lt;namespace&gt;/stationapi/textures/gui/effect/&lt;path&gt;.png and names under
 * gui.stationapi.effect.&lt;namespace&gt;.&lt;path&gt;.name, as for any StationAPI effect.
 */
public final class EffectTypes {
    /**
     * The backported vanilla effects. Hunger, saturation, luck, unluck, bad omen and
     * hero_of_the_village are not here: beta has no food meter, luck or raids.
     */
    public static final Set<String> VANILLA = Set.of(
            "speed", "slowness", "haste", "mining_fatigue", "strength", "weakness", "jump_boost",
            "regeneration", "resistance", "fire_resistance", "water_breathing", "night_vision", "poison",
            "absorption", "levitation", "darkness", "conduit_power", "instant_health", "instant_damage", "nausea",
            "blindness", "invisibility", "slow_falling", "wither", "glowing", "health_boost", "dolphins_grace");

    private static final Map<Identifier, EntityEffectType<AmplifiedEffect>> BY_ID = new LinkedHashMap<>();
    private static final Map<EntityEffectType<?>, Identifier> IDS = new IdentityHashMap<>();
    private static final Map<EntityEffectType<?>, EffectBehavior> BEHAVIORS = new IdentityHashMap<>();

    private EffectTypes() {
    }

    /** Registers an amplified effect; call while StationAPI's effect registry event runs. */
    public static EntityEffectType<AmplifiedEffect> register(Identifier id, EffectBehavior behavior) {
        EntityEffectType<AmplifiedEffect> existing = BY_ID.get(id);
        if (existing != null) return existing;
        @SuppressWarnings("unchecked")
        EntityEffectType<AmplifiedEffect>[] holder = new EntityEffectType[1];
        // Anonymous subclass: getType() has to work while the instance is being constructed.
        EntityEffectType<AmplifiedEffect> type = EntityEffectType.<AmplifiedEffect>builder((entity, ticks) ->
                new AmplifiedEffect(entity, ticks) {
                    @Override
                    public EntityEffectType<AmplifiedEffect> getType() {
                        return holder[0];
                    }
                }).build();
        holder[0] = type;
        Registry.register(EntityEffectTypeRegistry.INSTANCE, id, type);
        BY_ID.put(id, type);
        IDS.put(type, id);
        BEHAVIORS.put(type, behavior);
        return type;
    }

    /** Internal: Scarlet Lib registers the vanilla effects from its own registry listener. */
    @org.jetbrains.annotations.ApiStatus.Internal
    public static void registerVanilla() {
        for (String path : VANILLA) register(Identifier.of("minecraft:" + path), VanillaEffects.behavior(path));
        ScarletLib.LOGGER.info("Registered {} vanilla status effects.", VANILLA.size());
    }

    @Nullable
    public static EntityEffectType<AmplifiedEffect> get(@Nullable Identifier id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** "speed" or "minecraft:speed" (a bare path means minecraft). */
    @Nullable
    public static EntityEffectType<AmplifiedEffect> get(@Nullable String id) {
        if (id == null || id.isEmpty()) return null;
        return get(Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id));
    }

    @Nullable
    public static Identifier id(EntityEffectType<?> type) {
        return IDS.get(type);
    }

    public static boolean isVanilla(Identifier id) {
        return id.namespace.toString().equals("minecraft") && VANILLA.contains(id.path);
    }

    /** The behaviour an effect type was registered with. */
    public static EffectBehavior behavior(EntityEffectType<?> type) {
        return BEHAVIORS.getOrDefault(type, EffectBehavior.NONE);
    }

    public static Collection<EntityEffectType<AmplifiedEffect>> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
