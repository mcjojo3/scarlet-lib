package mc.sayda.scarlet_lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets the chat key open chat in singleplayer too (beta only allows it on servers), so commands
 * can be typed there. The first isWorldRemote() check in tick() is the chat key's.
 */
@Mixin(Minecraft.class)
abstract class SingleplayerChatMixin {
    @Shadow
    public World world;

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;isWorldRemote()Z", ordinal = 0))
    private boolean scarlet_lib$chatInSingleplayer(boolean remote) {
        return remote || world != null;
    }
}
