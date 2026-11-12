package mc.sayda.scarlet_lib.mixin.client;

import mc.sayda.scarlet_lib.attribute.Attributes;
import mc.sayda.scarlet_lib.effect.Effects;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The client-only parts of a player: mining speed (beta computes it on the client) and the
 * push up out of blocks when spawning.
 */
@Mixin(PlayerEntity.class)
abstract class PlayerEntityClientMixin {
    /**
     * Spawning and respawning reset the player to beta's 0.6 x 1.8 body and push them up until
     * that fits, which would lift a small body out of a low space. Fit the real body first.
     */
    @Inject(method = "teleportTop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;teleportTop()V"))
    private void scarlet_lib$fitBeforeSpawnCheck(CallbackInfo ci) {
        BodySize.fit((PlayerEntity) (Object) this);
    }

    /**
     * Block break speed, haste and mining fatigue, multiplied onto whatever the speed already is so
     * other mods can adjust it too.
     */

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void scarlet_lib$miningSpeed(Block block, CallbackInfoReturnable<Float> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        float speed = cir.getReturnValue() * (float) Attributes.value(self, Attributes.BLOCK_BREAK_SPEED);
        int haste = Effects.amplifier(self, "minecraft:haste");
        if (haste >= 0) speed *= 1f + 0.2f * (haste + 1);
        int fatigue = Effects.amplifier(self, "minecraft:mining_fatigue");
        if (fatigue >= 0) speed *= switch (Math.min(fatigue, 3)) {
            case 0 -> 0.3f;
            case 1 -> 0.09f;
            case 2 -> 0.0027f;
            default -> 8.1E-4f;
        };
        cir.setReturnValue(speed);
    }
}
