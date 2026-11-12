package mc.sayda.scarlet_lib.damage;

import java.util.Set;

/**
 * Modern damage type ids for beta's typeless damage. Vanilla damage is tagged automatically:
 * fire, lava, falling, drowning, suffocation, the void, melee, arrows, thrown things, fireballs,
 * explosions, cactus and lightning. Mods pass their own type to {@link Damage#deal}; any
 * namespaced id works ("mymod:bleeding").
 */
public final class DamageTypes {
    public static final String GENERIC = "minecraft:generic";
    public static final String IN_FIRE = "minecraft:in_fire";
    public static final String ON_FIRE = "minecraft:on_fire";
    public static final String LAVA = "minecraft:lava";
    public static final String FALL = "minecraft:fall";
    public static final String DROWN = "minecraft:drown";
    public static final String IN_WALL = "minecraft:in_wall";
    public static final String OUT_OF_WORLD = "minecraft:out_of_world";
    public static final String PLAYER_ATTACK = "minecraft:player_attack";
    public static final String MOB_ATTACK = "minecraft:mob_attack";
    public static final String ARROW = "minecraft:arrow";
    public static final String THROWN = "minecraft:thrown";
    public static final String FIREBALL = "minecraft:fireball";
    public static final String EXPLOSION = "minecraft:explosion";
    public static final String PLAYER_EXPLOSION = "minecraft:player_explosion";
    public static final String CACTUS = "minecraft:cactus";
    public static final String LIGHTNING_BOLT = "minecraft:lightning_bolt";
    public static final String MAGIC = "minecraft:magic";
    public static final String WITHER = "minecraft:wither";
    public static final String THORNS = "minecraft:thorns";

    /** Blocked by fire resistance. */
    public static final Set<String> FIRE = Set.of(IN_FIRE, ON_FIRE, LAVA);

    private DamageTypes() {
    }

    public static boolean isFire(String type) {
        return FIRE.contains(type);
    }
}
