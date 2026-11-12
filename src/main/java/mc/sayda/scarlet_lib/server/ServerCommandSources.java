package mc.sayda.scarlet_lib.server;

import mc.sayda.scarlet_lib.command.CommandSource;
import mc.sayda.scarlet_lib.command.Commands;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.play.ChatMessagePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Command senders on a dedicated server: a player in chat, or the console. Server-only. */
public final class ServerCommandSources {
    /** Beta clients refuse chat packets longer than this. */
    public static final int MAX_CHAT = 119;

    private ServerCommandSources() {
    }

    /**
     * For a command another mod dispatched (Retro Commands): the player who ran it, or the console
     * when {@code player} is null. That mod sends the replies, so only the op status and player
     * lookups of this source are used.
     */
    public static CommandSource of(@Nullable PlayerEntity player) {
        MinecraftServer server = (MinecraftServer) FabricLoader.getInstance().getGameInstance();
        if (player instanceof ServerPlayerEntity serverPlayer) return player(server, serverPlayer);
        return console(server, server); // the server is the console's output
    }

    public static CommandSource player(MinecraftServer server, ServerPlayerEntity player) {
        return new Base(server) {
            @Override
            public String name() {
                return player.name;
            }

            @Override
            public boolean isOp() {
                return server.playerManager.isOperator(player.name);
            }

            @Override
            public @Nullable PlayerEntity self() {
                return player;
            }

            @Override
            public void reply(String message) {
                if (player.networkHandler == null) return;
                for (String line : Commands.wrap(message)) {
                    String text = line.length() > MAX_CHAT ? line.substring(0, MAX_CHAT) : line;
                    player.networkHandler.sendPacket(new ChatMessagePacket(text));
                }
            }
        };
    }

    public static CommandSource console(MinecraftServer server, CommandOutput output) {
        return new Base(server) {
            @Override
            public String name() {
                return output.getName();
            }

            @Override
            public boolean isOp() {
                return true;
            }

            @Override
            public @Nullable PlayerEntity self() {
                return null;
            }

            @Override
            public void reply(String message) {
                output.sendMessage(message.replaceAll("§.", ""));
            }
        };
    }

    private abstract static class Base implements CommandSource {
        final MinecraftServer server;

        Base(MinecraftServer server) {
            this.server = server;
        }

        @Override
        public @Nullable PlayerEntity findPlayer(String name) {
            return server.playerManager.getPlayer(name);
        }

        @Override
        public List<PlayerEntity> onlinePlayers() {
            List<PlayerEntity> players = new ArrayList<>();
            for (Object o : server.playerManager.players) players.add((PlayerEntity) o);
            return players;
        }

        @Override
        public boolean isSingleplayer() {
            return false;
        }
    }
}
