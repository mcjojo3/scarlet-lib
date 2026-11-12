package mc.sayda.scarlet_lib.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.util.math.Box;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Joining and respawning push the player up a block at a time until their body is clear of
 * blocks, testing beta's default 0.6 x 1.8 body because the player has not ticked yet. Test the
 * real body instead, so a small player is not lifted out of a low space and a large one does not
 * end up in a wall. (Mods that keep size data on the player need it on the new player before
 * these loops run; on joining it is already loaded from the save.)
 */
@Mixin(PlayerManager.class)
abstract class PlayerManagerMixin {
    @WrapOperation(method = { "respawnPlayer", "addPlayer" }, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/ServerWorld;getEntityCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/util/List;"))
    private List<?> scarlet_lib$fitBody(ServerWorld world, Entity entity, Box box, Operation<List<?>> original) {
        if (entity instanceof PlayerEntity player) {
            BodySize.fit(player);
            box = player.boundingBox;
        }
        return original.call(world, entity, box);
    }
}
