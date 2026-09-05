package com.github.dreamdawn_dev.somnia.acceleration;

import com.github.dreamdawn_dev.somnia.SomniaAwoken;
import com.github.dreamdawn_dev.somnia.SomniaConfig;
import com.github.dreamdawn_dev.somnia.capability.CapabilityFatigue;
import com.github.dreamdawn_dev.somnia.capability.Fatigue;
import com.github.dreamdawn_dev.somnia.compat.Compat;
import com.github.dreamdawn_dev.somnia.compat.DarkUtilsCompat;
import com.github.dreamdawn_dev.somnia.network.SomniaNetwork;
import com.github.dreamdawn_dev.somnia.network.client.PlayerWakeUpPacket;
import com.github.dreamdawn_dev.somnia.util.SomniaUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SomniaAwoken.MODID)
public final class PlayerSleepController {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSleepingTimeCheck(SleepingTimeCheckEvent event) {
        Player player = event.getEntity();
        if (DarkUtilsCompat.hasSleepCharm(player) || player.getCapability(CapabilityFatigue.INSTANCE).map(Fatigue::shouldSleepNormally).orElse(false)) return;

        if (!SomniaUtil.isEnterSleepTime(player.level())) event.setResult(Event.Result.DENY);
        else event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public static void onPlayerSleepInBed(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        if (!SomniaConfig.COMMON.sleepWithArmor.get() && !player.isCreative() && SomniaUtil.hasArmor(player)) {
            player.displayClientMessage(Component.translatable("somnia.status.armor"), true);
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        }

        player.getCapability(CapabilityFatigue.INSTANCE)
            .ifPresent(props -> {
                props.setSleepNormally(player.isShiftKeyDown());
                // 疲劳值高于睡眠阈值：加速睡眠（世界模拟）。
                // 低于阈值：休息模式 - 玩家可以躺下，但世界不会加速。
                props.setAcceleratedSleep(props.getFatigue() >= SomniaConfig.COMMON.minimumFatigueToSleep.get());
            });

        SomniaUtil.updateWakeTime((ServerPlayer) player);
    }

    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        LevelAccessor level = event.getLevel();

        if (SomniaConfig.COMMON.enableFatigue.get()) {
            level.players().stream()
                .filter(Player::isSleepingLongEnough)
                .forEach(player -> player.getCapability(CapabilityFatigue.INSTANCE)
                    .filter(props -> props.shouldSleepNormally() || DarkUtilsCompat.hasSleepCharm(player))
                    .ifPresent(props -> {
                        long timeSlept = event.getNewTime() - level.dayTime();
                        double replenish = SomniaConfig.COMMON.fatigueReplenishRate.get() * timeSlept;
                        props.setFatigue(props.getFatigue() - replenish);
                    }));
        }
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        player.getCapability(CapabilityFatigue.INSTANCE).ifPresent(props -> {
            props.maxFatigueCounter();
            props.setResetSpawn(true);
            props.setSleepNormally(false);
            props.setSleepOverride(false);
            props.setAcceleratedSleep(false);
            props.setWakeTime(-1);
        });
    }

    @SubscribeEvent
    public static void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        event.getEntity().getCapability(CapabilityFatigue.INSTANCE)
            .map(Fatigue::getResetSpawn)
            .ifPresent(resetSpawn -> {
                if (!resetSpawn) event.setCanceled(true);
            });
    }

    // 我们需要最早的PlayerEntity#hurt监听器
    // 因为必须在MC调用stopSleeping之前将sleepOverride设置为false
    // 否则PlayerSleepTickHandler#tickEnd会让玩家重新开始睡觉
    @SubscribeEvent
    public static void onPlayerDamage(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof ServerPlayer player && entity.isSleeping()) {
            if (player.isInvulnerableTo(event.getSource())
                || player.isInvulnerable() && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || player.isOnFire() && player.hasEffect(MobEffects.FIRE_RESISTANCE)
            ) {
                return;
            }

            entity.getCapability(CapabilityFatigue.INSTANCE).ifPresent(props -> props.setSleepOverride(false));
            entity.stopSleeping();
            SomniaNetwork.sendToClient(new PlayerWakeUpPacket(), player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(CapabilityFatigue.INSTANCE)
                .ifPresent(fatigue -> {
                    if (event.phase == TickEvent.Phase.START) playerTickStart(fatigue, serverPlayer);
                    else playerTickEnd(fatigue, serverPlayer);
                });
        }
    }

    private static void playerTickStart(Fatigue fatigue, Player player) {
        if (player.isSleeping() && player.checkBedExists()) {
            if (fatigue.shouldSleepNormally() || player.getSleepTimer() >= 90 && DarkUtilsCompat.hasSleepCharm(player) || Compat.isSleepingInHammock(player)) {
                fatigue.setSleepOverride(false);
            }
            else if (fatigue.isAcceleratedSleep()) {
                fatigue.setSleepOverride(true);

                if (SomniaConfig.COMMON.fading.get()) {
                    int sleepTimer = player.getSleepTimer() + 1;
                    if (sleepTimer >= 99) sleepTimer = 98;
                    player.sleepCounter = sleepTimer;
                }
            }
            else {
                // 休息模式睡眠（疲劳值低于阈值）：无模拟，无睡眠覆写。
                // 保持睡眠计时器低于100，这样原版的"跳过夜晚"永远不会触发，
                // 意味着世界在玩家躺床时继续以正常速度运行。
                fatigue.setSleepOverride(false);
                if (SomniaConfig.COMMON.fading.get()) {
                    int sleepTimer = player.getSleepTimer() + 1;
                    if (sleepTimer >= 99) sleepTimer = 98;
                    player.sleepCounter = sleepTimer;
                }
            }
        }
    }

    private static void playerTickEnd(Fatigue fatigue, ServerPlayer player) {
        if (!player.isSleeping()) return;

        long wakeTime = fatigue.getWakeTime();
        boolean wakeByTime = wakeTime != -1 && player.level().getGameTime() >= wakeTime;
        boolean wakeByFatigue = wakeTime == -1 && fatigue.isAcceleratedSleep()
            && SomniaConfig.COMMON.enableFatigue.get() && !player.isCreative()
            && fatigue.getFatigue() < SomniaUtil.WAKE_UP_FATIGUE;
        if (wakeByTime || wakeByFatigue) {
            player.stopSleepInBed(true, true);
            SomniaNetwork.sendToClient(new PlayerWakeUpPacket(), player);
        }
        else if (fatigue.sleepOverride()) {
            fatigue.setSleepOverride(false);

            player.startSleeping(player.getSleepingPos().orElse(player.blockPosition()));
        }
    }

    private PlayerSleepController() {}
}