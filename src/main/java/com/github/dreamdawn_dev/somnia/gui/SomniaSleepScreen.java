package com.github.dreamdawn_dev.somnia.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

/**
 * Replacement for the vanilla {@link net.minecraft.client.gui.screens.InBedChatScreen} while sleeping.
 * Unlike the vanilla screen it contains no chat bar or chat history - only the wake-up button,
 * so the UI is fully hidden during sleep.
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
        if (keyCode == 256) { // ESC wakes the player up, like the vanilla sleep screen
            this.wakeUp();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // No background, no chat - only the wake-up button, so the black fade below stays visible
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
