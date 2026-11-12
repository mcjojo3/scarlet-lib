package mc.sayda.scarlet_lib;

import mc.sayda.scarlet_lib.compat.retrocommands.RetroCommandsBridge;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric's main entrypoint, for what has to happen after other mods have initialized (StationAPI's
 * InitEvent, where everything else is set up, runs before any of them).
 */
public final class ScarletLibMain implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("retrocommands")) RetroCommandsBridge.install();
    }
}
