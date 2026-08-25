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
        // The sleep overlay no longer displays the simulation multiplier, so this packet is intentionally ignored.
        ctx.get().setPacketHandled(true);
    }
}
