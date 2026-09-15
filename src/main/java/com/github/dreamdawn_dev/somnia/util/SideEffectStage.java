package com.github.dreamdawn_dev.somnia.util;

import com.github.dreamdawn_dev.somnia.SomniaConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public record SideEffectStage(int minFatigue, int maxFatigue, ResourceLocation effect, int duration, int amplifier) {

    // 每次调用都重新解析配置，这样运行时修改配置后无需重启即可生效
    public static SideEffectStage[] getSideEffectStages() {
        List<? extends List<Object>> sideEffectStages = SomniaConfig.COMMON.sideEffectStages.get();
        SideEffectStage[] stages = new SideEffectStage[sideEffectStages.size()];
        for (int i = 0; i < stages.length; i++) {
            stages[i] = parseStage(sideEffectStages.get(i));
        }
        return stages;
    }

    private static SideEffectStage parseStage(List<Object> stage) {
        return new SideEffectStage((int) stage.get(0), (int) stage.get(1), new ResourceLocation((String) stage.get(2)), (int) stage.get(3), (int) stage.get(4));
    }

    public MobEffect getEffect() {
        return ForgeRegistries.MOB_EFFECTS.getValue(this.effect);
    }
}
