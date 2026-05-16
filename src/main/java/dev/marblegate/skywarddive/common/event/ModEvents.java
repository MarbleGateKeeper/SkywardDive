package dev.marblegate.skywarddive.common.event;

import dev.marblegate.skywarddive.common.core.LaunchManager;
import dev.marblegate.skywarddive.common.core.LaunchSession;
import dev.marblegate.skywarddive.common.core.PlayerWaypointData;
import dev.marblegate.skywarddive.common.registry.SDAttachmentTypes;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;

public class ModEvents {
    private static final double JOIN_DISTANCE = 5.0;

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;
        if (!(event.getEntity() instanceof ServerPlayer joiner)) return;

        LaunchSession targetSession = LaunchManager.INSTANCE.getOwnSession(target.getUUID());
        if (targetSession == null || targetSession.getPhase() != LaunchSession.Phase.WAITING) return;

        if (LaunchManager.INSTANCE.isInSession(joiner.getUUID())) {
            joiner.sendSystemMessage(Component.translatable("item.skywarddive.launch_beacon.already_in_session"), true);
            event.setCanceled(true);
            return;
        }

        LaunchSession joined = LaunchManager.INSTANCE.tryJoinNearest(joiner, JOIN_DISTANCE);
        if (joined != null) {
            joiner.sendSystemMessage(Component.translatable("item.skywarddive.launch_beacon.joined"), true);
            target.sendSystemMessage(Component.translatable("item.skywarddive.launch_beacon.player_joined",
                    joiner.getDisplayName()), true);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceKey<Level> dim = player.level().dimension();
        BlockPos pos = player.blockPosition();
        PlayerWaypointData current = player.getData(SDAttachmentTypes.WAYPOINTS);
        player.setData(SDAttachmentTypes.WAYPOINTS, new PlayerWaypointData(
                Optional.of(new GlobalPos(dim, pos)),
                current.respawnPoint()));
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        LevelData.RespawnData respawn = sp.getRespawnConfig().respawnData();
        Optional<GlobalPos> death = sp.getLastDeathLocation();
        sp.setData(SDAttachmentTypes.WAYPOINTS, new PlayerWaypointData(death, Optional.of(respawn.globalPos())));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSetSpawn(PlayerSetSpawnEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockPos spawnPos = event.getNewSpawn();
        PlayerWaypointData current = player.getData(SDAttachmentTypes.WAYPOINTS);
        if (spawnPos == null) {
            player.setData(SDAttachmentTypes.WAYPOINTS,
                    new PlayerWaypointData(current.deathPoint(), Optional.empty()));
        } else {
            player.setData(SDAttachmentTypes.WAYPOINTS, new PlayerWaypointData(
                    current.deathPoint(),
                    Optional.of(new GlobalPos(event.getSpawnLevel(), spawnPos))));
        }
    }
}
