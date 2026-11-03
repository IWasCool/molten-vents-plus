package com.ourcreation.moltenventsplus.scanner;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The client chooses a whitelisted type only; coordinates and lifetime always come from the server. */
public final class ScannerNetwork {
    public enum Status { SEARCHING, FOUND, NOT_FOUND, REJECTED, COMPLETE }

    public record Request(String targetId, int requestId) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(id("scanner_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> { buffer.writeUtf(value.targetId, 48); buffer.writeVarInt(value.requestId); },
                buffer -> new Request(buffer.readUtf(48), buffer.readVarInt()));
        @Override public Type<Request> type() { return TYPE; }
    }

    public record Cancel(int requestId) implements CustomPacketPayload {
        public static final Type<Cancel> TYPE = new Type<>(id("scanner_cancel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Cancel> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> buffer.writeVarInt(value.requestId),
                buffer -> new Cancel(buffer.readVarInt()));
        @Override public Type<Cancel> type() { return TYPE; }
    }

    public record Result(int requestId, String targetId, Status status, ResourceKey<Level> dimension,
                         BlockPos position, int lifetimeTicks, String reason, int retryTicks) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(id("scanner_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> {
                    buffer.writeVarInt(value.requestId);
                    buffer.writeUtf(value.targetId, 48);
                    buffer.writeEnum(value.status);
                    buffer.writeResourceLocation(value.dimension.location());
                    buffer.writeBlockPos(value.position);
                    buffer.writeVarInt(value.lifetimeTicks);
                    buffer.writeUtf(value.reason, 128);
                    buffer.writeVarInt(value.retryTicks);
                }, buffer -> new Result(buffer.readVarInt(), buffer.readUtf(48), buffer.readEnum(Status.class),
                        ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation()), buffer.readBlockPos(),
                        buffer.readVarInt(), buffer.readUtf(128), buffer.readVarInt()));
        public Result { position = position.immutable(); }
        @Override public Type<Result> type() { return TYPE; }
    }

    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath(MoltenVentsPlus.MODID, name); }

    public static void register(RegisterPayloadHandlersEvent event) {
        // PayloadRegistrar defaults to main-thread handlers in NeoForge 21.1.
        var registrar = event.registrar("vent_surveyor_5").optional();
        registrar.playToServer(Request.TYPE, Request.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) ScannerSearch.request(player, payload);
        });
        registrar.playToClient(Result.TYPE, Result.STREAM_CODEC, (payload, context) -> ScannerClientHooks.accept(payload));
        registrar.playToServer(Cancel.TYPE, Cancel.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) ScannerSearch.cancel(player, payload);
        });
    }

    private ScannerNetwork() {}
}
