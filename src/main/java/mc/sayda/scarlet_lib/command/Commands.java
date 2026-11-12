package mc.sayda.scarlet_lib.command;

import mc.sayda.scarlet_lib.ScarletLib;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Slash commands for beta, which has no command framework: Scarlet Lib hands typed commands to
 * whatever is registered here from singleplayer chat (which it also enables), server chat and the
 * server console. Unregistered commands are left to vanilla and other mods. There are no
 * selectors beyond @s, @p and @a, and no tab completion.
 *
 * <p>With Retro Commands installed, Retro Commands runs every command instead and Scarlet Lib's
 * chat and console hooks stand down: registered commands are added to it (see
 * RetroCommandsBridge), so they follow its permissions, /help and disabled-commands config.
 */
public final class Commands {
    @FunctionalInterface
    public interface Command {
        /** {@code args} are the words after the command's name. */
        void run(CommandSource source, String[] args);
    }

    /** Who may run a command. */
    public enum Access {
        EVERYONE,
        /** Operators, the console and the singleplayer world owner. */
        OPERATORS
    }

    /** A registered command: its name, who may run it and its usage lines (for help). */
    public static final class Registration {
        private final String name;
        private final Access access;
        private final Command command;
        private List<String> usage = List.of();

        private Registration(String name, Access access, Command command) {
            this.name = name;
            this.access = access;
            this.command = command;
        }

        /** The usage lines /help shows for this command, e.g. "/heal [player]". */
        public Registration usage(String... lines) {
            this.usage = List.of(lines);
            return this;
        }

        public String name() {
            return name;
        }

        public Access access() {
            return access;
        }

        public Command command() {
            return command;
        }

        public List<String> usage() {
            return usage.isEmpty() ? List.of("/" + name) : usage;
        }
    }

    private static final Map<String, Registration> COMMANDS = new LinkedHashMap<>();
    /** Set when another command mod runs the commands; Scarlet Lib's own hooks then stand down. */
    @Nullable
    private static Consumer<Registration> delegate;

    private Commands() {
    }

    /** Registers /{@code name} for everyone; call from your init. */
    public static Registration register(String name, Command command) {
        return register(name, Access.EVERYONE, command);
    }

    /** Registers /{@code name}; call from your init. */
    public static Registration register(String name, Access access, Command command) {
        Registration registration = new Registration(name.toLowerCase(Locale.ROOT), access, command);
        COMMANDS.put(registration.name(), registration);
        if (delegate != null) delegate.accept(registration);
        return registration;
    }

    public static Set<String> names() {
        return Collections.unmodifiableSet(COMMANDS.keySet());
    }

    @Nullable
    public static Registration get(String name) {
        return COMMANDS.get(name.toLowerCase(Locale.ROOT));
    }

    public static Collection<Registration> all() {
        return Collections.unmodifiableCollection(COMMANDS.values());
    }

    /**
     * Hands command dispatch to another command mod: {@code delegate} gets every registration, now
     * and later, and Scarlet Lib stops reading chat and console commands itself.
     */
    public static void delegateTo(Consumer<Registration> delegate) {
        Commands.delegate = delegate;
        for (Registration registration : List.copyOf(COMMANDS.values())) delegate.accept(registration);
    }

    /** True when another command mod runs the commands (Scarlet Lib's chat and console hooks then do nothing). */
    public static boolean isDelegated() {
        return delegate != null;
    }

    /**
     * Runs {@code text} (without the leading slash) if it names a registered command; returns false
     * otherwise, so the caller can leave it to vanilla.
     */
    public static boolean dispatch(CommandSource source, String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return false;
        Registration registration = COMMANDS.get(parts[0].toLowerCase(Locale.ROOT));
        if (registration == null) return false;
        if (registration.access() == Access.OPERATORS && !requireOp(source, registration.name())) return true;
        run(registration, source, Arrays.copyOfRange(parts, 1, parts.length));
        return true;
    }

