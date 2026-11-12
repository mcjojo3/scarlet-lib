package mc.sayda.scarlet_lib.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("fallDistance")
    float scarlet_lib$getFallDistance();

    @Accessor("fallDistance")
    void scarlet_lib$setFallDistance(float value);
}
