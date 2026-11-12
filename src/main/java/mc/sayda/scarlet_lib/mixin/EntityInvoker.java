package mc.sayda.scarlet_lib.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityInvoker {
    @Invoker("setBoundingBoxSpacing")
    void scarlet_lib$setBoundingBoxSpacing(float width, float height);
}
