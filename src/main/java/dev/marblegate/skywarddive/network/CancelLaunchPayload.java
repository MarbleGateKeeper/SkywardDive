package dev.marblegate.skywarddive.network;

import dev.marblegate.skywarddive.common.SkywardDive;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record CancelLaunchPayload() implements CustomPacketPayload {
    public static final Type<CancelLaunchPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkywardDive.MODID, "cancel_launch"));
    public static final StreamCodec<FriendlyByteBuf, CancelLaunchPayload> CODEC = StreamCodec.unit(new CancelLaunchPayload());

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
