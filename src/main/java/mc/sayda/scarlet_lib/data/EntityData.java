package mc.sayda.scarlet_lib.data;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

/**
 * An NBT compound saved with every entity (modern persistent data / data attachments). Mods
 * should prefix their keys with their namespace ("mymod:owner"). Saved under "ScarletLibData"
 * by the entity mixin.
 */
public final class EntityData {
    /** Implemented on Entity by the mixin. */
    public interface Holder {
        NbtCompound scarlet_lib$getData();
    }

    private EntityData() {
    }

    /** The entity's compound; changes are saved with the entity. */
    public static NbtCompound of(Entity entity) {
        return ((Holder) entity).scarlet_lib$getData();
    }

    public static boolean has(Entity entity, String key) {
        return of(entity).contains(key);
    }

    public static double getDouble(Entity entity, String key) {
        NbtCompound tag = of(entity);
        return tag.contains(key) ? tag.getDouble(key) : 0.0;
    }

    public static void putDouble(Entity entity, String key, double value) {
        of(entity).putDouble(key, value);
    }

    @Nullable
    public static String getString(Entity entity, String key) {
        NbtCompound tag = of(entity);
        return tag.contains(key) ? tag.getString(key) : null;
    }

    public static void putString(Entity entity, String key, String value) {
        of(entity).putString(key, value);
    }

    public static NbtCompound getCompound(Entity entity, String key) {
        NbtCompound tag = of(entity);
        if (!tag.contains(key)) tag.put(key, new NbtCompound());
        return tag.getCompound(key);
    }
}
