package mc.sayda.scarlet_lib.compat.retrocommands;

import com.periut.retrocommands.util.SharedCommandSource;
import mc.sayda.scarlet_lib.client.LocalCommandSource;
import mc.sayda.scarlet_lib.command.CommandSource;
import mc.sayda.scarlet_lib.command.Commands;
import mc.sayda.scarlet_lib.server.ServerCommandSources;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Retro Commands' command sender as a Scarlet Lib {@link CommandSource}. Replies go through Retro
 * Commands; op status and player lookups come from Scarlet Lib's own source for that side.
 * Retro Commands only runs commands on the client in singleplayer, so the client side is always
 * the singleplayer world owner.
 */
final class RetroSource implements CommandSource {
    private static final boolean SERVER = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;

    private final SharedCommandSource retro;
    private final CommandSource lookups;

    RetroSource(SharedCommandSource retro) {
        this.retro = retro;
        this.lookups = SERVER ? ServerCommandSources.of(retro.getPlayer()) : LocalCommandSource.local();
    }

    @Override
    public String name() {
        return retro.getName();
    }

    @Override
    public boolean isOp() {
        return lookups.isOp();
    }

    @Override
    public @Nullable PlayerEntity self() {
        return retro.getPlayer();
    }

    @Override
    public @Nullable PlayerEntity findPlayer(String name) {
        return lookups.findPlayer(name);
    }

    @Override
    public List<PlayerEntity> onlinePlayers() {
        return lookups.onlinePlayers();
    }

    @Override
    public boolean isSingleplayer() {
        return lookups.isSingleplayer();
    }

    /** Wrapped to chat width; the console gets no colour codes, players no line past beta's chat limit. */
    @Override
    public void reply(String message) {
        boolean console = SERVER && retro.getPlayer() == null;
        for (String line : Commands.wrap(message)) {
            if (console) line = line.replaceAll("§.", "");
            else if (SERVER && line.length() > ServerCommandSources.MAX_CHAT) line = line.substring(0, ServerCommandSources.MAX_CHAT);
            retro.sendFeedback(line);
        }
    }
}
