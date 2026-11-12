package mc.sayda.scarlet_lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mc.sayda.scarlet_lib.client.VisionClient;
import net.minecraft.world.WorldRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Night vision on terrain: chunk meshes bake the brightness this view reports. */
@Mixin(WorldRegion.class)
abstract class WorldRegionMixin {
    @ModifyReturnValue(method = "getNaturalBrightness", at = @At("RETURN"))
    private float scarlet_lib$naturalBrightness(float brightness) {
        return VisionClient.lift(brightness);
    }

    @ModifyReturnValue(method = "method_1782", at = @At("RETURN"))
    private float scarlet_lib$brightness(float brightness) {
        return VisionClient.lift(brightness);
    }
}
