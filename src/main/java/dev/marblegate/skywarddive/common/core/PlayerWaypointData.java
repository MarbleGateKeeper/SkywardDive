package dev.marblegate.skywarddive.common.core;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.marblegate.skywarddive.client.waypoint.SkywardDiveWaypoint;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public class PlayerWaypointData {
    private static final int COLOR_DEATH   = 0xFFFF4444;
    private static final int COLOR_RESPAWN = 0xFF44FF88;

    private final Optional<GlobalPos> deathPoint;
    private final Optional<GlobalPos> respawnPoint;
    private List<SkywardDiveWaypoint> waypoints;

    public PlayerWaypointData(Optional<GlobalPos> deathPoint, Optional<GlobalPos> respawnPoint) {
        this.deathPoint = deathPoint;
        this.respawnPoint = respawnPoint;
    }

    public PlayerWaypointData() {
        this.deathPoint = Optional.empty();
        this.respawnPoint = Optional.empty();
    }

    public Optional<GlobalPos> deathPoint() {
        return deathPoint;
    }

    public Optional<GlobalPos> respawnPoint() {
        return respawnPoint;
    }

    public List<SkywardDiveWaypoint> getWaypoints() {
        if (waypoints == null) {
            waypoints = new ArrayList<>();
            deathPoint.ifPresent(globalPos -> waypoints.add(SkywardDiveWaypoint.builder(Vec3.atCenterOf(globalPos.pos()), globalPos.dimension())
                    .icon("✦").label("Death").color(COLOR_DEATH).build()));
            respawnPoint.ifPresent(globalPos -> waypoints.add(SkywardDiveWaypoint.builder(Vec3.atCenterOf(globalPos.pos()), globalPos.dimension())
                    .icon("⌂").label("Spawn").color(COLOR_RESPAWN).build()));

        }
        return waypoints;
    }

    public static final PlayerWaypointData EMPTY = new PlayerWaypointData();

    public static final MapCodec<PlayerWaypointData> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            GlobalPos.CODEC.optionalFieldOf("death_point").forGetter(PlayerWaypointData::deathPoint),
            GlobalPos.CODEC.optionalFieldOf("respawn_point").forGetter(PlayerWaypointData::respawnPoint))
            .apply(inst, PlayerWaypointData::new));

    public static final StreamCodec<FriendlyByteBuf, PlayerWaypointData> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional), PlayerWaypointData::deathPoint,
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional), PlayerWaypointData::respawnPoint,
            PlayerWaypointData::new);
}
