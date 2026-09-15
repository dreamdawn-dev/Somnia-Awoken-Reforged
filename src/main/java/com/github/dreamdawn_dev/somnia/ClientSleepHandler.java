package com.github.dreamdawn_dev.somnia;

import com.github.dreamdawn_dev.somnia.capability.CapabilityFatigue;
import com.github.dreamdawn_dev.somnia.gui.SomniaSleepScreen;
import com.github.dreamdawn_dev.somnia.network.SomniaNetwork;
import com.github.dreamdawn_dev.somnia.network.server.FadeCompletePacket;
import com.github.dreamdawn_dev.somnia.util.SideEffectStage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientSleepHandler {
    public static final ClientSleepHandler INSTANCE = new ClientSleepHandler();

    private enum SleepFadeState {
        NONE,
        FADE_IN,
        FADE_OUT
    }

    private static final ResourceLocation[] FATIGUE_ICONS = new ResourceLocation[] {
        new ResourceLocation(SomniaAwoken.MODID, "textures/gui/side_effect_0.png"),
        new ResourceLocation(SomniaAwoken.MODID, "textures/gui/side_effect_1.png"),
        new ResourceLocation(SomniaAwoken.MODID, "textures/gui/side_effect_2.png"),
        new ResourceLocation(SomniaAwoken.MODID, "textures/gui/side_effect_3.png"),
        new ResourceLocation(SomniaAwoken.MODID, "textures/gui/side_effect_4.png")
    };
    private static final int FATIGUE_ICON_SIZE = 22;

    private final Minecraft mc = Minecraft.getInstance();

    private SleepFadeState fadeState = SleepFadeState.NONE;
    private long fadeStartMillis = -1;
    private float fadeStartAlpha;
    private boolean wasSleeping;
    private long zzzStartMillis = -1;
    private boolean fullBlackNotified;

    private int fatigueIconStage = -1;
    private long lastIconUpdate = -1;

    private boolean muted;
    private double previousVolume;
    private boolean hideGuiHidden;
    private boolean previousHideGui;

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || this.mc.player == null) return;

        boolean sleeping = this.mc.player.isSleeping();

        if (sleeping && !this.wasSleeping) {
            // 只有加速睡眠（疲劳值高于睡眠阈值）才会有淡入淡出、Zzz动画和静音效果。
            // 休息模式只是让玩家躺在床上，不会有任何视觉变化。
            boolean accelerated = this.mc.player.getCapability(CapabilityFatigue.INSTANCE)
                .map(fatigue -> fatigue.getFatigue() >= SomniaConfig.COMMON.minimumFatigueToSleep.get())
                .orElse(false);
            if (accelerated) {
                this.fadeState = SleepFadeState.FADE_IN;
                this.fadeStartMillis = System.currentTimeMillis();
                this.fadeStartAlpha = 0.0F;
                this.zzzStartMillis = -1;
                this.fullBlackNotified = false;
            }
            else {
                this.fadeState = SleepFadeState.NONE;
            }
        }
        else if (!sleeping && this.wasSleeping) {
            if (this.fadeState != SleepFadeState.NONE) {
                float currentAlpha = this.getFadeAlpha();
                this.fadeState = SleepFadeState.FADE_OUT;
                this.fadeStartMillis = System.currentTimeMillis();
                this.fadeStartAlpha = currentAlpha;
            }
            this.zzzStartMillis = -1;
        }
        else if (!sleeping && this.fadeState == SleepFadeState.FADE_OUT
            && System.currentTimeMillis() - this.fadeStartMillis >= SomniaConfig.CLIENT.fadeOutTicks.get() * 50L) {
            this.fadeState = SleepFadeState.NONE;
        }
        this.wasSleeping = sleeping;

        // 完全黑屏的时刻：Zzz开始淡入，并通知服务端开始恢复疲劳
        if (this.fadeState == SleepFadeState.FADE_IN && !this.fullBlackNotified && this.getFadeAlpha() >= 1.0F) {
            this.fullBlackNotified = true;
            this.zzzStartMillis = System.currentTimeMillis();
            SomniaNetwork.INSTANCE.sendToServer(new FadeCompletePacket());
        }

        // 抑制原版睡眠叠加层，只渲染我们自定义的淡入淡出效果
        this.mc.player.sleepCounter = 0;

        // 睡眠时隐藏所有HUD，只保留睡眠界面的起床按钮
        if (sleeping && !this.hideGuiHidden) {
            this.hideGuiHidden = true;
            this.previousHideGui = this.mc.options.hideGui;
            this.mc.options.hideGui = true;
        }
        else if (!sleeping && this.hideGuiHidden) {
            this.hideGuiHidden = false;
            this.mc.options.hideGui = this.previousHideGui;
        }

        // 每5秒（现实时间）刷新一次疲劳值图标阶段，与服务器的同步间隔保持一致
        if (System.currentTimeMillis() - this.lastIconUpdate >= 5000) {
            this.lastIconUpdate = System.currentTimeMillis();
            this.mc.player.getCapability(CapabilityFatigue.INSTANCE)
                .ifPresent(fatigue -> this.fatigueIconStage = getFatigueStage(fatigue.getFatigue()));
        }

        // 音量随淡入淡出变化：入睡时逐渐静音，起床时逐渐恢复。
        // 当音量降至零时，屏幕恰好完全变黑。
        if (this.fadeState != SleepFadeState.NONE) {
            if (!this.muted) {
                this.muted = true;
                this.previousVolume = this.mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).get();
            }
            float alpha = this.getFadeAlpha();
            double volume = this.fadeState == SleepFadeState.FADE_IN ? this.previousVolume * (1.0D - alpha) : this.previousVolume * alpha;
            this.mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(volume);
        }
        else if (this.muted) {
            this.muted = false;
            this.mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(this.previousVolume);
        }
    }

    public void renderGuiOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        boolean sleeping = this.mc.player != null && this.mc.player.isSleeping();
        boolean hudAllowed = this.mc.screen == null || this.mc.screen instanceof PauseScreen || sleeping;

        // 即使有界面打开（例如正在关闭的睡眠界面），淡入淡出也会持续渲染，
        // 这样起床时的淡出效果就不会被打断。
        // 遮罩注册在belowAll（物品栏等原版HUD之下），因此疲劳图标画在遮罩之上：
        // 醒来时它和物品栏一样立即恢复，不参与屏幕从全黑到正常的过渡，保证二者表现一致。
        if (this.fadeState != SleepFadeState.NONE && this.mc.level != null) {
            float alpha = this.getFadeAlpha();
            if (alpha > 0.0F) {
                guiGraphics.fill(0, 0, screenWidth, screenHeight, (int) (255.0F * alpha) << 24);
                // Zzz动画在屏幕完全变黑后才出现（自身先淡入约2秒），醒来淡出效果一开始就立即消失
                if (this.fadeState == SleepFadeState.FADE_IN && alpha >= 1.0F) {
                    renderZzz(guiGraphics, screenWidth, screenHeight);
                }
            }
        }

        if (hudAllowed && this.mc.player != null && !this.mc.player.isSpectator() && !this.mc.options.hideGui && SomniaConfig.COMMON.enableFatigue.get()) {
            renderFatigueIcon(guiGraphics, screenWidth, screenHeight);
        }
    }

    @SubscribeEvent
    public void onScreenOpen(ScreenEvent.Opening event) {
        // 替换原版睡眠界面（会显示聊天栏），改为只包含起床按钮的界面，
        // 让睡眠时UI完全隐藏。
        if (event.getScreen() instanceof InBedChatScreen) {
            event.setNewScreen(new SomniaSleepScreen());
        }
    }

    private float getFadeAlpha() {
        long elapsed = System.currentTimeMillis() - this.fadeStartMillis;
        return switch (this.fadeState) {
            case FADE_IN -> Mth.clamp(elapsed / (float) (SomniaConfig.CLIENT.fadeInTicks.get() * 50L), 0.0F, 1.0F);
            case FADE_OUT -> Mth.clamp(this.fadeStartAlpha * (1.0F - elapsed / (float) (SomniaConfig.CLIENT.fadeOutTicks.get() * 50L)), 0.0F, 1.0F);
            default -> 0.0F;
        };
    }

    private static int getFatigueStage(double fatigue) {
        // 图标分界与配置的负面效果阶段保持一致：
        // 门槛 = 睡眠阈值 + 前三个效果阶段的起始疲劳值，改配置后图标自动跟随。
        int stage = fatigue >= SomniaConfig.COMMON.minimumFatigueToSleep.get() ? 1 : 0;
        SideEffectStage[] stages = SideEffectStage.getSideEffectStages();
        for (int i = 0; i < Math.min(stages.length, 3); i++) {
            if (fatigue >= stages[i].minFatigue()) stage = i + 2;
        }
        return stage;
    }

    private void renderFatigueIcon(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (this.fatigueIconStage < 0 || this.fatigueIconStage >= FATIGUE_ICONS.length) return;

        // 相对于屏幕中心定位，这样GUI缩放不会破坏布局。
        // 偏移量可在客户端配置中调整（fatigueIconXOffset / fatigueIconYOffset）。
        int x = screenWidth / 2 + SomniaConfig.CLIENT.fatigueIconXOffset.get();
        int y = screenHeight - SomniaConfig.CLIENT.fatigueIconYOffset.get();
        guiGraphics.blit(FATIGUE_ICONS[this.fatigueIconStage], x, y, 0, 0, FATIGUE_ICON_SIZE, FATIGUE_ICON_SIZE, FATIGUE_ICON_SIZE, FATIGUE_ICON_SIZE);
    }

    private void renderZzz(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        // Zzz出现后先淡入约2秒，随后进入呼吸动画循环（每4秒一个完整周期）
        long start = this.zzzStartMillis < 0 ? System.currentTimeMillis() : this.zzzStartMillis;
        float fadeIn = Mth.clamp((System.currentTimeMillis() - start) / 2000.0F, 0.0F, 1.0F);

        long cycle = System.currentTimeMillis() % 4000;
        double breathe = 0.5D - 0.5D * Math.cos(2.0D * Math.PI * cycle / 4000.0D);
        float alpha = Mth.clamp(fadeIn * (0.3F + 0.7F * (float) breathe), 0.0F, 1.0F);
        int alphaByte = (int) (255.0F * alpha);
        // 字体渲染器会把alpha字节<4的颜色强制变为不透明（Font.adjustColor），
        // 淡入起点会因此闪出一条纯白的Zzz，所以透明度低于该阈值时直接不画
        if (alphaByte < 4) return;
        int color = (alphaByte << 24) | 16777215;

        String text = "Zzz...";
        int x = (screenWidth - this.mc.font.width(text)) / 2;
        int y = screenHeight / 2 - this.mc.font.lineHeight / 2;
        guiGraphics.drawString(this.mc.font, text, x, y, color, false);
    }

    private ClientSleepHandler() {}
}