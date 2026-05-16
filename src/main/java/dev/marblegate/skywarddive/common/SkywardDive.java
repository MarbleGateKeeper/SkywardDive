package dev.marblegate.skywarddive.common;

import dev.marblegate.skywarddive.common.core.LaunchManager;
import dev.marblegate.skywarddive.common.core.LaunchSession;
import dev.marblegate.skywarddive.common.event.ModEvents;
import dev.marblegate.skywarddive.common.registry.SDAttachmentTypes;
import dev.marblegate.skywarddive.common.registry.SDEffects;
import dev.marblegate.skywarddive.common.registry.SDItems;
import dev.marblegate.skywarddive.common.registry.SDTags;
import dev.marblegate.skywarddive.config.Config;
import dev.marblegate.skywarddive.network.CancelLaunchPayload;
import dev.marblegate.skywarddive.network.JumpLaunchPayload;
import dev.marblegate.skywarddive.network.StartKidnapPayload;
import dev.marblegate.skywarddive.network.StartSessionPayload;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(SkywardDive.MODID)
public class SkywardDive {
    public static final String MODID = "skywarddive";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SKY_TAB = CREATIVE_MODE_TABS.register("sky_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.skywarddive"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> SDItems.TITAN_ARMOR.get().getDefaultInstance())
            .displayItems((_, output) -> {
                output.accept(SDItems.TITAN_ARMOR.get());
            })
            .build());

    public SkywardDive(IEventBus modEventBus, ModContainer modContainer) {
        SDItems.ITEMS.register(modEventBus);
        SDEffects.MOB_EFFECTS.register(modEventBus);
        SDAttachmentTypes.ATTACHMENT_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        NeoForge.EVENT_BUS.register(new ModEvents());
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(SkywardDive::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(MODID).optional();

        registrar.playToServer(StartSessionPayload.TYPE, StartSessionPayload.CODEC, (_, context) -> {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer sp)) return;
                if (!(sp.getItemBySlot(EquipmentSlot.CHEST).is(SDItems.TITAN_ARMOR.get()))) return;
                if (LaunchManager.INSTANCE.isInSession(sp.getUUID())) {
                    sp.sendSystemMessage(Component.translatable("item.skywarddive.launch_beacon.already_in_session"), true);
                    return;
                }
                int foodCost = Config.FOOD_COST.getAsInt();
                if (sp.getFoodData().getFoodLevel() < foodCost && !sp.isCreative()) {
                    sp.sendSystemMessage(Component.translatable("item.skywarddive.titan_armor.not_enough_food"), true);
                    return;
                }
                if (!sp.isCreative()) {
                    sp.getFoodData().setFoodLevel(sp.getFoodData().getFoodLevel() - foodCost);
                    sp.getFoodData().setSaturation(0);
                }
                LaunchManager.INSTANCE.startSession(sp);
                sp.sendSystemMessage(Component.translatable("item.skywarddive.launch_beacon.hovering"), true);
            });
        });

        registrar.playToServer(StartKidnapPayload.TYPE, StartKidnapPayload.CODEC, (_, context) -> {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer sp)) return;
                if (!(sp.getItemBySlot(EquipmentSlot.CHEST).is(SDItems.TITAN_ARMOR.get()))) return;
                if (LaunchManager.INSTANCE.isInSession(sp.getUUID())) {
                    sp.sendSystemMessage(Component.translatable("item.skywarddive.launch_beacon.already_in_session"), true);
                    return;
                }
                int foodCost = Config.FOOD_COST.getAsInt();
                if (sp.getFoodData().getFoodLevel() < foodCost && !sp.isCreative()) {
                    sp.sendSystemMessage(Component.translatable("item.skywarddive.titan_armor.not_enough_food"), true);
                    return;
                }
                if (!sp.isCreative()) {
                    sp.getFoodData().setFoodLevel(sp.getFoodData().getFoodLevel() - foodCost);
                    sp.getFoodData().setSaturation(0);
                }
                LaunchSession session = LaunchManager.INSTANCE.startSession(sp);
                double r = Config.KIDNAP_RANGE.getAsDouble();
                AABB box = sp.getBoundingBox().inflate(r);
                List<LivingEntity> nearby = sp.level().getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != sp && e.isAlive());

                int count = 0;
                for (LivingEntity entity : nearby) {
                    if (entity instanceof ServerPlayer target) {
                        if (!Config.ALLOW_KIDNAP_PLAYERS.get()) continue;
                        if (!LaunchManager.INSTANCE.isInSession(target.getUUID()) && session.canJoin()) {
                            boolean joined = LaunchManager.INSTANCE.forceJoin(sp, target);
                            if (joined) {
                                target.sendSystemMessage(Component.translatable("item.skywarddive.kidnap_beacon.kidnapped"), true);
                                sp.sendSystemMessage(Component.translatable("item.skywarddive.launch_beacon.player_joined", target.getDisplayName()), true);
                                count++;
                            }
                        }
                    } else {
                        if (entity.is(SDTags.EntityTypeTag.KIDNAP_BLACKLIST)) continue;
                        if (count >= Config.MAX_KIDNAP_ENTITIES.getAsInt()) continue;
                        session.addEntityPassenger(entity.getUUID(), entity);
                        count++;
                    }
                }

                if (count == 0) {
                    sp.sendSystemMessage(Component.translatable("item.skywarddive.kidnap_beacon.no_victims"), true);
                } else {
                    sp.sendSystemMessage(Component.translatable("item.skywarddive.kidnap_beacon.launched", count), true);
                }
            });
        });

        registrar.playToServer(JumpLaunchPayload.TYPE, JumpLaunchPayload.CODEC,
                (_, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer sp)) return;
                        LaunchSession session = LaunchManager.INSTANCE.getOwnSession(sp.getUUID());
                        if (session != null && session.getPhase() == LaunchSession.Phase.WAITING) {
                            session.triggerLaunch(sp);
                            sp.sendSystemMessage(
                                    net.minecraft.network.chat.Component.translatable(
                                            "item.skywarddive.launch_beacon.launching"),
                                    true);
                        }
                    });
                });

        registrar.playToServer(CancelLaunchPayload.TYPE, CancelLaunchPayload.CODEC, (_, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (!(sp.level() instanceof ServerLevel level)) return;
            LaunchSession own = LaunchManager.INSTANCE.getOwnSession(sp.getUUID());
            if (own != null && own.getPhase() == LaunchSession.Phase.WAITING) {
                LaunchManager.INSTANCE.cancelSession(sp, level);
                return;
            }
            LaunchManager.INSTANCE.leaveSession(sp, level);
        }));
    }
}
