package mc.sayda.scarlet_lib.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Leaves out mixins another mod does the job of. Retro Commands enables chat in singleplayer
 * itself, by redirecting the same call SingleplayerChatMixin changes, so with it installed that
 * mixin isn't applied.
 */
public final class ScarletMixinPlugin implements IMixinConfigPlugin {
    private static final boolean RETRO_COMMANDS = FabricLoader.getInstance().isModLoaded("retrocommands");

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !(RETRO_COMMANDS && mixinClassName.endsWith(".client.SingleplayerChatMixin"));
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
