package dev.marblegate.skywarddive.common.registry;

import dev.marblegate.skywarddive.common.SkywardDive;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.*;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SDItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SkywardDive.MODID);

    public static final ResourceKey<EquipmentAsset> EQUIPABBLE = ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(SkywardDive.MODID, "titan_armor"));

    public static final DeferredItem<Item> TITAN_ARMOR = ITEMS.registerItem("titan_armor",
            props -> new Item(props
                    .humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.CHESTPLATE)
                    .fireResistant()
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                            .setEquipSound(SoundEvents.ARMOR_EQUIP_GENERIC)
                            .setAsset(EQUIPABBLE)
                            .setDamageOnHurt(false)
                            .build())
                    .setId(ResourceKey.create(Registries.ITEM,
                            Identifier.fromNamespaceAndPath(SkywardDive.MODID, "titan_armor")))));
}
