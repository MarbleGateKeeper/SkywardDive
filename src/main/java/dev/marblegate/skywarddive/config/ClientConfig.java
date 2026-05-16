package dev.marblegate.skywarddive.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue RENDER_TARGETING = BUILDER
            .comment("Whether to render the targeting lock-on overlay during gliding (default: true)")
            .define("renderTargeting", true);
    public static final ModConfigSpec.BooleanValue RENDER_ADVANCED_TARGETING = BUILDER
            .comment("Whether to render advanced targeting effects (beam, ring, corner brackets, name label) — requires renderTargeting to also be true (default: true)")
            .define("renderAdvancedTargeting", true);
    public static final ModConfigSpec.IntValue WAYPOINT_RENDER_DISTANCE = BUILDER
            .comment("Maximum distance (in blocks) at which waypoints are rendered on the HUD compass bar (default: 96, range: 16–4096)")
            .defineInRange("waypointRenderDistance", 96, 16, 4096);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
