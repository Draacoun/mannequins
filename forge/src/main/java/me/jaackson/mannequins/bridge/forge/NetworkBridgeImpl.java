package me.jaackson.mannequins.bridge.forge;

import me.jaackson.mannequins.Mannequins;
import me.jaackson.mannequins.common.network.MannequinsPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Jackson
 */
public class NetworkBridgeImpl {
    public static final SimpleChannel PLAY = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(Mannequins.MOD_ID, "play"))
            .networkProtocolVersion(() -> "1")
            .clientAcceptedVersions("1"::equals)
            .serverAcceptedVersions("1"::equals)
            .simpleChannel();
    private static int currentIndex = -1;

    public static <T extends MannequinsPacket> void registerPlayToClient(ResourceLocation channel, Class<T> messageType, Function<FriendlyByteBuf, T> read, Supplier<Consumer<T>> handle) {
        PLAY.registerMessage(currentIndex++, messageType, MannequinsPacket::write, read, (packet, context) -> {
            NetworkEvent.Context ctx = context.get();
            if (ctx.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                ctx.enqueueWork(() -> handle.get().accept(packet));
                ctx.setPacketHandled(true);
            }
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static <T extends MannequinsPacket> void registerPlayToServer(ResourceLocation channel, Class<T> messageType, Function<FriendlyByteBuf, T> read, BiConsumer<T, Player> handle) {
        PLAY.registerMessage(currentIndex++, messageType, MannequinsPacket::write, read, (packet, context) -> {
            NetworkEvent.Context ctx = context.get();
            if (ctx.getDirection().getReceptionSide() == LogicalSide.SERVER) {
                ctx.enqueueWork(() ->
                {
                    if (ctx.getSender() == null)
                        return;

                    handle.accept(packet, ctx.getSender());
                });
                ctx.setPacketHandled(true);
            }
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToPlayer(ServerPlayer player, MannequinsPacket packet) {
        PLAY.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToTracking(Entity tracking, MannequinsPacket packet) {
        PLAY.send(PacketDistributor.TRACKING_ENTITY.with(() -> tracking), packet);
    }

    public static void sendToNear(ServerLevel level, double x, double y, double z, double distance, MannequinsPacket packet) {
        PLAY.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, distance * distance, level.dimension())), packet);
    }

    public static void sendToServer(MannequinsPacket packet) {
        PLAY.sendToServer(packet);
    }

    public static Packet<?> toVanillaPacket(MannequinsPacket packet, boolean clientbound) {
        return PLAY.toVanillaPacket(packet, clientbound ? NetworkDirection.PLAY_TO_CLIENT : NetworkDirection.PLAY_TO_SERVER);
    }
}
