package com.github.dreamdawn_dev.somnia.network.server;

import com.github.dreamdawn_dev.somnia.capability.CapabilityFatigue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FadeCompletePacket {

    public void encode(FriendlyByteBuf buf) {}

    public static FadeCompletePacket decode(FriendlyByteBuf buf) {
        return new FadeCompletePacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        // 客户端屏幕已完全变黑：有效睡眠的疲劳恢复从此刻开始计算
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.isSleeping()) {
            player.getCapability(CapabilityFatigue.INSTANCE).ifPresent(props -> props.setFullyAsleep(true));
        }
    }
}
