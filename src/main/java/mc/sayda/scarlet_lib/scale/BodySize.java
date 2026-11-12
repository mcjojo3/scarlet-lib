package mc.sayda.scarlet_lib.scale;

import mc.sayda.scarlet_lib.ScarletLib;
import mc.sayda.scarlet_lib.attribute.Attributes;
import mc.sayda.scarlet_lib.mixin.EntityInvoker;
import net.minecraft.entity.player.PlayerEntity;
import net.modificationstation.stationapi.api.network.packet.MessagePacket;
import net.modificationstation.stationapi.api.network.packet.PacketHelper;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player size: the generic.scale attribute times whatever {@link Provider}s mods register (for
 * width/height-only scaling, a separate hitbox, ...). Applied every tick to the hitbox, the eye
 * height (and so the camera) and the rendered model, plus the things that follow from size:
 * fluid detection, item pickup reach, bed reach and the server's stance check all use the
 * resized body. Other clients get the render size in scarlet_lib:size.
 */
public final class BodySize {
    public static final Identifier SYNC_PACKET = ScarletLib.NAMESPACE.id("size");
    public static final float PLAYER_WIDTH = 0.6F, PLAYER_HEIGHT = 1.8F, PLAYER_EYE = 1.62F;

    private static final List<Provider> PROVIDERS = new ArrayList<>();
    /** Other players' sizes on a multiplayer client, by entity id, as their server sent them. */
    private static final Map<Integer, Size> REMOTE = new ConcurrentHashMap<>();
    private static final Map<PlayerEntity, String> LAST_SENT = new WeakHashMap<>();
    /** Client: set while a GUI preview (the inventory's player) is drawn, which shows full size. */
    public static boolean guiPreview;
    /** Each player's size as of their last tick. */
    private static final Map<PlayerEntity, Size> CURRENT = new WeakHashMap<>();

    /** Multipliers on a vanilla player's size. */
    public record Size(float hitboxWidth, float hitboxHeight, float eye, float renderWidth, float renderHeight) {
        public static final Size DEFAULT = new Size(1, 1, 1, 1, 1);

        public static Size uniform(float scale) {
            return new Size(scale, scale, scale, scale, scale);
        }

        public Size times(Size o) {
            return new Size(hitboxWidth * o.hitboxWidth, hitboxHeight * o.hitboxHeight, eye * o.eye,
                    renderWidth * o.renderWidth, renderHeight * o.renderHeight);
        }
    }

    /** A mod's contribution to a player's size (Size.DEFAULT for none). */
    @FunctionalInterface
    public interface Provider {
        Size size(PlayerEntity player);
    }

    private BodySize() {
    }

    public static void addProvider(Provider provider) {
        PROVIDERS.add(provider);
    }

    public static Size compute(PlayerEntity player) {
        // Another player on a multiplayer client (their y is at their feet, so no eye height): this
        // client has none of their data, so it uses the size their server sent.
        if (player.world != null && player.world.isRemote && player.standingEyeHeight == 0) {
            Size remote = REMOTE.get(player.id);
            return remote != null ? remote : Size.DEFAULT;
        }
        Size size = Size.uniform((float) Attributes.value(player, Attributes.SCALE));
        for (Provider provider : PROVIDERS) size = size.times(provider.size(player));
        return size;
    }

    /** The player's size as of their last tick (computed now if they have not ticked yet). */
    public static Size current(PlayerEntity player) {
        Size size = CURRENT.get(player);
        return size != null ? size : compute(player);
    }

    /** Height relative to a vanilla player (1.0 = normal). */
    public static float heightScale(PlayerEntity player) {
        return player.height / PLAYER_HEIGHT;
    }

    /** Eye height above the feet (1.62 for a vanilla player). */
    public static float eyeHeight(PlayerEntity player) {
        return Math.max(0.1F, PLAYER_EYE * current(player).eye());
    }

