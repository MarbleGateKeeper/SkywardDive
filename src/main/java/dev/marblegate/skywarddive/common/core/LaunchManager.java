package dev.marblegate.skywarddive.common.core;

import dev.marblegate.skywarddive.common.SkywardDive;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = SkywardDive.MODID)
public class LaunchManager {
    public static final LaunchManager INSTANCE = new LaunchManager();
    private final Map<UUID, LaunchSession> sessionsByInitiator = new HashMap<>();

    private LaunchManager() {}

    public boolean isInSession(UUID playerId) {
        if (sessionsByInitiator.containsKey(playerId)) return true;
        return sessionsByInitiator.values().stream().anyMatch(s -> s.hasPassenger(playerId));
    }

    public LaunchSession startSession(ServerPlayer initiator) {
        LaunchSession session = new LaunchSession(initiator);
        sessionsByInitiator.put(initiator.getUUID(), session);
        return session;
    }

    public LaunchSession tryJoinNearest(ServerPlayer joiner, double maxDistance) {
        if (isInSession(joiner.getUUID())) return null;
        if (!(joiner.level() instanceof ServerLevel level)) return null;

        LaunchSession best = null;
        ServerPlayer bestInit = null;
        double bestDist = Double.MAX_VALUE;

        for (LaunchSession session : sessionsByInitiator.values()) {
            if (!session.canJoin()) continue;
            Player ip = level.getPlayerByUUID(session.getInitiatorId());
            if (!(ip instanceof ServerPlayer init)) continue;
            double d = init.distanceToSqr(joiner);
            if (d <= maxDistance * maxDistance && d < bestDist) {
                bestDist = d;
                best = session;
                bestInit = init;
            }
        }

        if (best != null && bestInit != null) best.addPassenger(joiner, bestInit);
        return best;
    }

    public boolean forceJoin(ServerPlayer initiator, ServerPlayer joiner) {
        if (isInSession(joiner.getUUID())) return false;
        LaunchSession session = sessionsByInitiator.get(initiator.getUUID());
        if (session == null || !session.canJoin()) return false;
        session.addPassenger(joiner, initiator);
        return true;
    }

    public void cancelSession(ServerPlayer initiator, ServerLevel level) {
        LaunchSession session = sessionsByInitiator.remove(initiator.getUUID());
        if (session != null) session.cancel(level);
    }

    public void leaveSession(ServerPlayer passenger, ServerLevel level) {
        LaunchSession session = findSessionOf(passenger.getUUID());
        if (session == null) return;

        switch (session.getPhase()) {
            case WAITING -> {
                ServerPlayer init = resolveInitiator(session, level);
                session.removePassenger(passenger, init);
            }
            case GLIDING -> session.detachPassenger(passenger);
            default -> {}
        }
    }

    public LaunchSession getOwnSession(UUID playerId) {
        return sessionsByInitiator.get(playerId);
    }

    public LaunchSession getSessionOf(UUID playerId) {
        LaunchSession own = sessionsByInitiator.get(playerId);
        return own != null ? own : findSessionOf(playerId);
    }

    private LaunchSession findSessionOf(UUID passengerId) {
        for (LaunchSession s : sessionsByInitiator.values()) {
            if (s.hasPassenger(passengerId)) return s;
        }
        return null;
    }

    private static ServerPlayer resolveInitiator(LaunchSession session, ServerLevel level) {
        Player p = level.getPlayerByUUID(session.getInitiatorId());
        return p instanceof ServerPlayer sp ? sp : null;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, LaunchSession> entry : INSTANCE.sessionsByInitiator.entrySet()) {
            LaunchSession session = entry.getValue();
            if (!session.getDimension().equals(level.dimension())) continue;
            session.tick(level);
            session.cleanupDepartedPassengers();
            if (session.getPhase() == LaunchSession.Phase.DONE) {
                toRemove.add(entry.getKey());
            }
        }

        toRemove.forEach(INSTANCE.sessionsByInitiator::remove);
    }
}
