package mc.sayda.scarlet_lib.vision;

import mc.sayda.scarlet_lib.effect.Effects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Who can see whom. A hidden entity is not rendered (model, name tag, shadow) and mobs neither
 * pick it as a target nor keep it as one. Beta has no partial invisibility, so the invisibility
 * effect hides completely; mods add their own {@link Rule}s (stealth, other planes...).
 */
public final class Visibility {
    private static final List<Rule> RULES = new ArrayList<>();

    /** True when {@code target} is hidden from {@code viewer} (null: from everyone / mobs in general). */
    @FunctionalInterface
    public interface Rule {
        boolean hides(@Nullable Entity viewer, Entity target);
    }

    static {
        addRule((viewer, target) -> target instanceof LivingEntity && Effects.has(target, "minecraft:invisibility"));
    }

    private Visibility() {
    }

    public static void addRule(Rule rule) {
        RULES.add(rule);
    }

    public static boolean canSee(@Nullable Entity viewer, Entity target) {
        for (Rule rule : RULES) {
            if (rule.hides(viewer, target)) return false;
        }
        return true;
    }
}
