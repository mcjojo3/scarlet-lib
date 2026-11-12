package mc.sayda.scarlet_lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mc.sayda.scarlet_lib.client.VisionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The camera at a resized player's eyes, and darkness/blindness fog.
 * <p>Beta offsets the camera by (eye height - 1.62), which pins it 1.62 above the feet whatever
 * the eye height; measuring from the player's own eye height leaves it at the eyes. The
 * third-person distance scales with the player too. Sleeping and dead players keep vanilla's.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Shadow
    private Minecraft client;
    @Shadow
    private float fogRed;
    @Shadow
    private float fogGreen;
    @Shadow
    private float fogBlue;

    @Unique
    private float scarlet_lib$eyeHeight() {
        LivingEntity camera = client.camera;
        if (camera instanceof PlayerEntity player && !player.isSleeping() && player.health > 0
                && player.standingEyeHeight > 0) {
            return player.standingEyeHeight;
        }
        return 1.62F;
    }

    @ModifyConstant(method = "applyCameraTransform", constant = @Constant(floatValue = 1.62F))
    private float scarlet_lib$cameraAtEyes(float vanilla) {
        return scarlet_lib$eyeHeight();
    }

    @ModifyExpressionValue(method = "applyCameraTransform",
            at = { @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;thirdPersonDistance:F"),
                    @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;prevThirdPersonDistance:F") })
    private float scarlet_lib$scaledThirdPersonDistance(float distance) {
        return distance * scarlet_lib$eyeHeight() / 1.62F;
    }

    /** Darkness and blindness: the sky and fog colour fade to black... */
    @Inject(method = "updateSkyAndFogColors", at = @At("TAIL"))
    private void scarlet_lib$darkSky(float tickDelta, CallbackInfo ci) {
        float dark = VisionClient.fogDarkness(client, tickDelta);
        if (dark <= 0) return;
        float keep = 1F - dark;
        fogRed *= keep;
        fogGreen *= keep;
        fogBlue *= keep;
        GL11.glClearColor(fogRed, fogGreen, fogBlue, 0F);
    }

    /** ...and the fog closes in to a few blocks. */
    @Inject(method = "applyFog", at = @At("TAIL"))
    private void scarlet_lib$darkFog(int mode, float tickDelta, CallbackInfo ci) {
        float dark = VisionClient.fogDarkness(client, tickDelta);
        if (dark <= 0) return;
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
        GL11.glFogf(GL11.GL_FOG_START, 0F);
        GL11.glFogf(GL11.GL_FOG_END, 4F + 20F * (1F - dark));
    }
}
