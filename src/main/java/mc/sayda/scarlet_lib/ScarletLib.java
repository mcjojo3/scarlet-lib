package mc.sayda.scarlet_lib;

import mc.sayda.scarlet_lib.attribute.Attributes;
import mc.sayda.scarlet_lib.command.AttributeCommand;
import mc.sayda.scarlet_lib.command.Commands;
import mc.sayda.scarlet_lib.command.EffectCommand;
import mc.sayda.scarlet_lib.effect.EffectTypes;
import mc.sayda.scarlet_lib.scale.BodySize;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.event.effect.EntityEffectTypeRegistryEvent;
import net.modificationstation.stationapi.api.event.entity.player.PlayerEvent;
import net.modificationstation.stationapi.api.event.mod.InitEvent;
import net.modificationstation.stationapi.api.event.registry.MessageListenerRegistryEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.EntrypointManager;
import net.modificationstation.stationapi.api.util.Namespace;
import net.minecraft.util.hit.HitResultType;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

/**
 * Scarlet Lib: modern vanilla mechanics for b1.7.3 on StationAPI, usable by any mod.
 * <ul>
 *   <li>{@code effect}: the vanilla status effects, on StationAPI's effect API plus amplifiers.</li>
 *   <li>{@code attribute}: 1.21-style attributes and modifiers, including scale, step height,
 *       jump strength, gravity, fall and reach attributes.</li>
 *   <li>{@code damage}: damage types for beta's typeless damage, with modifier/listener hooks.</li>
 *   <li>{@code data}: an NBT store saved with every entity.</li>
 * </ul>
 */
public final class ScarletLib {
    static {
        EntrypointManager.registerLookup(MethodHandles.lookup());
    }

    public static final String MOD_ID = "scarlet_lib";
    /** Use this, not Namespace.resolve(), which cannot map some dev class layouts to a mod. */
    public static final Namespace NAMESPACE = Namespace.of(MOD_ID);
    public static final Logger LOGGER = NAMESPACE.getLogger();

    @EventListener
    private static void registerEffects(EntityEffectTypeRegistryEvent event) {
        EffectTypes.registerVanilla();
    }

    @EventListener
    private static void init(InitEvent event) {
        Commands.register("effect", Commands.Access.OPERATORS, EffectCommand::run)
                .usage("/effect give <targets> <effect> [seconds] [amplifier]", "/effect clear [targets] [effect]");
        Commands.register("attribute", Commands.Access.OPERATORS, AttributeCommand::run)
                .usage("/attribute <target> <attribute> get [scale]",
                        "/attribute <target> <attribute> base get|set|reset ...",
                        "/attribute <target> <attribute> modifier add|remove|value ...");
    }

    @EventListener
    private static void registerMessages(MessageListenerRegistryEvent event) {
        event.register(BodySize.SYNC_PACKET, BodySize::handlePacket);
        event.register(Attributes.SYNC_PACKET, Attributes::handleSync);
    }

    /**
     * The interaction range attributes, on every reach StationAPI asks about: block and entity
     * reach on the client, and the server's digging and attacking distance checks.
     */
    @EventListener
    private static void onReach(PlayerEvent.Reach event) {
        if (event.player == null) return;
        var attribute = event.type == HitResultType.ENTITY ? Attributes.ENTITY_INTERACTION_RANGE
                : Attributes.BLOCK_INTERACTION_RANGE;
        event.currentReach *= Attributes.ratio(event.player, attribute);
    }
}
