package dev.marblegate.skywarddive.network;

import dev.marblegate.skywarddive.common.SkywardDive;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record StartSessionPayload() implements CustomPacketPayload {
    public static final Type<StartSessionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SkywardDive.MODID, "start_session"));
    public static final StreamCodec<ByteBuf, StartSessionPayload> CODEC = StreamCodec.unit(new StartSessionPayload());

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
