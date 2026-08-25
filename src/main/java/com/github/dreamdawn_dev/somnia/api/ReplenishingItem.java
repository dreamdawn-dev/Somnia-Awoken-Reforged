package com.github.dreamdawn_dev.somnia.api;

import net.minecraft.world.item.Item;

public record ReplenishingItem(Item item, double replenishedFatigue, double fatigueRateModifier) {
}
