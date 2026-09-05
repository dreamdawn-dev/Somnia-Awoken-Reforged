package com.github.dreamdawn_dev.somnia.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

/**
 * 睡眠时替换原版{@link net.minecraft.client.gui.screens.InBedChatScreen}的界面。
 * 与原版界面不同，它不包含聊天栏或聊天历史——只有起床按钮，
 * 这样睡眠时UI就完全隐藏了。
 */
public class SomniaSleepScreen extends Screen {
    private Button wakeUpButton;

    public SomniaSleepScreen() {
        super(Component.translatable("multiplayer.stopSleeping"));
    }

    @Override
    protected void init() {
        this.wakeUpButton = Button.builder(Component.translatable("multiplayer.stopSleeping"), button -> wakeUp())
            .bounds(this.width / 2 - 100, this.height - 40, 200, 20)
            .build();
        this.addRenderableWidget(this.wakeUpButton);
    }

    @Override
    public void tick() {
        if (this.minecraft != null && this.minecraft.player != null && !this.minecraft.player.isSleeping()) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC键唤醒玩家，与原版睡眠界面一致
            this.wakeUp();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 无背景、无聊天——只有起床按钮，这样下方的黑色淡入淡出效果保持可见
        this.wakeUpButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void wakeUp() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ClientPacketListener connection = mc.player.connection;
            connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SLEEPING));
        }
    }
}