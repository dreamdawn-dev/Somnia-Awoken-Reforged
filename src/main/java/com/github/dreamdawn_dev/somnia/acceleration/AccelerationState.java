package com.github.dreamdawn_dev.somnia.acceleration;

import com.github.dreamdawn_dev.somnia.SomniaCommand;
import com.github.dreamdawn_dev.somnia.capability.CapabilityFatigue;
import com.github.dreamdawn_dev.somnia.capability.Fatigue;
import com.github.dreamdawn_dev.somnia.compat.Compat;
import com.github.dreamdawn_dev.somnia.compat.DarkUtilsCompat;
import com.github.dreamdawn_dev.somnia.util.SomniaUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public enum AccelerationState {
    UNAVAILABLE,
    INACTIVE,
    WAITING,
    SIMULATING;

    public static AccelerationState forLevel(ServerLevel level) {
        if (!SomniaUtil.isValidSleepTime(level)) return UNAVAILABLE;

        List<ServerPlayer> players = level.players();
        if (!players.isEmpty()) {
            boolean anySleeping = false;
            boolean allSleeping = true;
            int acceleratedSleep = 0;
            int normalSleep = 0;

            for (ServerPlayer player : players) {
                boolean sleeping = player.isSleeping() || SomniaCommand.OVERRIDES.contains(player.getUUID());
                anySleeping |= sleeping;
                allSleeping &= sleeping;

                // Rest-mode sleepers (fatigue below the sleep threshold) are asleep, but they don't
                // take part in the simulation vote - the world simply keeps running while they rest.
                if (sleeping && isParticipatingSleeper(player)) {
                    if (shouldSleepNormally(player)) normalSleep++;
                    else acceleratedSleep++;
                }
            }

            if (allSleeping) {
                if (acceleratedSleep > 0 && acceleratedSleep >= normalSleep) {
                    return SIMULATING;
                }
            }
            else if (anySleeping) {
                return WAITING;
            }
        }

        return INACTIVE;
    }

    private static boolean shouldSleepNormally(Player player) {
        boolean sleepNormally = player.getCapability(CapabilityFatigue.INSTANCE)
            .map(Fatigue::shouldSleepNormally)
            .orElse(false);
        return sleepNormally || DarkUtilsCompat.hasSleepCharm(player);
    }

    private static boolean isParticipatingSleeper(ServerPlayer player) {
        return player.getCapability(CapabilityFatigue.INSTANCE)
            .map(Fatigue::isAcceleratedSleep)
            .orElse(false)
            || shouldSleepNormally(player)
            || Compat.isSleepingInHammock(player);
    }
}
