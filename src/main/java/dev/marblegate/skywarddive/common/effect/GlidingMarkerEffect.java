package dev.marblegate.skywarddive.common.effect;

import dev.marblegate.skywarddive.common.registry.SDEffects;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class GlidingMarkerEffect extends MobEffect {
    private static final int REACTION_TICKS = 20;

    public GlidingMarkerEffect() {
        super(MobEffectCategory.NEUTRAL, 0x87CEEB);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public MobEffect addAttributeModifier(Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        return super.addAttributeModifier(attribute, id, amount, operation);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.onGround() || entity.isInWater() || entity.isInLava()) {
            MobEffectInstance current = entity.getEffect(SDEffects.SKY_GLIDING);
            if (current != null) {
                var duration = current.getDuration();
                entity.removeEffect(SDEffects.SKY_GLIDING);
                entity.addEffect(new MobEffectInstance(
                        SDEffects.SKY_GLIDING, duration > REACTION_TICKS ? REACTION_TICKS : duration - 1, amplifier, false, false, false));
            }
        }
        return true;
    }
}
