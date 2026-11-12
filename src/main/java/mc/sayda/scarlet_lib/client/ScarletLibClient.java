package mc.sayda.scarlet_lib.client;

import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.Minecraft;
import net.modificationstation.stationapi.api.event.tick.GameTickEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.EntrypointManager;

import java.lang.invoke.MethodHandles;

/** Client event-bus entrypoint ("stationapi:event_bus_client"). */
public final class ScarletLibClient {
    static {
        EntrypointManager.registerLookup(MethodHandles.lookup());
    }

    @SuppressWarnings("deprecation")
    public static Minecraft minecraft() {
        return (Minecraft) FabricLoader.getInstance().getGameInstance();
    }

    @EventListener
    private static void onTick(GameTickEvent.End event) {
        VisionClient.tick(minecraft());
    }
}