    /** Runs a command, reporting a crash in it to the sender instead of passing it on. */
    public static void run(Registration registration, CommandSource source, String[] args) {
        try {
            registration.command().run(source, args);
        } catch (RuntimeException e) {
            ScarletLib.LOGGER.error("/{} failed", registration.name(), e);
            source.reply("§cThat command failed: " + e.getClass().getSimpleName());
        }
    }

    // ------------------------------------------------------------------ helpers for commands

    /** Operators only (vanilla's permission level 2 commands); tells the sender otherwise. */
    public static boolean requireOp(CommandSource source, String command) {
        if (source.isOp()) return true;
        source.reply("§cYou need to be an operator to use /" + command + ".");
        return false;
    }

    /**
     * The players a target argument names: a player name, @s or @p (the sender) or @a (everyone
     * online). Tells the sender and returns an empty list when nobody matches.
     */
    public static List<PlayerEntity> targets(CommandSource source, String arg) {
        List<PlayerEntity> result = new ArrayList<>();
        String a = arg.toLowerCase(Locale.ROOT);
        if (a.equals("@s") || a.equals("@p")) {
            if (source.self() != null) result.add(source.self());
            else source.reply("§cName a player: the console has no player of its own.");
        } else if (a.equals("@a")) {
            result.addAll(source.onlinePlayers());
            if (result.isEmpty()) source.reply("§cNo player was found");
        } else if (a.startsWith("@")) {
            source.reply("§cOnly @s, @p and @a are supported here.");
        } else {
            PlayerEntity player = source.findPlayer(arg);
            if (player != null) result.add(player);
            else source.reply("§cNo player was found");
        }
        return result;
    }

    /** "speed" means minecraft:speed. */
    public static String withNamespace(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        return lower.contains(":") ? lower : "minecraft:" + lower;
    }

    /** A number for a command argument, or null (after telling the sender) if it is not one. */
    public static Double number(CommandSource source, String arg) {
        try {
            return Double.parseDouble(arg);
        } catch (NumberFormatException e) {
            source.reply("§cExpected a number, got '" + arg + "'");
            return null;
        }
    }

    // ------------------------------------------------------------------ chat formatting

    /** Beta's chat wraps at 320 pixels; a little less leaves room for its own margin. */
    private static final int CHAT_WIDTH = 312;

    /**
     * Splits a reply into chat lines that fit beta's chat, at spaces. Beta wraps long lines itself
     * but loses the colour on the continuation; here each wrapped line starts with the colour in
     * effect. Works on a server too (widths are estimated from beta's default font).
     */
    public static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        int width = 0;
        String color = "";
        for (String word : text.split(" ")) {
            int w = width(word);
            if (width > 0 && width + width(" ") + w > CHAT_WIDTH) {
                lines.add(line.toString());
                line = new StringBuilder(color);
                width = 0;
            }
            if (width > 0) {
                line.append(' ');
                width += width(" ");
            }
            line.append(word);
            width += w;
            color = lastColor(word, color);
        }
        lines.add(line.toString());
        return lines;
    }

    /** Pixel width of text in beta's default font, ignoring § codes. */
    private static int width(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            width += switch (c) {
                case 'i', '!', '.', ',', ':', ';', '|', '\'' -> 2;
                case 'l', '`' -> 3;
                case 'I', 't', '[', ']', ' ' -> 4;
                case 'f', 'k', '<', '>', '(', ')', '{', '}', '*', '"' -> 5;
                default -> 6;
            };
        }
        return width;
    }

    private static String lastColor(String text, String current) {
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != '§') continue;
            char code = Character.toLowerCase(text.charAt(i + 1));
            if (code == 'r') current = "";
            else if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) current = "§" + code;
        }
        return current;
    }

    /** Short display of a value: no trailing ".0" for whole numbers. */
    public static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0E9) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
