package com.github.dreamdawn_dev.somnia.util;

import com.github.dreamdawn_dev.somnia.SomniaConfig;
import com.github.dreamdawn_dev.somnia.capability.CapabilityFatigue;
import com.github.dreamdawn_dev.somnia.network.SomniaNetwork;
import com.github.dreamdawn_dev.somnia.network.client.ClientWakeTimeUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class SomniaUtil {
    /**
     * The fatigue level at which an accelerated sleep ends. Note that this is independent from the
     * minimum fatigue required to start an accelerated sleep ({@code minimumFatigueToSleep}).
     */
    public static final double WAKE_UP_FATIGUE = 8.0;

    public static boolean hasArmor(Player player) {
        return player.getInventory().armor.stream()
            .anyMatch(stack -> !stack.isEmpty());
    }

    public static int getLevelDayTime(Level level) {
        return (int) (level.getDayTime() % 24000L);
    }

    public static long calculateWakeTime(Level level, int target) {
        return calculateWakeTime(level.getGameTime(), getLevelDayTime(level), target);
    }

    public static long calculateWakeTime(long gameTime, long dayTime, int target) {
        long wakeTime = gameTime - dayTime + target;
        return dayTime > target ? wakeTime + 24000L : wakeTime;
    }

    public static String timeStringForGameTime(long gameTime) {
        long time = (gameTime + 6000) % 24000;
        String hours = String.valueOf(time / 1000);
        String minutes = String.valueOf((int) ((time % 1000) / 1000D * 60));

        if (hours.length() == 1) hours = "0" + hours;
        if (minutes.length() == 1) minutes = "0" + minutes;

        return hours + ":" + minutes;
    }

    public static boolean isEnterSleepTime(Level level) {
        long time = getLevelDayTime(level);
        return time >= SomniaConfig.COMMON.enterSleepStart.get() && time <= SomniaConfig.COMMON.enterSleepEnd.get();
    }

    public static boolean isValidSleepTime(ServerLevel level) {
        long time = getLevelDayTime(level);
        return time >= SomniaConfig.COMMON.validSleepStart.get() && time <= SomniaConfig.COMMON.validSleepEnd.get();
    }

    public static void updateWakeTime(ServerPlayer player) {
        player.getCapability(CapabilityFatigue.INSTANCE)
            .filter(props -> props.getWakeTime() < 0)
            .ifPresent(props -> {
                // Normal players wake up when their fatigue drops below the minimum sleep threshold.
                // A default wake time is only assigned to players the fatigue system doesn't apply to,
                // so they don't end up sleeping forever.
                if (!SomniaConfig.COMMON.enableFatigue.get() || player.isCreative()) {
                    long dayTime = SomniaUtil.getLevelDayTime(player.level());
                    long wakeTime = SomniaUtil.calculateWakeTime(player.level().getGameTime(), dayTime, dayTime > 12000 ? 0 : 12000);
                    props.setWakeTime(wakeTime);
                    SomniaNetwork.sendToClient(new ClientWakeTimeUpdatePacket(wakeTime), player);
                }
            });
    }

    private SomniaUtil() {}
}
