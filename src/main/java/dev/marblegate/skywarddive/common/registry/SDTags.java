package dev.marblegate.skywarddive.common.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class SDTags {
    public static class EntityTypeTag {
        public static final TagKey<EntityType<?>> KIDNAP_BLACKLIST = TagKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath("skywarddive", "kidnap_blacklist"));
    }
}
