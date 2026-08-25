package com.github.dreamdawn_dev.somnia.capability;

import com.github.dreamdawn_dev.somnia.SomniaAwoken;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class CapabilityFatigue {
    public static final Capability<Fatigue> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation NAME = new ResourceLocation(SomniaAwoken.MODID, "fatigue");

    private CapabilityFatigue() {}
}
