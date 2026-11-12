package mc.sayda.scarlet_lib.mixin.client;

import mc.sayda.scarlet_lib.client.ScarletLibClient;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.network.packet.play.PlayerMovePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A server teleport (joining, respawning, getting into bed) sends the player's position with
 * their eyes at feet + 1.62, and the client puts its feet at that minus its own eye height. For a
 * resized player those differ, so the position is converted to this player's eye height first;
 * the reply to the server then reports the right feet as well.
 */
@Mixin(ClientNetworkHandler.class)
abstract class ClientNetworkHandlerMixin {
    @Inject(method = "onPlayerMove", at = @At("HEAD"))
    private void scarlet_lib$ownEyeHeight(PlayerMovePacket packet, CallbackInfo ci) {
        ClientPlayerEntity player = ScarletLibClient.minecraft().player;
        if (player == null || !packet.changePosition) return;
        packet.y = packet.y - (double) 1.62F + player.standingEyeHeight;
    }
}
