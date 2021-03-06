package com.github.upcraftlp.netweight.loot;

import com.github.glasspane.mesh.util.CollectionHelper;
import com.github.upcraftlp.netweight.NetWeight;
import com.github.upcraftlp.netweight.api.FishType;
import com.github.upcraftlp.netweight.init.NWItems;
import com.github.upcraftlp.netweight.util.FishingDataManager;
import com.github.upcraftlp.netweight.util.NwHooks;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class NWFishingLootEntry extends LeafEntry {

    public NWFishingLootEntry(int weight, int quality, LootCondition[] conditions, LootFunction[] functions) {
        super(weight, quality, conditions, functions);
    }

    @Override
    protected void drop(Consumer<ItemStack> itemDropper, LootContext context) {
        BlockPos pos = Objects.requireNonNull(context.get(LootContextParameters.POSITION));
        ItemStack bait = context.hasParameter(NwHooks.LOOT_CONTEXT_BAIT) ? Objects.requireNonNull(context.get(NwHooks.LOOT_CONTEXT_BAIT)) : new ItemStack(NWItems.CREATIVE_BAIT);
        ServerWorld world = context.getWorld();
        boolean raining = world.hasRain(pos);
        boolean thundering = world.isThundering();
        Biome biome = world.getBiome(pos);
        List<FishType> types = FishingDataManager.FISH_TYPES.values().stream()
                .filter(fishType -> fishType.isBait(bait) || bait.getItem() == NWItems.CREATIVE_BAIT)
                .filter(fishType -> raining || !fishType.requiresRain())
                .filter(fishType -> thundering || !fishType.requiresThunder())
                .filter(fishType -> pos.getY() >= fishType.getMinHeight() && pos.getY() <= fishType.getMaxHeight())
                .filter(fishType -> biome.getTemperature() >= fishType.getMinTemperature() && biome.getTemperature() <= fishType.getMaxTemperature())
                .collect(Collectors.toList());
        NetWeight.logger.trace("Fishing! temp: {}, at: {}, fishes: {}", biome.getTemperature(), pos, StringUtils.join(types, ","));
        if(!types.isEmpty()) {
            FishType result = CollectionHelper.getRandomElement(types, context.getRandom());
            ItemStack fish = result.getFish().copy();
            NwHooks.setFishWeight(fish, result.getMinWeight(), result.getMaxWeight(), context.getRandom(), context.getLuck());
            itemDropper.accept(fish);
        }
    }

    public static class Serializer extends LeafEntry.Serializer<NWFishingLootEntry> {

        public Serializer(Identifier id) {
            super(id, NWFishingLootEntry.class);
        }

        @Override
        protected NWFishingLootEntry fromJson(JsonObject entryJson, JsonDeserializationContext context, int weight, int quality, LootCondition[] conditions, LootFunction[] functions) {
            return new NWFishingLootEntry(weight, quality, conditions, functions);
        }
    }
}
