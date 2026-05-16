package dev.marblegate.skywarddive.mixin;

import dev.marblegate.skywarddive.common.registry.SDEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void skywarddive$canGlide(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(SDEffects.SKY_GLIDING)) {
            cir.setReturnValue(true);
        }
    }
}
