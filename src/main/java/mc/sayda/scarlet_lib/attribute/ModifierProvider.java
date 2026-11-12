package mc.sayda.scarlet_lib.attribute;

import net.minecraft.entity.LivingEntity;

import java.util.function.Consumer;

/**
 * Supplies modifiers computed on the fly (from effects, equipment, a mod's own player data...)
 * instead of stored ones. Called every time an attribute is read, so keep it cheap.
 */
@FunctionalInterface
public interface ModifierProvider {
    void collect(LivingEntity entity, Attribute attribute, Consumer<AttributeModifier> sink);
}