    /**
     * How far one stride goes, relative to a vanilla player: the model's height, since legs scale
     * with it. Walking animation, footsteps and view bobbing are paced by distance / stride, so a
     * quarter-size body's legs move at a natural rate rather than in slow motion.
     */
    public static float stride(PlayerEntity player) {
        return Math.max(0.1F, current(player).renderHeight());
    }

    /** Once per tick, both sides: resizes the hitbox and (client players) the eye height. */
    public static void apply(PlayerEntity player) {
        float step = (float) Attributes.value(player, Attributes.STEP_HEIGHT);
        if (Math.abs(player.stepHeight - step) > 1.0E-4) player.stepHeight = step;
        // Sleeping (0.2 x 0.2 box, eye 0.2) and dying (eye 0.1) change the body on purpose; vanilla
        // restores it on waking or respawn and the next tick rescales it.
        if (player.isSleeping() || player.health <= 0) return;
        Size size = compute(player);
        CURRENT.put(player, size);
        float w = clamp(PLAYER_WIDTH * size.hitboxWidth()), h = clamp(PLAYER_HEIGHT * size.hitboxHeight());
        if (Math.abs(player.width - w) > 1.0E-3 || Math.abs(player.height - h) > 1.0E-3) {
            double feet = player.boundingBox.minY;
            ((EntityInvoker) player).scarlet_lib$setBoundingBoxSpacing(w, h);
            player.setPosition(player.x, feet + player.standingEyeHeight, player.z);
        }
        // Server players keep y at their feet (eye height 0); client players at their eyes.
        if (player.standingEyeHeight > 0) {
            float eye = Math.max(0.1F, PLAYER_EYE * size.eye());
            if (Math.abs(player.standingEyeHeight - eye) > 1.0E-3) {
                double feet = player.boundingBox.minY;
                player.standingEyeHeight = eye;
                player.setPosition(player.x, feet + eye, player.z);
            }
        }
        if (!player.world.isRemote && player.age % 20 == 0) broadcast(player, size);
    }

    /**
     * Resizes the player to their body right now, keeping their feet in place, whatever state
     * they are in: spawning checks (the push-up out of blocks on spawn and respawn) use this so they
     * test the real body, not beta's default 0.6 x 1.8 one.
     */
    public static void fit(PlayerEntity player) {
        Size size = compute(player);
        CURRENT.put(player, size);
        double feet = player.boundingBox.minY;
        ((EntityInvoker) player).scarlet_lib$setBoundingBoxSpacing(clamp(PLAYER_WIDTH * size.hitboxWidth()),
                clamp(PLAYER_HEIGHT * size.hitboxHeight()));
        // Server players keep y at their feet (eye height 0); client players at their eyes.
        if (player.standingEyeHeight > 0) player.standingEyeHeight = Math.max(0.1F, PLAYER_EYE * size.eye());
        player.setPosition(player.x, feet + player.standingEyeHeight, player.z);
    }

    private static float clamp(float v) {
        return Math.max(0.1F, Math.min(5F, v));
    }

    /** Sends the player's size to the clients that can see them (their hitbox, eyes and model). */
    private static void broadcast(PlayerEntity player, Size size) {
        String key = size.toString();
        if (key.equals(LAST_SENT.get(player)) && player.age % 200 != 0) return;
        LAST_SENT.put(player, key);
        MessagePacket packet = new MessagePacket(SYNC_PACKET);
        packet.ints = new int[] { player.id };
        packet.floats = new float[] { size.hitboxWidth(), size.hitboxHeight(), size.eye(), size.renderWidth(), size.renderHeight() };
        PacketHelper.sendToAllTracking(player, packet);
    }

    public static void handlePacket(PlayerEntity receiver, MessagePacket message) {
        float[] f = message.floats;
        if (message.ints == null || f == null || f.length < 5) return;
        REMOTE.put(message.ints[0], new Size(f[0], f[1], f[2], f[3], f[4]));
    }

    /** Entity ids belong to one world. */
    public static void clearRemote() {
        REMOTE.clear();
    }
}
