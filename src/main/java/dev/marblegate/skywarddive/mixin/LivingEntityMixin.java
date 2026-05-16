package dev.marblegate.skywarddive.mixin;

import dev.marblegate.skywarddive.common.core.LaunchSession;
import dev.marblegate.skywarddive.common.registry.SDAttachmentTypes;
import dev.marblegate.skywarddive.common.registry.SDEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getRandom(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/lang/Object;"), cancellable = true)
    private void updateFallFlying$safeGetRandomGliderSlot(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(SDEffects.SKY_GLIDING)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateWalkAnimation", at = @At("HEAD"), cancellable = true)
    private void updateWalkAnimation$stopWalkAnim(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        LaunchSession.Phase phase = self.getExistingDataOrNull(SDAttachmentTypes.PHASE);
        if (phase == null || phase == LaunchSession.Phase.DONE) return;
        self.walkAnimation.stop();
        ci.cancel();
    }
}
