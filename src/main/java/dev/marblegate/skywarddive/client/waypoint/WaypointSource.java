package dev.marblegate.skywarddive.client.waypoint;

import java.util.List;
import net.minecraft.client.player.LocalPlayer;

@FunctionalInterface
public interface WaypointSource {
    List<SkywardDiveWaypoint> getWaypoints(LocalPlayer player);
}
