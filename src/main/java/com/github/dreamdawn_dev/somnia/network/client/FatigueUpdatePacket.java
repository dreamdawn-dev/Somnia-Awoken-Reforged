package com.github.dreamdawn_dev.somnia.network.client;

import com.github.dreamdawn_dev.somnia.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FatigueUpdatePacket {
    private final double fatigue;

    public FatigueUpdatePacket(double fatigue) {
        this.fatigue = fatigue;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(this.fatigue);
    }

    public static FatigueUpdatePacket decode(FriendlyByteBuf buf) {
        double fatigue = buf.readDouble();
        return new FatigueUpdatePacket(fatigue);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.updateFatigue(this.fatigue));
    }
}
