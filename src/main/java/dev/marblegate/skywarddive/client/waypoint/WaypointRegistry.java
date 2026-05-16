package dev.marblegate.skywarddive.client.waypoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.player.LocalPlayer;

public final class WaypointRegistry {
    private static final Map<String, WaypointProvider> SOURCES = new LinkedHashMap<>();

    private WaypointRegistry() {}

    public static void register(String id, WaypointProvider provider) {
        SOURCES.put(id, provider);
    }

    public static void register(String id, WaypointSource source) {
        if (source instanceof WaypointProvider p) {
            SOURCES.put(id, p);
        } else {
            SOURCES.put(id, new WaypointProvider() {
                @Override
                protected List<SkywardDiveWaypoint> buildWaypoints(LocalPlayer player) {
                    return source.getWaypoints(player);
                }

                @Override
                public List<SkywardDiveWaypoint> getWaypoints(LocalPlayer player) {
                    return source.getWaypoints(player);
                }
            });
        }
    }

    public static void unregister(String id) {
        SOURCES.remove(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends WaypointProvider> T get(String id, Class<T> type) {
        WaypointProvider p = SOURCES.get(id);
        return type.isInstance(p) ? (T) p : null;
    }

    public static void markDirty(String id) {
        WaypointProvider p = SOURCES.get(id);
        if (p != null) p.markDirty();
    }

    public static void invalidateAll() {
        for (Map.Entry<String, WaypointProvider> entry : SOURCES.entrySet()) {
            try {
                entry.getValue().invalidate();
            } catch (Exception e) {
                logError(entry.getKey(), "invalidate", e);
            }
        }
    }

    public static List<SkywardDiveWaypoint> collectAll(LocalPlayer player) {
        if (SOURCES.isEmpty()) return Collections.emptyList();
        List<SkywardDiveWaypoint> result = new ArrayList<>();
        for (Map.Entry<String, WaypointProvider> entry : SOURCES.entrySet()) {
            try {
                List<SkywardDiveWaypoint> batch = entry.getValue().getWaypoints(player);
                if (batch != null) result.addAll(batch);
            } catch (Exception e) {
                logError(entry.getKey(), "getWaypoints", e);
            }
        }
        return result;
    }

    private static void logError(String id, String method, Exception e) {
        net.minecraft.client.Minecraft.getInstance().gui.setOverlayMessage(
                net.minecraft.network.chat.Component.literal(
                        "[SkywardDive] WaypointProvider '" + id + "'." + method + "() threw: " + e.getMessage()),
                false);
    }
}
