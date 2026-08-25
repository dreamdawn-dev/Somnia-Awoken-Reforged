package com.github.dreamdawn_dev.somnia;

import com.github.dreamdawn_dev.somnia.capability.CapabilityFatigue;
import com.github.dreamdawn_dev.somnia.gui.SomniaSleepScreen;
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
            // Only accelerated sleep (fatigue above the sleep threshold) gets the fade, Zzz and mute.
            // Rest-mode sleep just keeps the player lying in bed with no visual changes.
            boolean accelerated = this.mc.player.getCapability(CapabilityFatigue.INSTANCE)
                .map(fatigue -> fatigue.getFatigue() >= SomniaConfig.COMMON.minimumFatigueToSleep.get())
                .orElse(false);
            if (accelerated) {
                this.fadeState = SleepFadeState.FADE_IN;
                this.fadeStartMillis = System.currentTimeMillis();
                this.fadeStartAlpha = 0.0F;
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
        }
        else if (!sleeping && this.fadeState == SleepFadeState.FADE_OUT
            && System.currentTimeMillis() - this.fadeStartMillis >= SomniaConfig.CLIENT.fadeOutTicks.get() * 50L) {
            this.fadeState = SleepFadeState.NONE;
        }
        this.wasSleeping = sleeping;

        // Suppress the vanilla sleep overlay so our custom fade is the only one rendered
        this.mc.player.sleepCounter = 0;

        // Hide all HUD while sleeping, so only the wake-up button of the sleep screen stays visible
        if (sleeping && !this.hideGuiHidden) {
            this.hideGuiHidden = true;
            this.previousHideGui = this.mc.options.hideGui;
            this.mc.options.hideGui = true;
        }
        else if (!sleeping && this.hideGuiHidden) {
            this.hideGuiHidden = false;
            this.mc.options.hideGui = this.previousHideGui;
        }

        // Refresh the fatigue icon stage every 5 seconds (real time), matching the server's sync interval
        if (System.currentTimeMillis() - this.lastIconUpdate >= 5000) {
            this.lastIconUpdate = System.currentTimeMillis();
            this.mc.player.getCapability(CapabilityFatigue.INSTANCE)
                .ifPresent(fatigue -> this.fatigueIconStage = getFatigueStage(fatigue.getFatigue()));
        }

        // Volume follows the fade: gradually muted while falling asleep, restored while waking up.
        // The screen is fully black exactly when the volume reaches zero.
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

        if (hudAllowed && this.mc.player != null && !this.mc.player.isSpectator() && !this.mc.options.hideGui && SomniaConfig.COMMON.enableFatigue.get()) {
            renderFatigueIcon(guiGraphics, screenWidth, screenHeight);
        }

        // The fade keeps rendering even while a screen (e.g. the closing sleep screen) is open,
        // so the wake-up fade-out is never interrupted.
        if (this.fadeState == SleepFadeState.NONE || this.mc.level == null) return;

        float alpha = this.getFadeAlpha();
        if (alpha <= 0.0F) return;

        guiGraphics.fill(0, 0, screenWidth, screenHeight, (int) (255.0F * alpha) << 24);
        if (this.fadeState == SleepFadeState.FADE_IN) {
            // Zzz disappears as soon as the wake-up fade starts
            renderZzz(guiGraphics, screenWidth, screenHeight, alpha);
        }
    }

    @SubscribeEvent
    public void onScreenOpen(ScreenEvent.Opening event) {
        // Replace the vanilla sleep screen (which shows the chat bar) with one that only
        // contains the wake-up button, keeping the UI fully hidden while sleeping.
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
        int threshold = SomniaConfig.COMMON.minimumFatigueToSleep.get();
        if (fatigue < threshold) return 0;
        if (fatigue < 60) return 1;
        if (fatigue < 80) return 2;
        if (fatigue < 90) return 3;
        return 4;
    }

    private void renderFatigueIcon(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (this.fatigueIconStage < 0 || this.fatigueIconStage >= FATIGUE_ICONS.length) return;

        // Positioned relative to the screen center so GUI scaling can't break the layout.
        // The offsets are configurable in the client config (fatigueIconXOffset / fatigueIconYOffset).
        int x = screenWidth / 2 + SomniaConfig.CLIENT.fatigueIconXOffset.get();
        int y = screenHeight - SomniaConfig.CLIENT.fatigueIconYOffset.get();
        guiGraphics.blit(FATIGUE_ICONS[this.fatigueIconStage], x, y, 0, 0, FATIGUE_ICON_SIZE, FATIGUE_ICON_SIZE, FATIGUE_ICON_SIZE, FATIGUE_ICON_SIZE);
    }

    private void renderZzz(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float fadeAlpha) {
        // Slow breathing animation: from semi-transparent to fully opaque and back, one full cycle every 4 seconds
        long cycle = System.currentTimeMillis() % 4000;
        double breathe = 0.5D - 0.5D * Math.cos(2.0D * Math.PI * cycle / 4000.0D);
        float alpha = Mth.clamp(fadeAlpha * (0.3F + 0.7F * (float) breathe), 0.0F, 1.0F);
        int color = ((int) (255.0F * alpha) << 24) | 16777215;

        String text = "Zzz...";
        int x = (screenWidth - this.mc.font.width(text)) / 2;
        int y = screenHeight / 2 - this.mc.font.lineHeight / 2;
        guiGraphics.drawString(this.mc.font, text, x, y, color, false);
    }

    private ClientSleepHandler() {}
}
