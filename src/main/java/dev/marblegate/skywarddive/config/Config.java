package dev.marblegate.skywarddive.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue LAUNCH_HEIGHT = BUILDER
            .comment("How many blocks above the initiator's position players will ascend to before gliding (default: 96)")
            .defineInRange("launchHeight", 96, 10, 256);
    public static final ModConfigSpec.DoubleValue LAUNCH_SPEED = BUILDER
            .comment("Upward velocity per tick applied during launch (default: 1.5)")
            .defineInRange("launchSpeed", 1.5, 0.1, 10.0);
    public static final ModConfigSpec.IntValue WAIT_TICKS = BUILDER
            .comment("Ticks to wait in hover state before auto-launching (default: 100, i.e. 10 seconds)")
            .defineInRange("waitTicks", 200, 20, 1200);
    public static final ModConfigSpec.IntValue MAX_PASSENGERS = BUILDER
            .comment("Maximum number of additional players that can join a launch (not counting the initiator, default: 2)")
            .defineInRange("maxPassengers", 2, 0, 100);
    public static final ModConfigSpec.DoubleValue KIDNAP_RANGE = BUILDER
            .comment("Radius in blocks that Kidnap Beacon searches for creatures (default: 8)")
            .defineInRange("kidnapRange", 8.0, 1.0, 32.0);
    public static final ModConfigSpec.DoubleValue ASCENT_SPEED = BUILDER
            .comment("Upward velocity per tick while ascending to hover position (default: 0.15)")
            .defineInRange("ascentSpeed", 0.15, 0.01, 2.0);
    public static final ModConfigSpec.IntValue STUCK_THRESHOLD = BUILDER
            .comment("Ticks without upward progress before forcing transition to glide (default: 40)")
            .defineInRange("stuckThreshold", 40, 5, 200);
    public static final ModConfigSpec.DoubleValue HOVER_LIFT = BUILDER
            .comment("Blocks above initiator's feet to hover at during waiting phase (default: 2.0)")
            .defineInRange("hoverLift", 2.0, 0.5, 10.0);
    public static final ModConfigSpec.DoubleValue TARGET_MAX_RANGE = BUILDER
            .comment("Max distance in blocks for targeting lock-on during glide (default: 128)")
            .defineInRange("targetMaxRange", 128.0, 16.0, 512.0);
    public static final ModConfigSpec.IntValue TARGET_LOCK_TICKS = BUILDER
            .comment("Ticks to complete a targeting lock-on (default: 30, i.e. 1.5 seconds)")
            .defineInRange("targetLockTicks", 30, 1, 100);
    public static final ModConfigSpec.IntValue FOOD_COST = BUILDER
            .comment("Food levels consumed when activating Skyward Dive (default: 6)")
            .defineInRange("foodCost", 6, 1, 20);
    public static final ModConfigSpec.IntValue MAX_KIDNAP_ENTITIES = BUILDER
            .comment("Maximum number of non-player entities that can be kidnapped per use (default: 6)")
            .defineInRange("maxKidnapEntities", 6, 0, 100);
    public static final ModConfigSpec.BooleanValue ALLOW_KIDNAP_PLAYERS = BUILDER
            .comment("Whether players can be kidnapped by the Kidnap Dive (default: true)")
            .define("allowKidnapPlayers", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
