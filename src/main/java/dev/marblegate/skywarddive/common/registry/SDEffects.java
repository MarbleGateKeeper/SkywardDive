package dev.marblegate.skywarddive.common.registry;

import dev.marblegate.skywarddive.common.SkywardDive;
import dev.marblegate.skywarddive.common.effect.GlidingMarkerEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SDEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, SkywardDive.MODID);

    public static final Holder<MobEffect> SKY_GLIDING = MOB_EFFECTS.register("sky_gliding", () -> new GlidingMarkerEffect().addAttributeModifier(Attributes.MOVEMENT_SPEED, Identifier.withDefaultNamespace("effect.speed"), 2F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
}
