package mc.sayda.scarlet_lib.attribute;

import net.modificationstation.stationapi.api.util.Identifier;

/**
 * An attribute: a number per living entity, computed from a base value and modifiers as 1.21's
 * AttributeInstance does. {@code defaultValue} is beta's own value where it differs from 1.21
 * (block reach 4, step height 0.5), so an entity without modifiers behaves exactly like beta.
 */
public record Attribute(Identifier id, double defaultValue, double min, double max) {
    public double clamp(double value) {
        return Math.max(min, Math.min(max, value));
    }
}
