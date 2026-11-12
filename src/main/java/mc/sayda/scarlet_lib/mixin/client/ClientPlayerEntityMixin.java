package mc.sayda.scarlet_lib.mixin.client;

import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Beta pushes a player out of solid blocks by checking the block at their waist and the one above
 * it, which assumes a body two blocks tall: it would push a small body out of one-block-high gaps
 * and miss a tall body's head. For a resized body, check the blocks it actually spans; a
 * vanilla-sized body keeps the vanilla check.
 */
@Mixin(ClientPlayerEntity.class)
abstract class ClientPlayerEntityMixin {
    @Inject(method = "pushOutOfBlock", at = @At("HEAD"), cancellable = true)
    private void scarlet_lib$pushOutOfBody(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (Math.abs(self.height - 1.8F) < 0.01F) return;
        cir.setReturnValue(false);
        World world = self.world;
        int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
        int y0 = MathHelper.floor(self.boundingBox.minY + 0.01), y1 = MathHelper.floor(self.boundingBox.maxY - 0.01);
        if (!scarlet_lib$blocked(world, bx, y0, y1, bz)) return;
        double fx = x - bx, fz = z - bz;
        int side = -1;
        double best = 9999.0;
        if (!scarlet_lib$blocked(world, bx - 1, y0, y1, bz) && fx < best) {
            best = fx;
            side = 0;
        }
        if (!scarlet_lib$blocked(world, bx + 1, y0, y1, bz) && 1.0 - fx < best) {
            best = 1.0 - fx;
            side = 1;
        }
        if (!scarlet_lib$blocked(world, bx, y0, y1, bz - 1) && fz < best) {
            best = fz;
            side = 4;
        }
        if (!scarlet_lib$blocked(world, bx, y0, y1, bz + 1) && 1.0 - fz < best) {
            side = 5;
        }
        float push = 0.1F;
        if (side == 0) self.velocityX = -push;
        if (side == 1) self.velocityX = push;
        if (side == 4) self.velocityZ = -push;
        if (side == 5) self.velocityZ = push;
    }

    @Unique
    private static boolean scarlet_lib$blocked(World world, int x, int y0, int y1, int z) {
        for (int y = y0; y <= y1; y++) {
            if (world.shouldSuffocate(x, y, z)) return true;
        }
        return false;
    }
}
