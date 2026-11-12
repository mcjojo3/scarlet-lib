package mc.sayda.scarlet_lib.attribute;

import mc.sayda.scarlet_lib.data.EntityData;
import mc.sayda.scarlet_lib.effect.VanillaEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.modificationstation.stationapi.api.network.packet.MessagePacket;
import net.modificationstation.stationapi.api.network.packet.PacketHelper;
import net.modificationstation.stationapi.api.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * The attribute registry and value computation. The vanilla attributes are applied where beta
 * uses the matching number: movement speed (walking/swimming acceleration), attack damage and
 * knockback (player melee), armor, max health, knockback resistance (chance, as before 1.16),
 * scale (with the size providers, see BodySize), step height, jump strength, gravity, safe fall
 * distance and fall damage multiplier, block/entity interaction range and block break speed.
 *
 * <p>Modifiers come from two places: stored ones ({@link #addModifier}, saved with the entity)
 * and {@link ModifierProvider}s, which compute them when the attribute is read.
 */
public final class Attributes {
    private static final Map<Identifier, Attribute> REGISTRY = new LinkedHashMap<>();
    private static final List<ModifierProvider> PROVIDERS = new ArrayList<>();
    private static final String STORE_KEY = "scarlet_lib:attributes";
    private static final String BASE_KEY = "scarlet_lib:attribute_bases";

    public static final Attribute MAX_HEALTH = vanilla("generic.max_health", 20, 1, 1024);
    public static final Attribute MOVEMENT_SPEED = vanilla("generic.movement_speed", 0.1, 0, 1024);
    public static final Attribute ATTACK_DAMAGE = vanilla("generic.attack_damage", 1, 0, 2048);
    public static final Attribute ATTACK_KNOCKBACK = vanilla("generic.attack_knockback", 0, 0, 5);
    public static final Attribute ARMOR = vanilla("generic.armor", 0, 0, 30);
    public static final Attribute KNOCKBACK_RESISTANCE = vanilla("generic.knockback_resistance", 0, 0, 1);
    public static final Attribute SCALE = vanilla("generic.scale", 1, 0.0625, 16);
    public static final Attribute STEP_HEIGHT = vanilla("generic.step_height", 0.5, 0, 10);
    public static final Attribute JUMP_STRENGTH = vanilla("generic.jump_strength", 0.41999998688697815, 0, 32);
    public static final Attribute GRAVITY = vanilla("generic.gravity", 0.08, -1, 1);
    public static final Attribute SAFE_FALL_DISTANCE = vanilla("generic.safe_fall_distance", 3, -1024, 1024);
    public static final Attribute FALL_DAMAGE_MULTIPLIER = vanilla("generic.fall_damage_multiplier", 1, 0, 100);
    public static final Attribute BLOCK_INTERACTION_RANGE = vanilla("player.block_interaction_range", 4, 0, 64);
    public static final Attribute ENTITY_INTERACTION_RANGE = vanilla("player.entity_interaction_range", 3, 0, 64);
    public static final Attribute BLOCK_BREAK_SPEED = vanilla("player.block_break_speed", 1, 0, 1024);

    static {
        addProvider(VanillaEffects::modifiers);
    }

    private Attributes() {
    }

    private static Attribute vanilla(String path, double defaultValue, double min, double max) {
        return register(new Attribute(Identifier.of("minecraft:" + path), defaultValue, min, max));
    }

    /** Registers a mod's own attribute (its id in the mod's namespace). */
    public static Attribute register(Attribute attribute) {
        REGISTRY.put(attribute.id(), attribute);
        return attribute;
    }

    @Nullable
    public static Attribute get(@Nullable Identifier id) {
        return id == null ? null : REGISTRY.get(id);
    }

    /**
     * "minecraft:generic.armor", "generic.armor", "armor", or 1.21.2's "minecraft:scale" /
     * "block_interaction_range" (no generic./player. prefix); other namespaces as written.
     */
    @Nullable
    public static Attribute get(@Nullable String id) {
        if (id == null || id.isEmpty()) return null;
        String s = id.toLowerCase(Locale.ROOT);
        if (s.startsWith("minecraft:")) s = s.substring("minecraft:".length());
        if (s.contains(":")) return get(Identifier.tryParse(s));
        if (s.contains(".")) return get(Identifier.tryParse("minecraft:" + s));
        Attribute generic = get(Identifier.tryParse("minecraft:generic." + s));
        return generic != null ? generic : get(Identifier.tryParse("minecraft:player." + s));
    }

    public static Collection<Attribute> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static void addProvider(ModifierProvider provider) {
        PROVIDERS.add(provider);
    }

    // ------------------------------------------------------------------ values

    public static double value(LivingEntity entity, Attribute attribute) {
        return value(entity, attribute, baseValue(entity, attribute));
    }

    // ------------------------------------------------------------------ base values (/attribute base set)

    /** The entity's base value: the attribute's default unless set with {@link #setBaseValue}. */
    public static double baseValue(LivingEntity entity, Attribute attribute) {
        NbtCompound store = EntityData.of(entity);
        if (store.contains(BASE_KEY)) {
            NbtCompound bases = store.getCompound(BASE_KEY);
            String key = attribute.id().toString();
            if (bases.contains(key)) return bases.getDouble(key);
        }
        return attribute.defaultValue();
    }

    /** Sets the entity's base value (saved with it); where callers pass their own base, that wins. */
    public static void setBaseValue(LivingEntity entity, Attribute attribute, double base) {
        NbtCompound store = EntityData.of(entity);
        NbtCompound bases = store.contains(BASE_KEY) ? store.getCompound(BASE_KEY) : new NbtCompound();
        bases.putDouble(attribute.id().toString(), attribute.clamp(base));
        store.put(BASE_KEY, bases);
        changed(entity);
    }

    public static void resetBaseValue(LivingEntity entity, Attribute attribute) {
        NbtCompound store = EntityData.of(entity);
        if (!store.contains(BASE_KEY)) return;
        NbtCompound old = store.getCompound(BASE_KEY), kept = new NbtCompound();
        String key = attribute.id().toString();
        for (Object o : old.values()) {
            NbtElement element = (NbtElement) o;
            if (!key.equals(element.getKey())) kept.put(element.getKey(), element);
        }
        store.put(BASE_KEY, kept);
        changed(entity);
    }

    // ------------------------------------------------------------------ client sync (dedicated server)

    /**
     * The stored modifiers and base values of a player, sent to their own client when they change
     * (movement, step height, jump and size are worked out on the client).
     */
    public static final Identifier SYNC_PACKET = Identifier.of("scarlet_lib:attributes");
    private static final Map<LivingEntity, Boolean> SYNCED = new WeakHashMap<>();

    private static void changed(LivingEntity entity) {
        SYNCED.remove(entity);
    }

    /** Server, once per player tick: sends the player's stored attributes if their client is behind. */
    public static void syncIfChanged(PlayerEntity player) {
        if (player.world == null || player.world.isRemote || SYNCED.containsKey(player)) return;
        SYNCED.put(player, Boolean.TRUE);
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) return;
        List<String> lines = new ArrayList<>();
        NbtCompound store = EntityData.of(player);
        if (store.contains(STORE_KEY)) {
            NbtList list = store.getList(STORE_KEY);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound tag = (NbtCompound) list.get(i);
                lines.add("m|" + tag.getString("Attribute") + "|" + tag.getString("Id") + "|" + tag.getByte("Op") + "|" + tag.getDouble("Value"));
            }
        }
        if (store.contains(BASE_KEY)) {
            NbtCompound bases = store.getCompound(BASE_KEY);
            for (Object o : bases.values()) {
                NbtElement element = (NbtElement) o;
                lines.add("b|" + element.getKey() + "|" + bases.getDouble(element.getKey()));
            }
        }
        MessagePacket packet = new MessagePacket(SYNC_PACKET);
        packet.strings = lines.toArray(new String[0]);
        PacketHelper.sendTo(player, packet);
    }

    /** Client side of {@link #syncIfChanged}: replaces the local player's stored attributes. */
    public static void handleSync(PlayerEntity receiver, MessagePacket message) {
        if (receiver == null || message.strings == null) return;
        NbtList list = new NbtList();
        NbtCompound bases = new NbtCompound();
        for (String line : message.strings) {
            String[] p = line.split("\\|");
            try {
                if (p.length == 5 && p[0].equals("m")) {
                    NbtCompound tag = new NbtCompound();
                    tag.putString("Attribute", p[1]);
                    tag.putString("Id", p[2]);
                    tag.putByte("Op", Byte.parseByte(p[3]));
                    tag.putDouble("Value", Double.parseDouble(p[4]));
                    list.add(tag);
                } else if (p.length == 3 && p[0].equals("b")) {
                    bases.putDouble(p[1], Double.parseDouble(p[2]));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        NbtCompound store = EntityData.of(receiver);
        store.put(STORE_KEY, list);
        store.put(BASE_KEY, bases);
    }

    /** The value starting from {@code base} (e.g. the held weapon's damage for attack damage). */
    public static double value(LivingEntity entity, Attribute attribute, double base) {
        double[] acc = { 0, 0, 1 }; // add, multiply base, multiply total
        Consumer<AttributeModifier> sink = mod -> {
            switch (mod.operation()) {
                case ADD_VALUE -> acc[0] += mod.value();
                case ADD_MULTIPLIED_BASE -> acc[1] += mod.value();
                case ADD_MULTIPLIED_TOTAL -> acc[2] *= 1 + mod.value();
            }
        };
        for (AttributeModifier mod : getModifiers(entity, attribute)) sink.accept(mod);
        for (ModifierProvider provider : PROVIDERS) provider.collect(entity, attribute, sink);
        return attribute.clamp((base + acc[0]) * (1 + acc[1]) * acc[2]);
    }

    /** The value relative to the default (1.0 = unmodified). */
    public static double ratio(LivingEntity entity, Attribute attribute) {
        return attribute.defaultValue() == 0 ? 1 : value(entity, attribute) / attribute.defaultValue();
    }

    /** Max health as beta's whole-number health. */
    public static int maxHealth(LivingEntity entity) {
        return (int) Math.max(1, Math.round(value(entity, MAX_HEALTH)));
    }

    // ------------------------------------------------------------------ stored modifiers

    public static List<AttributeModifier> getModifiers(LivingEntity entity, Attribute attribute) {
        NbtCompound store = EntityData.of(entity);
        if (!store.contains(STORE_KEY)) return List.of();
        NbtList list = store.getList(STORE_KEY);
        List<AttributeModifier> result = new ArrayList<>();
        String key = attribute.id().toString();
        for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = (NbtCompound) list.get(i);
            if (key.equals(tag.getString("Attribute"))) result.add(AttributeModifier.fromNbt(tag));
        }
        return result;
    }

    /** Adds (or replaces, by id) a modifier that is saved with the entity. */
    public static void addModifier(LivingEntity entity, Attribute attribute, AttributeModifier modifier) {
        removeModifier(entity, attribute, modifier.id());
        NbtList list = list(entity);
        NbtCompound tag = modifier.toNbt();
        tag.putString("Attribute", attribute.id().toString());
        list.add(tag);
        changed(entity);
    }

    /** A stored modifier by id, or null. */
    @Nullable
    public static AttributeModifier getModifier(LivingEntity entity, Attribute attribute, Identifier modifierId) {
        for (AttributeModifier mod : getModifiers(entity, attribute)) {
            if (mod.id().equals(modifierId)) return mod;
        }
        return null;
    }

    public static void removeModifier(LivingEntity entity, Attribute attribute, Identifier modifierId) {
        NbtCompound store = EntityData.of(entity);
        if (!store.contains(STORE_KEY)) return;
        changed(entity);
        NbtList old = store.getList(STORE_KEY);
        NbtList kept = new NbtList();
        String key = attribute.id().toString(), id = modifierId.toString();
        for (int i = 0; i < old.size(); i++) {
            NbtCompound tag = (NbtCompound) old.get(i);
            if (!(key.equals(tag.getString("Attribute")) && id.equals(tag.getString("Id")))) kept.add(tag);
        }
        store.put(STORE_KEY, kept);
    }

    private static NbtList list(LivingEntity entity) {
        NbtCompound store = EntityData.of(entity);
        if (!store.contains(STORE_KEY)) store.put(STORE_KEY, new NbtList());
        return store.getList(STORE_KEY);
    }
}
