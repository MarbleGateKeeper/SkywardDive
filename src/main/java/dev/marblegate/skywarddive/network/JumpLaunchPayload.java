package dev.marblegate.skywarddive.network;

import dev.marblegate.skywarddive.common.SkywardDive;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record JumpLaunchPayload() implements CustomPacketPayload {
    public static final Type<JumpLaunchPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkywardDive.MODID, "jump_launch"));

    public static final StreamCodec<FriendlyByteBuf, JumpLaunchPayload> CODEC = StreamCodec.unit(new JumpLaunchPayload());

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
