package mc.sayda.scarlet_lib.mixin.server;

import mc.sayda.scarlet_lib.command.Commands;
import mc.sayda.scarlet_lib.server.ServerCommandSources;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.Command;
import net.minecraft.server.command.ServerCommandHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registered commands typed into the server console (players' are caught earlier, in chat). */
@Mixin(ServerCommandHandler.class)
abstract class ServerCommandHandlerMixin {
    @Shadow
    private MinecraftServer server;

    @Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true)
    private void scarlet_lib$command(Command command, CallbackInfo ci) {
        String text = command.commandAndArgs;
        if (text != null && !Commands.isDelegated() && Commands.dispatch(ServerCommandSources.console(server, command.output), text)) ci.cancel();
    }
}
