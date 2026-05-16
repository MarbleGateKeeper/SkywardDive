package dev.marblegate.skywarddive.common.core;

import dev.marblegate.skywarddive.common.registry.SDAttachmentTypes;
import dev.marblegate.skywarddive.common.registry.SDEffects;
import dev.marblegate.skywarddive.config.Config;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LaunchSession {
    public enum Phase implements StringRepresentable {
        WAITING, LAUNCHING, GLIDING, DONE;

        @Override
        public String getSerializedName() {
            return this.toString();
        }
    }

    private static final double SLOT_SPACING = 1.5;

    private final UUID initiatorId;
    private final ResourceKey<Level> dimension;
    private final List<UUID> passengerIds = new ArrayList<>();
    private final List<UUID> entityPassengerIds = new ArrayList<>();
    private final List<UUID> toDetach = new ArrayList<>();
    private Phase phase = Phase.WAITING;
    private int waitTimer = 0;
    private final double hoverY;
    private double targetY;
    private double lastLaunchY;
    private int stuckTimer;

    public LaunchSession(ServerPlayer initiator) {
        this.initiatorId = initiator.getUUID();
        this.dimension = initiator.level().dimension();
        this.hoverY = initiator.getY() + Config.HOVER_LIFT.getAsDouble();
        initiator.setNoGravity(true);
        updatePhaseAndSync(initiator, Phase.WAITING);
    }

    public boolean canJoin() {
        return phase == Phase.WAITING && passengerIds.size() < Config.MAX_PASSENGERS.getAsInt();
    }

    public boolean hasPassenger(UUID id) {
        return passengerIds.contains(id);
    }

    public void addPassenger(ServerPlayer joiner, ServerPlayer initiator) {
        int slot = passengerIds.size();
        passengerIds.add(joiner.getUUID());
        joiner.setNoGravity(true);
        Vec3 pos = formationPos(initiator, slot);
        joiner.teleportTo(pos.x, joiner.getY(), pos.z);
        updatePhaseAndSync(joiner, Phase.WAITING);
    }

    public void addEntityPassenger(UUID entityId, LivingEntity entity) {
        entityPassengerIds.add(entityId);
        entity.setNoGravity(true);
        updatePhaseAndSync(entity, Phase.WAITING);
    }

    public void removePassenger(ServerPlayer passenger, ServerPlayer initiator) {
        passengerIds.remove(passenger.getUUID());
        passenger.removeEffect(SDEffects.SKY_GLIDING);
        passenger.setNoGravity(false);
        updatePhaseAndSync(passenger, Phase.DONE);
        passenger.setDeltaMovement(0, 0, 0);
        passenger.hurtMarked = true;
        passenger.sendSystemMessage(
                Component.translatable("item.skywarddive.launch_beacon.you_left"), true);
        if (initiator != null) {
            initiator.sendSystemMessage(Component.translatable(
                    "item.skywarddive.launch_beacon.passenger_left",
                    passenger.getDisplayName()), true);
        }
    }

    public void detachPassenger(ServerPlayer passenger) {
        passengerIds.remove(passenger.getUUID());
        passenger.sendSystemMessage(
                Component.translatable("item.skywarddive.launch_beacon.you_left"), true);
    }

    public void cancel(ServerLevel level) {
        forAll(level, p -> {
            p.removeEffect(SDEffects.SKY_GLIDING);
            p.setNoGravity(false);
            updatePhaseAndSync(p, Phase.DONE);
            p.setDeltaMovement(0, 0, 0);
            p.hurtMarked = true;
            p.sendSystemMessage(
                    Component.translatable("item.skywarddive.launch_beacon.session_cancelled"), true);
        });
        restoreEntityPassengers(level);
        phase = Phase.DONE;
    }

    public void triggerLaunch(ServerPlayer initiator) {
        if (phase == Phase.WAITING) beginLaunch(initiator, initiator.level());
    }

    public void cleanupDepartedPassengers() {
        passengerIds.removeAll(toDetach);
        toDetach.clear();
    }

    public List<UUID> getAllParticipants() {
        List<UUID> all = new ArrayList<>();
        all.add(initiatorId);
        all.addAll(passengerIds);
        return all;
    }

    public void tick(ServerLevel level) {
        ServerPlayer initiator = resolve(level, initiatorId);
        if (initiator == null || !initiator.isAlive()) {
            cancel(level);
            return;
        }
        switch (phase) {
            case WAITING -> tickWaiting(initiator, level);
            case LAUNCHING -> tickLaunching(initiator, level);
            case GLIDING -> tickGliding(initiator, level);
            default -> {}
        }
    }

    private void tickWaiting(ServerPlayer initiator, ServerLevel level) {
        ascendToHover(initiator, initiator.getX(), initiator.getZ());

        forPassengers(level, (p, slot) -> {
            Vec3 t = formationPos(initiator, slot);
            ascendToHover(p, t.x, t.z);
        });

        float yaw = initiator.getYRot();
        for (int i = 0; i < entityPassengerIds.size(); i++) {
            Entity e = level.getEntity(entityPassengerIds.get(i));
            if (e == null || !e.isAlive()) continue;
            Vec3 target = formationPos(initiator, passengerIds.size() + i);
            e.setNoGravity(true);
            e.teleportTo(target.x, e.getY(), target.z);
            double dy = target.y - e.getY();
            double vy = dy > 0.1 ? Math.min(Config.ASCENT_SPEED.getAsDouble(), dy) : 0.0;
            e.setDeltaMovement(e.getDeltaMovement().x(), vy, e.getDeltaMovement().z());
            e.hurtMarked = true;
            forceHorizontalFacing(e, yaw);
        }

        int left = Config.WAIT_TICKS.getAsInt() - waitTimer;
        if (left > 0 && left % 20 == 0) {
            notifyAll(level, Component.translatable(
                    "item.skywarddive.launch_beacon.countdown", left / 20));
        }

        waitTimer++;
        if (waitTimer >= Config.WAIT_TICKS.getAsInt()) beginLaunch(initiator, level);
    }

    private void tickLaunching(ServerPlayer initiator, ServerLevel level) {
        applyLaunchVelocity(initiator, initiator.getX(), initiator.getZ());

        forPassengers(level, (p, slot) -> {
            Vec3 t = formationPos(initiator, slot);
            applyLaunchVelocity(p, t.x, t.z);
        });

        float yaw = initiator.getYRot();
        for (int i = 0; i < entityPassengerIds.size(); i++) {
            Entity e = level.getEntity(entityPassengerIds.get(i));
            if (e == null || !e.isAlive()) continue;
            Vec3 target = formationPos(initiator, passengerIds.size() + i);
            e.teleportTo(target.x, e.getY(), target.z);
            double vy = Math.min(Config.LAUNCH_SPEED.getAsDouble(), targetY - e.getY());
            e.setDeltaMovement(e.getDeltaMovement().x(), vy, e.getDeltaMovement().z());
            e.hurtMarked = true;
            forceHorizontalFacing(e, yaw);
        }

        if (Math.abs(initiator.getY() - lastLaunchY) < 0.05) {
            if (++stuckTimer >= Config.STUCK_THRESHOLD.getAsInt()) {
                transitionToGlide(initiator, level);
                return;
            }
        } else {
            stuckTimer = 0;
            lastLaunchY = initiator.getY();
        }

        if (initiator.getY() >= targetY) {
            transitionToGlide(initiator, level);
        }
    }

    private void tickGliding(ServerPlayer initiator, ServerLevel level) {
        if (!initiator.hasEffect(SDEffects.SKY_GLIDING)) {
            updatePhaseAndSync(initiator, Phase.DONE);
            forPassengers(level, (p, _) -> {
                p.removeEffect(SDEffects.SKY_GLIDING);
                p.setNoGravity(false);
                updatePhaseAndSync(p, Phase.DONE);
            });
            restoreEntityPassengers(level);
            phase = Phase.DONE;
            return;
        }

        initiator.startFallFlying();
        initiator.fallDistance = 0.0f;
        initiator.resetFallDistance();

        Vec3 initVel = initiator.getDeltaMovement();
        forPassengers(level, (p, slot) -> {
            if (!p.hasEffect(SDEffects.SKY_GLIDING)) {
                toDetach.add(p.getUUID());
                p.sendSystemMessage(
                        Component.translatable("item.skywarddive.launch_beacon.you_left"), true);
                updatePhaseAndSync(initiator, Phase.DONE);
                return;
            }
            p.startFallFlying();
            Vec3 target = formationPosForGlide(initiator, slot);
            double dist = p.position().distanceTo(target);
            Vec3 vel;
            if (dist > 5.0) {
                p.teleportTo(target.x, target.y, target.z);
                vel = initVel;
            } else {
                Vec3 correction = dist > 0.5
                        ? target.subtract(p.position()).normalize().scale(Math.min(dist * 0.4, 0.5))
                        : Vec3.ZERO;
                vel = initVel.add(correction);
            }
            p.setDeltaMovement(vel);
            p.hurtMarked = true;
            p.fallDistance = 0.0f;
            p.resetFallDistance();
        });

        float yaw = initiator.getYRot();
        for (int i = 0; i < entityPassengerIds.size(); i++) {
            Entity e = level.getEntity(entityPassengerIds.get(i));
            if (e == null || !e.isAlive()) continue;
            Vec3 target = formationPosForGlide(initiator, passengerIds.size() + i);
            double dist = e.position().distanceTo(target);
            e.setNoGravity(true);
            if (dist > 3.0) {
                e.teleportTo(target.x, target.y, target.z);
                e.setDeltaMovement(initVel);
            } else {
                Vec3 correction = dist > 0.5
                        ? target.subtract(e.position()).normalize().scale(Math.min(dist * 0.4, 0.5))
                        : Vec3.ZERO;
                e.setDeltaMovement(initVel.add(correction));
            }
            e.hurtMarked = true;
            e.fallDistance = 0.0f;
            e.resetFallDistance();
            forceHorizontalFacing(e, yaw);
        }
    }

    private void transitionToGlide(ServerPlayer initiator, ServerLevel level) {
        var look = startControllerGlide(initiator);
        forPassengers(level, (p, _) -> startGlide(p, look));
        for (int i = 0; i < entityPassengerIds.size(); i++) {
            Entity e = level.getEntity(entityPassengerIds.get(i));
            if (e == null || !e.isAlive()) continue;
            startGlide((LivingEntity) e, look);
        }
        notifyAll(level, Component.translatable("item.skywarddive.launch_beacon.gliding"));
        phase = Phase.GLIDING;
    }

    private void ascendToHover(ServerPlayer player, double tx, double tz) {
        player.setNoGravity(true);
        player.fallDistance = 0.0f;
        player.resetFallDistance();
        if (Math.hypot(tx - player.getX(), tz - player.getZ()) > 0.3)
            player.teleportTo(tx, player.getY(), tz);
        double dy = hoverY - player.getY();
        player.setDeltaMovement(player.getDeltaMovement().x(),
                dy > 0.05 ? Math.min(Config.ASCENT_SPEED.getAsDouble(), dy) : 0.0,
                player.getDeltaMovement().z());
        player.hurtMarked = true;
    }

    private void applyLaunchVelocity(ServerPlayer player, double tx, double tz) {
        player.fallDistance = 0.0f;
        player.resetFallDistance();
        if (Math.hypot(tx - player.getX(), tz - player.getZ()) > 0.3)
            player.teleportTo(tx, player.getY(), tz);
        player.setDeltaMovement(player.getDeltaMovement().x(),
                Math.min(Config.LAUNCH_SPEED.getAsDouble(), targetY - player.getY()),
                player.getDeltaMovement().z());
        player.hurtMarked = true;
    }

    private Vec3 startControllerGlide(ServerPlayer player) {
        giveEffect(player);
        player.setNoGravity(false);
        updatePhaseAndSync(player, Phase.GLIDING);
        player.startFallFlying();
        var look = horizontalLook(player);
        player.setDeltaMovement(look.x * Config.LAUNCH_SPEED.getAsDouble(), 0, look.z * Config.LAUNCH_SPEED.getAsDouble());
        player.hurtMarked = true;
        player.fallDistance = 0.0f;
        player.resetFallDistance();
        return look;
    }

    private void startGlide(LivingEntity living, Vec3 controllerHorizontalLook) {
        giveEffect(living);
        living.setNoGravity(false);
        updatePhaseAndSync(living, Phase.GLIDING);
        if (living instanceof ServerPlayer player)
            player.startFallFlying();
        living.setDeltaMovement(controllerHorizontalLook.x * Config.LAUNCH_SPEED.getAsDouble(), 0, controllerHorizontalLook.z * Config.LAUNCH_SPEED.getAsDouble());
        living.hurtMarked = true;
        living.fallDistance = 0.0f;
        living.resetFallDistance();
    }

    private void beginLaunch(ServerPlayer initiator, ServerLevel level) {
        targetY = initiator.getY() + Config.LAUNCH_HEIGHT.getAsInt();
        lastLaunchY = initiator.getY();
        stuckTimer = 0;
        phase = Phase.LAUNCHING;
        updatePhaseAndSync(initiator, Phase.LAUNCHING);
        forPassengers(level, (p, _) -> updatePhaseAndSync(p, Phase.LAUNCHING));
        for (int i = 0; i < entityPassengerIds.size(); i++) {
            Entity e = level.getEntity(entityPassengerIds.get(i));
            if (e == null || !e.isAlive()) continue;
            updatePhaseAndSync(e, Phase.LAUNCHING);
        }
    }

    private Vec3 horizontalLook(ServerPlayer player) {
        Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
        if (look.length() == 0) // fallback
            look = Vec3.X_AXIS;
        return look;
    }

    private Vec3 formationPos(ServerPlayer initiator, int slot) {
        Vec3 look = horizontalLook(initiator);
        int sign = (slot % 2 == 0) ? 1 : -1;
        double off = sign * (slot / 2 + 1) * SLOT_SPACING;
        return new Vec3(initiator.getX() + look.z * off, hoverY, initiator.getZ() - look.x * off);
    }

    private Vec3 formationPosForGlide(ServerPlayer initiator, int slot) {
        Vec3 look = horizontalLook(initiator);
        int rank = slot / 2;
        int sign = (slot % 2 == 0) ? 1 : -1;
        double side = sign * (rank + 1) * SLOT_SPACING;
        double back = rank * SLOT_SPACING;
        return new Vec3(initiator.getX() + look.z * side - look.x * back, initiator.getY(), initiator.getZ() - look.x * side - look.z * back);
    }

    private static void forceHorizontalFacing(Entity entity, float yaw) {
        if (!(entity instanceof LivingEntity living)) return;
        if (living instanceof EnderDragon)
            yaw = yaw + 180.f;
        living.setYRot(yaw);
        living.yHeadRot = yaw;
        living.yBodyRot = yaw;
        living.setXRot(0);
    }

    private static void giveEffect(LivingEntity living) {
        living.addEffect(new MobEffectInstance(
                SDEffects.SKY_GLIDING, 100000, 0, false, false, false));
    }

    private void restoreEntityPassengers(ServerLevel level) {
        for (UUID eid : entityPassengerIds) {
            Entity e = level.getEntity(eid);
            if (e != null) {
                e.setNoGravity(false);
                ((LivingEntity) e).removeEffect(SDEffects.SKY_GLIDING);
                updatePhaseAndSync(e, Phase.DONE);
            }
        }
        entityPassengerIds.clear();
    }

    private void notifyAll(ServerLevel level, Component msg) {
        forAll(level, p -> p.sendSystemMessage(msg, true));
    }

    private void updatePhaseAndSync(Entity entity, Phase phase) {
        entity.setData(SDAttachmentTypes.PHASE, phase);
    }

    @FunctionalInterface
    private interface PassengerConsumer {
        void accept(ServerPlayer p, int slot);
    }

    private void forPassengers(ServerLevel level, PassengerConsumer action) {
        for (int i = 0; i < passengerIds.size(); i++) {
            ServerPlayer p = resolve(level, passengerIds.get(i));
            if (p != null && p.isAlive()) action.accept(p, i);
        }
    }

    private void forAll(ServerLevel level, Consumer<ServerPlayer> action) {
        ServerPlayer init = resolve(level, initiatorId);
        if (init != null) action.accept(init);
        for (UUID id : passengerIds) {
            ServerPlayer p = resolve(level, id);
            if (p != null) action.accept(p);
        }
    }

    private static ServerPlayer resolve(ServerLevel level, UUID id) {
        Player p = level.getPlayerByUUID(id);
        return p instanceof ServerPlayer sp ? sp : null;
    }

    public UUID getInitiatorId() {
        return initiatorId;
    }

    public Phase getPhase() {
        return phase;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }
}
