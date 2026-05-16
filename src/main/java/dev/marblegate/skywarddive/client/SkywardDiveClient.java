package dev.marblegate.skywarddive.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.marblegate.skywarddive.client.model.TitanArmorModel;
import dev.marblegate.skywarddive.client.waypoint.SkywardDiveWaypointProvider;
import dev.marblegate.skywarddive.client.waypoint.WaypointRegistry;
import dev.marblegate.skywarddive.common.SkywardDive;
import dev.marblegate.skywarddive.common.core.LaunchSession;
import dev.marblegate.skywarddive.common.registry.SDAttachmentTypes;
import dev.marblegate.skywarddive.common.registry.SDItems;
import dev.marblegate.skywarddive.config.ClientConfig;
import dev.marblegate.skywarddive.network.CancelLaunchPayload;
import dev.marblegate.skywarddive.network.JumpLaunchPayload;
import dev.marblegate.skywarddive.network.StartKidnapPayload;
import dev.marblegate.skywarddive.network.StartSessionPayload;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

@Mod(value = SkywardDive.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SkywardDive.MODID, value = Dist.CLIENT)
public class SkywardDiveClient {
    private static final int TRAIL_LENGTH = 40;
    private static final Map<Integer, ArrayDeque<Vec3>> trailHistory = new HashMap<>();

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(SkywardDive.MODID, "category"));
    public static final KeyMapping KEY_LAUNCH = new KeyMapping(
            "key.skywarddive.launch", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping KEY_KIDNAP = new KeyMapping(
            "key.skywarddive.kidnap", KeyConflictContext.IN_GAME, KeyModifier.SHIFT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    public SkywardDiveClient(ModContainer container, IEventBus modEventBus) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        WaypointRegistry.register("skywarddive:internal", new SkywardDiveWaypointProvider());
    }

    @SubscribeEvent
    static void onEntityModelLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TitanArmorModel.LAYER, TitanArmorModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerItemClientExtension(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private TitanArmorModel model;

            @Override
            public Model getHumanoidArmorModel(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model original) {
                if (model == null) {
                    model = new TitanArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(TitanArmorModel.LAYER));
                }
                return model;
            }

            @Override
            public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier _default) {
                return Identifier.fromNamespaceAndPath(SkywardDive.MODID, "textures/entity/titan_armor.png");
            }
        }, SDItems.TITAN_ARMOR);
    }

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(KEY_LAUNCH);
        event.register(KEY_KIDNAP);
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        WaypointRegistry.invalidateAll();
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean launch = KEY_LAUNCH.consumeClick();
        boolean kidnap = KEY_KIDNAP.consumeClick();
        if (mc.player == null || mc.level == null) return;

        if (launch) {
            ClientPacketDistributor.sendToServer(new StartSessionPayload());
        } else if (kidnap) {
            ClientPacketDistributor.sendToServer(new StartKidnapPayload());
        } else if (mc.options.keyJump.consumeClick()) {
            ClientPacketDistributor.sendToServer(new JumpLaunchPayload());
        } else if (mc.options.keyShift.consumeClick()) {
            ClientPacketDistributor.sendToServer(new CancelLaunchPayload());
        }
        StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).toList().forEach(entity -> {
            if (entity instanceof LivingEntity living) {
                double x = living.getX(), y = living.getY(), z = living.getZ();
                var data = living.getExistingDataOrNull(SDAttachmentTypes.PHASE);
                if (data == null || data == LaunchSession.Phase.DONE) {
                    trailHistory.remove(living.getId());
                    return;
                } else if (data == LaunchSession.Phase.GLIDING) {
                    double cy = y + living.getBbHeight() * 0.5;
                    ArrayDeque<Vec3> history = trailHistory.computeIfAbsent(living.getId(), k -> new ArrayDeque<>());
                    history.addFirst(new Vec3(x, cy, z));
                    while (history.size() > TRAIL_LENGTH) history.removeLast();
                    int idx = 0;
                    for (Vec3 pos : history) {
                        float spawnChance = 1.0f - (float) idx / TRAIL_LENGTH;
                        if (mc.level.getRandom().nextFloat() < spawnChance) {
                            addParticles(mc.level, ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0.0);
                        }
                        idx++;
                    }
                } else if (data == LaunchSession.Phase.WAITING) {
                    if (entity.level().getGameTime() % 5 == 0) {
                        addParticles(mc.level, ParticleTypes.FLAME, x, y, z, 3, 0.15, 0, 0.15, 0.05);
                        addParticles(mc.level, ParticleTypes.FLAME, x, y, z, 3, 0.1, 0, 0.1, -0.2);
                    }
                } else if (data == LaunchSession.Phase.LAUNCHING) {
                    addParticles(mc.level, ParticleTypes.FLAME, x, y, z, 15, 0.15, 0, 0.15, 0.05);
                    addParticles(mc.level, ParticleTypes.FLAME, x, y, z, 15, 0.1, 0, 0.1, -0.2);
                }
            }
        });
    }

    private static void addParticles(ClientLevel level, ParticleOptions type, double x, double y, double z,
            int count, double xDist, double yDist, double zDist, double speed) {
        RandomSource rand = level.getRandom();
        for (int i = 0; i < count; i++) {
            double px = xDist != 0 ? x + (rand.nextDouble() * 2 - 1) * xDist : x;
            double py = yDist != 0 ? y + (rand.nextDouble() * 2 - 1) * yDist : y;
            double pz = zDist != 0 ? z + (rand.nextDouble() * 2 - 1) * zDist : z;
            double vx = (rand.nextDouble() * 2 - 1) * speed;
            double vy = (rand.nextDouble() * 2 - 1) * speed;
            double vz = (rand.nextDouble() * 2 - 1) * speed;
            level.addParticle(type, px, py, pz, vx, vy, vz);
        }
    }
}
