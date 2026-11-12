package mc.sayda.scarlet_lib.compat.retrocommands;

import com.periut.retrocommands.api.Command;
import com.periut.retrocommands.api.CommandRegistry;
import com.periut.retrocommands.command.vanilla.Help;
import com.periut.retrocommands.util.RetroChatUtil;
import mc.sayda.scarlet_lib.ScarletLib;
import mc.sayda.scarlet_lib.command.Commands;

/**
 * Retro Commands integration. With Retro Commands installed it is the master: it reads every
 * command typed in chat or the console, checks permissions, lists /help and applies its own
 * disabled-commands config. Commands registered through Scarlet Lib's {@link Commands} are added
 * to it as Retro Commands commands, and Scarlet Lib's own chat and console hooks stand down.
 *
 * <p>Uses only Retro Commands' public classes: commands go in through CommandRegistry.add and
 * their help lines through Help.addHelpTip; its command list is only read, to leave its own
 * commands alone when a name is taken.
 *
 * <p>Only loaded when Retro Commands is installed.
 */
public final class RetroCommandsBridge {
    private RetroCommandsBridge() {
    }

    /**
     * Called from Scarlet Lib's main entrypoint. Fabric Loader starts mods in mod id order, so
     * Retro Commands ("retrocommands") has registered its own commands and built its /help pages
     * by now, and ours go in after them.
     */
    public static void install() {
        Commands.delegateTo(RetroCommandsBridge::add);
        ScarletLib.LOGGER.info("Retro Commands is installed: it runs the commands, Scarlet Lib's are added to it");
    }

    private static void add(Commands.Registration registration) {
        for (Command existing : RetroChatUtil.commands) {
            if (!registration.name().equals(existing.name())) continue;
            // Re-registering one of ours: the existing command looks the registration up when it runs
            if (!(existing instanceof RetroCommand)) {
                ScarletLib.LOGGER.warn("Retro Commands already has /{}, so it keeps its own and the one registered through Scarlet Lib is left out",
                        registration.name());
            }
            return;
        }
        CommandRegistry.add(new RetroCommand(registration.name()));
        Help.addHelpTip("/" + registration.name(), registration.access() == Commands.Access.OPERATORS);
    }
}
