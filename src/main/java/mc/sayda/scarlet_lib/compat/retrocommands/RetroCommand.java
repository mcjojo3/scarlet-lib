package mc.sayda.scarlet_lib.compat.retrocommands;

import com.periut.retrocommands.api.Command;
import com.periut.retrocommands.util.SharedCommandSource;
import mc.sayda.scarlet_lib.command.Commands;

import java.util.Arrays;

/**
 * A command registered through Scarlet Lib, as Retro Commands sees it. It looks the registration
 * up by name each time it runs, so registering the name again replaces what runs.
 */
final class RetroCommand implements Command {
    private final String name;

    RetroCommand(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    /** Retro Commands keeps these from non-operators (they get its "not found" reply). */
    @Override
    public boolean needsPermissions() {
        Commands.Registration registration = Commands.get(name);
        return registration != null && registration.access() == Commands.Access.OPERATORS;
    }

    @Override
    public void command(SharedCommandSource source, String[] parameters) {
        Commands.Registration registration = Commands.get(name);
        if (registration == null) return;
        // Retro Commands passes the command's own name first, and splits on single spaces
        String[] args = Arrays.stream(parameters).skip(1).filter(word -> !word.isEmpty()).toArray(String[]::new);
        Commands.run(registration, new RetroSource(source), args);
    }

    /** /help &lt;name&gt; */
    @Override
    public void manual(SharedCommandSource source) {
        Commands.Registration registration = Commands.get(name);
        if (registration == null) return;
        RetroSource reply = new RetroSource(source);
        for (String line : registration.usage()) reply.reply("§eUsage: §f" + line);
    }
}
