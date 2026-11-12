package mc.sayda.scarlet_lib.command;

import mc.sayda.scarlet_lib.attribute.Attribute;
import mc.sayda.scarlet_lib.attribute.AttributeModifier;
import mc.sayda.scarlet_lib.attribute.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.List;
import java.util.Locale;

/**
 * 1.21's /attribute for players:
 * <pre>
 * /attribute &lt;target&gt; &lt;attribute&gt; get [scale]
 * /attribute &lt;target&gt; &lt;attribute&gt; base get [scale]
 * /attribute &lt;target&gt; &lt;attribute&gt; base set &lt;value&gt;
 * /attribute &lt;target&gt; &lt;attribute&gt; base reset
 * /attribute &lt;target&gt; &lt;attribute&gt; modifier add &lt;id&gt; &lt;value&gt; &lt;add_value|add_multiplied_base|add_multiplied_total&gt;
 * /attribute &lt;target&gt; &lt;attribute&gt; modifier remove &lt;id&gt;
 * /attribute &lt;target&gt; &lt;attribute&gt; modifier value get &lt;id&gt; [scale]
 * </pre>
 * Attributes by id: minecraft:generic.scale, generic.scale, scale and 1.21.2's minecraft:scale all
 * work. Base values and modifiers set here are saved with the player.
 */
public final class AttributeCommand {
    private AttributeCommand() {
    }

    public static void run(CommandSource source, String[] args) {
        if (args.length < 3) {
            usage(source);
            return;
        }
        List<PlayerEntity> targets = Commands.targets(source, args[0]);
        if (targets.isEmpty()) return;
        if (targets.size() > 1) {
            source.reply("§cOnly one entity is allowed, but the provided selector allows more than one");
            return;
        }
        PlayerEntity target = targets.get(0);
        Attribute attribute = Attributes.get(args[1]);
        if (attribute == null) {
            source.reply("§cUnknown attribute: " + args[1]);
            return;
        }
        String name = attribute.id().toString();
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "get" -> {
                double value = Attributes.value(target, attribute) * scale(source, args, 3);
                source.reply("Value of attribute " + name + " for entity " + target.name + " is " + Commands.format(value));
            }
            case "base" -> base(source, args, target, attribute);
            case "modifier" -> modifier(source, args, target, attribute);
            default -> usage(source);
        }
    }

    private static void base(CommandSource source, String[] args, PlayerEntity target, Attribute attribute) {
        String name = attribute.id().toString();
        String op = args.length > 3 ? args[3].toLowerCase(Locale.ROOT) : "";
        switch (op) {
            case "get" -> {
                double value = Attributes.baseValue(target, attribute) * scale(source, args, 4);
                source.reply("Base value of attribute " + name + " for entity " + target.name + " is " + Commands.format(value));
            }
            case "set" -> {
                if (args.length < 5) {
                    usageBase(source);
                    return;
                }
                Double value = Commands.number(source, args[4]);
                if (value == null) return;
                Attributes.setBaseValue(target, attribute, value);
                source.reply("Base value for attribute " + name + " for entity " + target.name + " set to "
                        + Commands.format(attribute.clamp(value)));
            }
            case "reset" -> {
                Attributes.resetBaseValue(target, attribute);
                source.reply("Base value for attribute " + name + " for entity " + target.name + " reset to default "
                        + Commands.format(attribute.defaultValue()));
            }
            default -> usageBase(source);
        }
    }

    private static void modifier(CommandSource source, String[] args, PlayerEntity target, Attribute attribute) {
        String name = attribute.id().toString();
        String op = args.length > 3 ? args[3].toLowerCase(Locale.ROOT) : "";
        switch (op) {
            case "add" -> {
                if (args.length < 7) {
                    usageModifier(source);
                    return;
                }
                Identifier id = Identifier.tryParse(Commands.withNamespace(args[4]));
                Double value = Commands.number(source, args[5]);
                if (id == null || value == null) return;
                AttributeModifier.Operation operation = operation(args[6]);
                if (operation == null) {
                    source.reply("§cUnknown operation '" + args[6] + "': add_value, add_multiplied_base or add_multiplied_total");
                    return;
                }
                if (Attributes.getModifier(target, attribute, id) != null) {
                    source.reply("§cModifier " + id + " is already present on attribute " + name + " for entity " + target.name);
                    return;
                }
                Attributes.addModifier(target, attribute, new AttributeModifier(id, value, operation));
                source.reply("Added modifier " + id + " to attribute " + name + " for entity " + target.name);
            }
            case "remove" -> {
                if (args.length < 5) {
                    usageModifier(source);
                    return;
                }
                Identifier id = Identifier.tryParse(Commands.withNamespace(args[4]));
                if (id == null || Attributes.getModifier(target, attribute, id) == null) {
                    source.reply("§cNo modifier with id " + args[4] + " on attribute " + name + " for entity " + target.name);
                    return;
                }
                Attributes.removeModifier(target, attribute, id);
                source.reply("Removed modifier " + id + " from attribute " + name + " for entity " + target.name);
            }
            case "value" -> {
                if (args.length < 6 || !args[4].equalsIgnoreCase("get")) {
                    usageModifier(source);
                    return;
                }
                Identifier id = Identifier.tryParse(Commands.withNamespace(args[5]));
                AttributeModifier modifier = id == null ? null : Attributes.getModifier(target, attribute, id);
                if (modifier == null) {
                    source.reply("§cNo modifier with id " + args[5] + " on attribute " + name + " for entity " + target.name);
                    return;
                }
                source.reply("Value of modifier " + id + " on attribute " + name + " for entity " + target.name + " is "
                        + Commands.format(modifier.value() * scale(source, args, 6)));
            }
            default -> usageModifier(source);
        }
    }

    private static AttributeModifier.Operation operation(String arg) {
        return switch (arg.toLowerCase(Locale.ROOT)) {
            case "add_value", "add", "addition" -> AttributeModifier.Operation.ADD_VALUE;
            case "add_multiplied_base", "multiply_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total", "multiply_total", "multiply" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> null;
        };
    }

    /** The optional [scale] argument at {@code index} (1 when absent or not a number). */
    private static double scale(CommandSource source, String[] args, int index) {
        if (args.length <= index) return 1;
        Double scale = Commands.number(source, args[index]);
        return scale == null ? 1 : scale;
    }

    private static void usage(CommandSource source) {
        source.reply("§cUsage: /attribute <target> <attribute> <action>");
        source.reply("§7Actions: get [scale], base, modifier");
    }

    private static void usageBase(CommandSource source) {
        source.reply("§cUsage: /attribute <target> <attribute> base ...");
        source.reply("§7get [scale], set <value>, reset");
    }

    private static void usageModifier(CommandSource source) {
        source.reply("§cUsage: /attribute <target> <attribute> modifier ...");
        source.reply("§7add <id> <value> <operation>, remove <id>, value get <id> [scale]");
        source.reply("§7Operations: add_value, add_multiplied_base, add_multiplied_total");
    }
}
