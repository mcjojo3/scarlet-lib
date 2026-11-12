package mc.sayda.scarlet_lib.mixin;

import mc.sayda.scarlet_lib.attribute.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The armor attribute, starting from the worn armor, in the value beta uses for damage reduction
 * and the HUD. Capped at 24: beta's formula (25 - armor) would stop all damage at 25.
 */
@Mixin(PlayerInventory.class)
abstract class PlayerInventoryMixin {
    @Shadow
    public PlayerEntity player;

    @Inject(method = "getTotalArmorDurability", at = @At("RETURN"), cancellable = true)
    private void scarlet_lib$armor(CallbackInfoReturnable<Integer> cir) {
        if (player == null) return;
        double armor = Attributes.value(player, Attributes.ARMOR, cir.getReturnValue());
        cir.setReturnValue((int) Math.max(0, Math.min(24, Math.round(armor))));
    }
}
