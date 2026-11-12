package mc.sayda.scarlet_lib.client;

import mc.sayda.scarlet_lib.command.CommandSource;
import mc.sayda.scarlet_lib.command.Commands;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The singleplayer player running a command. Beta singleplayer has no server, ops or cheats
 * setting, so the world owner may use every command.
 */
public final class LocalCommandSource implements CommandSource {
    private final Minecraft mc;

    public LocalCommandSource(Minecraft mc) {
        this.mc = mc;
    }

    /** The singleplayer player, for a command another mod dispatched (Retro Commands). */
    public static CommandSource local() {
        return new LocalCommandSource(ScarletLibClient.minecraft());
    }

    @Override
    public String name() {
        return mc.player == null ? "player" : mc.player.name;
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public @Nullable PlayerEntity self() {
        return mc.player;
    }

    @Override
    public @Nullable PlayerEntity findPlayer(String name) {
        return mc.player != null && mc.player.name.equalsIgnoreCase(name) ? mc.player : null;
    }

    @Override
    public List<PlayerEntity> onlinePlayers() {
        return mc.player == null ? List.of() : List.of(mc.player);
    }

    @Override
    public void reply(String message) {
        if (mc.inGameHud == null) return;
        for (String line : Commands.wrap(message)) mc.inGameHud.addChatMessage(line);
    }

    @Override
    public boolean isSingleplayer() {
        return true;
    }
}
