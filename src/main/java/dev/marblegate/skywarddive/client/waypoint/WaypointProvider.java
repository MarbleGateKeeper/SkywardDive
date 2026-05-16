package dev.marblegate.skywarddive.client.waypoint;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;

public abstract class WaypointProvider implements WaypointSource {

    private List<SkywardDiveWaypoint> cache = null;

    public final void markDirty() {
        cache = null;
    }

    protected List<SkywardDiveWaypoint> buildWaypoints(LocalPlayer player) {
        return Collections.emptyList();
    }

    @Override
    public List<SkywardDiveWaypoint> getWaypoints(LocalPlayer player) {
        if (cache == null) {
            cache = buildWaypoints(player);
        }
        return cache;
    }

    public void invalidate() {
        cache = null;
    }
}
