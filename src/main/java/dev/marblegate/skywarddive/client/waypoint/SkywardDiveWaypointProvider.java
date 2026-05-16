package dev.marblegate.skywarddive.client.waypoint;

import dev.marblegate.skywarddive.common.core.PlayerWaypointData;
import dev.marblegate.skywarddive.common.registry.SDAttachmentTypes;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;

public final class SkywardDiveWaypointProvider extends WaypointProvider {

    @Override
    public List<SkywardDiveWaypoint> getWaypoints(LocalPlayer player) {
        PlayerWaypointData wd = player.getData(SDAttachmentTypes.WAYPOINTS);
        return wd.getWaypoints();
    }
}
