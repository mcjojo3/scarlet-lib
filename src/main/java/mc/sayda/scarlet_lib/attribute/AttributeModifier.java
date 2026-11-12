package mc.sayda.scarlet_lib.attribute;

import net.minecraft.nbt.NbtCompound;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.Locale;

/**
 * One modifier, as 1.21's: the value is
 * (base + sum(ADD_VALUE)) * (1 + sum(ADD_MULTIPLIED_BASE)) * product(1 + ADD_MULTIPLIED_TOTAL).
 * The id identifies the modifier (re-adding an id replaces it).
 */
public record AttributeModifier(Identifier id, double value, Operation operation) {
    public enum Operation {
        ADD_VALUE, ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL;

        /** Accepts the 1.21 names and the older addition / multiply_base / multiply_total. */
        public static Operation parse(String s) {
            return switch (s == null ? "" : s.toUpperCase(Locale.ROOT)) {
                case "MULTIPLY_BASE", "ADD_MULTIPLIED_BASE" -> ADD_MULTIPLIED_BASE;
                case "MULTIPLY_TOTAL", "ADD_MULTIPLIED_TOTAL" -> ADD_MULTIPLIED_TOTAL;
                default -> ADD_VALUE;
            };
        }
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putString("Id", id.toString());
        tag.putDouble("Value", value);
        tag.putByte("Op", (byte) operation.ordinal());
        return tag;
    }

    public static AttributeModifier fromNbt(NbtCompound tag) {
        return fromNbt(Identifier.of(tag.getString("Id")), tag);
    }

    /** For modifiers saved under their id as the key. */
    public static AttributeModifier fromNbt(Identifier id, NbtCompound tag) {
        Operation[] ops = Operation.values();
        int op = Math.max(0, Math.min(tag.getByte("Op"), ops.length - 1));
        return new AttributeModifier(id, tag.getDouble("Value"), ops[op]);
    }
}
