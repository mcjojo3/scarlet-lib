package mc.sayda.scarlet_lib.command;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Who ran a command: the local player in singleplayer, a player on a server, or the server
 * console. Implementations live on their own side, so common code never loads the other side's
 * classes.
 */
public interface CommandSource {
    String name();

    /** Server operators and the console; the singleplayer world owner counts as one. */
    boolean isOp();

    /** The player who ran the command, or null for the console. */
    @Nullable
    PlayerEntity self();

    /** An online player by name (case-insensitive), or null. */
    @Nullable
    PlayerEntity findPlayer(String name);

    List<PlayerEntity> onlinePlayers();

    /** One chat line; may contain § colour codes. */
    void reply(String message);

    /** Singleplayer: the client and the game share one world, with no server in between. */
    boolean isSingleplayer();
}
