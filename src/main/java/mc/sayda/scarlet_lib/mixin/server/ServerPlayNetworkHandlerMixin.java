package mc.sayda.scarlet_lib.mixin.server;

import mc.sayda.scarlet_lib.attribute.Attributes;
import mc.sayda.scarlet_lib.command.Commands;
import mc.sayda.scarlet_lib.scale.BodySize;
import mc.sayda.scarlet_lib.server.ServerCommandSources;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.play.PlayerMovePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The server's movement checks, adjusted for body size: the stance check (eyes minus feet must
 * be between 0.1 and 1.65), the teleport confirmation and the 8-block limit on using blocks.
 * Digging and attacking range go through StationAPI's reach event, see ScarletLib. Also hands
 * commands typed in chat to {@link Commands}.
 */
@Mixin(ServerPlayNetworkHandler.class)
abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    private ServerPlayerEntity player;
    @Shadow
    private MinecraftServer server;
    @Shadow
    private boolean teleported;
    @Shadow
    private double teleportTargetX;
    @Shadow
    private double teleportTargetY;
    @Shadow
    private double teleportTargetZ;

    /**
     * Largest recent eye scale and when it was seen. Right after shrinking, packets already on
     * their way still carry the taller stance, so the old limit holds for two seconds.
     */
    @Unique
    private float scarlet_lib$recentEye = 1.0F;
    @Unique
    private int scarlet_lib$recentEyeAge;

    /**
     * After a teleport, beta ignores a player's movement until their client echoes the target back.
     * It compares the echoed feet; this also accepts the echoed eye position (the teleport's y plus
     * 1.62), which matches however the client measured its feet.
     */
    @Inject(method = "onPlayerMove", at = @At("HEAD"))
    private void scarlet_lib$confirmTeleport(PlayerMovePacket packet, CallbackInfo ci) {
        if (teleported || player == null || !packet.changePosition) return;
        if (packet.x == teleportTargetX && packet.z == teleportTargetZ
                && Math.abs(packet.eyeHeight - (teleportTargetY + (double) 1.62F)) < 0.1) {
            teleported = true;
        }
    }

    /** Registered commands from chat (unless Retro Commands runs them). */
    @Inject(method = "handleCommand", at = @At("HEAD"), cancellable = true)
    private void scarlet_lib$command(String message, CallbackInfo ci) {
        if (player == null || message.length() < 2 || Commands.isDelegated()) return;
        if (Commands.dispatch(ServerCommandSources.player(server, player), message.substring(1))) ci.cancel();
    }

    @ModifyConstant(method = "onPlayerMove", constant = @Constant(doubleValue = 1.65))
    private double scarlet_lib$stanceMax(double vanillaMax) {
        if (player == null) return vanillaMax;
        float eye = BodySize.current(player).eye();
        if (eye >= scarlet_lib$recentEye || player.age - scarlet_lib$recentEyeAge > 40) {
            scarlet_lib$recentEye = eye;
            scarlet_lib$recentEyeAge = player.age;
        }
        return Math.max(vanillaMax, BodySize.PLAYER_EYE * Math.max(eye, scarlet_lib$recentEye) + 0.05);
    }

    /**
     * The low end. Stepping up adds the step to the client's camera offset, which lowers its eyes
     * relative to its feet until that decays (x0.4 a tick): a small player (eyes at 0.405) stepping
     * onto a bed reports 0.405 - 0.51. Allow what the eye and step heights can produce; vanilla
     * sizes keep 0.1.
     */
    @ModifyConstant(method = "onPlayerMove", constant = @Constant(doubleValue = 0.1))
    private double scarlet_lib$stanceMin(double vanillaMin) {
        if (player == null) return vanillaMin;
        double eye = BodySize.PLAYER_EYE * Math.min(BodySize.current(player).eye(), scarlet_lib$recentEye);
        double steppedOffset = (Math.max(player.stepHeight, 0.5) + 0.01) / 0.6; // repeated steps, decaying x0.4
        return Math.min(vanillaMin, eye - steppedOffset - 0.05);
    }

    /** Using a block is allowed within 8 blocks (64 squared); longer reach widens that. */
    @ModifyConstant(method = "onPlayerInteractBlock", constant = @Constant(doubleValue = 64.0))
    private double scarlet_lib$useReach(double vanilla) {
        if (player == null) return vanilla;
        double ratio = Math.max(1, Attributes.ratio(player, Attributes.BLOCK_INTERACTION_RANGE));
        return vanilla * ratio * ratio;
    }
}
