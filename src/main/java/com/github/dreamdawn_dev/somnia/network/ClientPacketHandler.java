package com.github.dreamdawn_dev.somnia.network;

import com.github.dreamdawn_dev.somnia.capability.CapabilityFatigue;
import com.github.dreamdawn_dev.somnia.gui.WakeTimeSelectScreen;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {

    public static void updateWakeTime(long wakeTime) {
        Minecraft.getInstance().player.getCapability(CapabilityFatigue.INSTANCE).ifPresent(props -> props.setWakeTime(wakeTime));
    }

    public static void openGUI() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof WakeTimeSelectScreen)) mc.setScreen(new WakeTimeSelectScreen());
    }

    public static void wakeUpPlayer() {
        Minecraft.getInstance().player.stopSleeping();
    }

    public static void updateFatigue(double fatigue) {
        Minecraft.getInstance().player.getCapability(CapabilityFatigue.INSTANCE)
            .ifPresent(props -> props.setFatigue(fatigue));
    }

    private ClientPacketHandler() {}
}
