package dev.marblegate.skywarddive.client.waypoint;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SkywardDiveWaypoint {
    private final Vec3 worldPos;
    private final ResourceKey<Level> dimension;
    private final String icon;
    private final String label;
    private final int color;

    private SkywardDiveWaypoint(Builder b) {
        this.worldPos = b.worldPos;
        this.dimension = b.dimension;
        this.icon = b.icon;
        this.label = b.label;
        this.color = b.color;
    }

    public Vec3 worldPos() {
        return worldPos;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public String icon() {
        return icon;
    }

    public String label() {
        return label;
    }

    public int color() {
        return color;
    }

    public static Builder builder(Vec3 worldPos, ResourceKey<Level> dimension) {
        return new Builder(worldPos, dimension);
    }

    public static final class Builder {
        private final Vec3 worldPos;
        private final ResourceKey<Level> dimension;
        private String icon = "•";
        private String label = "";
        private int color = 0xFFFFFFFF;
        private final Map<String, Object> extras = new HashMap<>();

        private Builder(Vec3 worldPos, ResourceKey<Level> dimension) {
            this.worldPos = worldPos;
            this.dimension = dimension;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder color(int argb) {
            this.color = argb;
            return this;
        }

        public SkywardDiveWaypoint build() {
            return new SkywardDiveWaypoint(this);
        }
    }
}
