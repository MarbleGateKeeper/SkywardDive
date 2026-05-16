package dev.marblegate.skywarddive.common.registry;

import dev.marblegate.skywarddive.common.SkywardDive;
import dev.marblegate.skywarddive.common.core.LaunchSession;
import dev.marblegate.skywarddive.common.core.PlayerWaypointData;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class SDAttachmentTypes {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SkywardDive.MODID);

    public static final Supplier<AttachmentType<LaunchSession.Phase>> PHASE = ATTACHMENT_TYPES.register(
            "phase", () -> AttachmentType.builder(() -> LaunchSession.Phase.DONE).sync(NeoForgeStreamCodecs.enumCodec(LaunchSession.Phase.class)).build());

    public static final Supplier<AttachmentType<PlayerWaypointData>> WAYPOINTS = ATTACHMENT_TYPES.register(
            "waypoints", () -> AttachmentType.builder(() -> PlayerWaypointData.EMPTY)
                    .serialize(PlayerWaypointData.CODEC)
                    .sync(PlayerWaypointData.STREAM_CODEC)
                    .copyOnDeath()
                    .build());
}
