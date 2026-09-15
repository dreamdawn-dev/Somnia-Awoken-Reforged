package com.github.dreamdawn_dev.somnia.capability;

import com.github.dreamdawn_dev.somnia.SomniaAwoken;
import com.github.dreamdawn_dev.somnia.network.SomniaNetwork;
import com.github.dreamdawn_dev.somnia.network.client.FatigueUpdatePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SomniaAwoken.MODID)
public final class CapabilitySync {

    @SubscribeEvent
    public static void onEntityCapabilityAttach(AttachCapabilitiesEvent<Entity> event) {
        // 疲劳系统只对玩家有意义，跳过其他实体，避免为每个怪物/动物/掉落物都创建实例
        if (!(event.getObject() instanceof Player)) return;
        event.addCapability(CapabilityFatigue.NAME, new CapabilityFatigueProvider());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sync((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.getEntity().level().isClientSide && event.isWasDeath()) {
            event.getOriginal().getCapability(CapabilityFatigue.INSTANCE)
                .ifPresent(props -> {
                    CompoundTag old = props.serializeNBT();
                    event.getEntity().getCapability(CapabilityFatigue.INSTANCE)
                        .ifPresent(fatigue -> fatigue.deserializeNBT(old));
                });
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        event.getEntity().getCapability(CapabilityFatigue.INSTANCE)
            .ifPresent(props -> {
                props.setFatigue(0);
                props.setReplenishedFatigue(0);
                props.setExtraFatigueRate(0);
            });
    }

    private static void sync(ServerPlayer player) {
        player.getCapability(CapabilityFatigue.INSTANCE)
            .ifPresent(props -> SomniaNetwork.sendToClient(new FatigueUpdatePacket(props.getFatigue()), player));
    }

    private CapabilitySync() {}
}
