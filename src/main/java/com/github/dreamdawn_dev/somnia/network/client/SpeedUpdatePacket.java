package com.github.dreamdawn_dev.somnia.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpeedUpdatePacket {
    private final double speed;

    public SpeedUpdatePacket(double speed) {
        this.speed = speed;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(this.speed);
    }

    public static SpeedUpdatePacket decode(FriendlyByteBuf buf) {
        double speed = buf.readDouble();
        return new SpeedUpdatePacket(speed);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        // 睡眠叠加层不再显示模拟倍率，因此此数据包被有意忽略。
        ctx.get().setPacketHandled(true);
    }
}