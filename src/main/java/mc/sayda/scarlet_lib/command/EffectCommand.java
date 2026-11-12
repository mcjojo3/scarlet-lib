package mc.sayda.scarlet_lib.command;

import mc.sayda.scarlet_lib.effect.EffectTypes;
import mc.sayda.scarlet_lib.effect.Effects;
import net.minecraft.entity.player.PlayerEntity;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.List;
import java.util.Locale;

/**
 * 1.21's /effect for players:
 * <pre>
 * /effect give &lt;targets&gt; &lt;effect&gt; [seconds|infinite] [amplifier]   ("add" works too)
 * /effect clear [targets] [effect]
 * </pre>
 * Any amplified effect works, so other mods' effects too (namespace:path; a bare name is minecraft:).
 */
public final class EffectCommand {
    private EffectCommand() {
    }

    public static void run(CommandSource source, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give", "add" -> give(source, args);
            case "clear" -> clear(source, args);
            default -> usage(source);
        }
    }

    private static void usage(CommandSource source) {
        source.reply("§cUsage:");
        source.reply("§c/effect give <targets> <effect> [seconds] [amplifier]");
        source.reply("§c/effect clear [targets] [effect]");
        source.reply("§7Seconds can also be \"infinite\".");
    }

    private static void give(CommandSource source, String[] args) {
        if (args.length < 3) {
            usage(source);
            return;
        }
        Identifier effect = effect(source, args[2]);
        if (effect == null) return;
        int ticks = 30 * 20;
        if (args.length > 3) {
            if (args[3].equalsIgnoreCase("infinite")) {
                ticks = -1;
            } else {
                Double seconds = Commands.number(source, args[3]);
                if (seconds == null) return;
                if (seconds < 1 || seconds > 1_000_000) {
                    source.reply("§cSeconds must be between 1 and 1000000 (or infinite)");
                    return;
                }
                ticks = (int) Math.round(seconds * 20);
            }
        }
        int amplifier = 0;
        if (args.length > 4) {
            Double amp = Commands.number(source, args[4]);
            if (amp == null) return;
            if (amp < 0 || amp > 255) {
                source.reply("§cThe amplifier must be between 0 and 255");
                return;
            }
            amplifier = amp.intValue();
        }
        List<PlayerEntity> targets = Commands.targets(source, args[1]);
        if (targets.isEmpty()) return;
        int applied = 0;
        PlayerEntity last = null;
        for (PlayerEntity target : targets) {
            if (Effects.apply(target, effect, ticks, amplifier)) {
                applied++;
                last = target;
            }
        }
        if (applied == 0) {
            source.reply("§cUnable to apply this effect (target is either immune to effects, or has something stronger)");
        } else if (applied == 1) {
            source.reply("Applied effect " + effect + " to " + last.name);
        } else {
            source.reply("Applied effect " + effect + " to " + applied + " targets");
        }
    }

    private static void clear(CommandSource source, String[] args) {
        List<PlayerEntity> targets = args.length > 1 ? Commands.targets(source, args[1]) : Commands.targets(source, "@s");
        if (targets.isEmpty()) return;
        Identifier effect = null;
        if (args.length > 2) {
            effect = effect(source, args[2]);
            if (effect == null) return;
        }
        int cleared = 0;
        PlayerEntity last = null;
        for (PlayerEntity target : targets) {
            if (effect == null) {
                if (Effects.all(target).isEmpty()) continue;
                Effects.removeAll(target);
            } else {
                if (!Effects.has(target, effect)) continue;
                Effects.remove(target, effect);
            }
            cleared++;
            last = target;
        }
        if (cleared == 0) {
            source.reply("§cTarget has no effects to remove");
        } else if (effect == null) {
            source.reply(cleared == 1 ? "Removed every effect from " + last.name : "Removed every effect from " + cleared + " targets");
        } else {
            source.reply(cleared == 1 ? "Removed effect " + effect + " from " + last.name
                    : "Removed effect " + effect + " from " + cleared + " targets");
        }
    }

    private static Identifier effect(CommandSource source, String arg) {
        Identifier id = Identifier.tryParse(Commands.withNamespace(arg));
        if (id == null || EffectTypes.get(id) == null) {
            source.reply("§cUnknown effect: " + arg);
            return null;
        }
        return id;
    }
}
