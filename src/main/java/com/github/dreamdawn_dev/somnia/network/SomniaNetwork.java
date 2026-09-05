package com.github.dreamdawn_dev.somnia.network;

import com.github.dreamdawn_dev.somnia.SomniaAwoken;
import com.github.dreamdawn_dev.somnia.network.client.ClientWakeTimeUpdatePacket;
import com.github.dreamdawn_dev.somnia.network.client.FatigueUpdatePacket;
import com.github.dreamdawn_dev.somnia.network.client.OpenGUIPacket;
import com.github.dreamdawn_dev.somnia.network.client.PlayerWakeUpPacket;
import com.github.dreamdawn_dev.somnia.network.client.SpeedUpdatePacket;
import com.github.dreamdawn_dev.somnia.network.server.ActivateBlockPacket;
import com.github.dreamdawn_dev.somnia.network.server.ResetSpawnPacket;
import com.github.dreamdawn_dev.somnia.network.server.WakeTimeUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SomniaNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(SomniaAwoken.MODID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static void registerMessages() {
        int id = 0;

        // 客户端消息
        INSTANCE.messageBuilder(ClientWakeTimeUpdatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ClientWakeTimeUpdatePacket::encode)
            .decoder(ClientWakeTimeUpdatePacket::decode)
            .consumerMainThread(ClientWakeTimeUpdatePacket::handle)
            .add();
        INSTANCE.messageBuilder(FatigueUpdatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FatigueUpdatePacket::encode)
            .decoder(FatigueUpdatePacket::decode)
            .consumerMainThread(FatigueUpdatePacket::handle)
            .add();
        INSTANCE.messageBuilder(OpenGUIPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((msg, buf) -> {})
            .decoder(buf -> new OpenGUIPacket())
            .consumerMainThread(OpenGUIPacket::handle)
            .add();
        INSTANCE.messageBuilder(PlayerWakeUpPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((msg, buf) -> {})
            .decoder(buf -> new PlayerWakeUpPacket())
            .consumerMainThread(PlayerWakeUpPacket::handle)
            .add();
        INSTANCE.messageBuilder(SpeedUpdatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SpeedUpdatePacket::encode)
            .decoder(SpeedUpdatePacket::decode)
            .consumerMainThread(SpeedUpdatePacket::handle)
            .add();

        // 服务端消息
        INSTANCE.messageBuilder(ActivateBlockPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ActivateBlockPacket::encode)
            .decoder(ActivateBlockPacket::decode)
            .consumerMainThread(ActivateBlockPacket::handle)
            .add();
        INSTANCE.messageBuilder(ResetSpawnPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ResetSpawnPacket::encode)
            .decoder(ResetSpawnPacket::decode)
            .consumerMainThread(ResetSpawnPacket::handle)
            .add();
        INSTANCE.messageBuilder(WakeTimeUpdatePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(WakeTimeUpdatePacket::encode)
            .decoder(WakeTimeUpdatePacket::decode)
            .consumerMainThread(WakeTimeUpdatePacket::handle)
            .add();
    }

    public static void sendToClient(Object packet, ServerPlayer player) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToDimension(Object packet, ResourceKey<Level> dimension) {
        INSTANCE.send(PacketDistributor.DIMENSION.with(() -> dimension), packet);
    }

    private SomniaNetwork() {}
}