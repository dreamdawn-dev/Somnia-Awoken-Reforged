package com.github.dreamdawn_dev.somnia.network.client;

import com.github.dreamdawn_dev.somnia.network.ClientPacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenGUIPacket {
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientPacketHandler::openGUI);
    }
}
