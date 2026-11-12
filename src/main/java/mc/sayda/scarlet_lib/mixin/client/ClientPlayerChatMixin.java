package mc.sayda.scarlet_lib.mixin.client;

import mc.sayda.scarlet_lib.client.LocalCommandSource;
import mc.sayda.scarlet_lib.command.Commands;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Singleplayer chat. Beta's ClientPlayerEntity drops chat (only the multiplayer subclass sends it),
 * so registered commands run here against the local game, and plain messages are shown back to
 * the player. Unregistered commands are left alone for other mods.
 */
@Mixin(ClientPlayerEntity.class)
abstract class ClientPlayerChatMixin {
    @Shadow
    protected Minecraft minecraft;

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void scarlet_lib$singleplayerChat(String message, CallbackInfo ci) {
        // With Retro Commands installed, it handles singleplayer chat (commands and the echo)
        if (Commands.isDelegated() || minecraft == null || minecraft.world == null || minecraft.world.isRemote) return;
        if (message.startsWith("/")) {
            if (Commands.dispatch(new LocalCommandSource(minecraft), message.substring(1))) ci.cancel();
        } else if (minecraft.inGameHud != null) {
            minecraft.inGameHud.addChatMessage("<" + minecraft.player.name + "> " + message);
        }
    }
}
