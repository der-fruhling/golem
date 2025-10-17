package net.derfruhling.minecraft.golem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.FabricRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

import static net.minecraft.registry.Registries.*;

public class Golem implements ModInitializer {
    public static final String MOD_ID = "golem";

    private static final List<BlockItem> items = new ArrayList<>();

    private static <T> RegistryKey<T> id(Registry<T> reg, String id) {
        return RegistryKey.of(reg.getKey(), Identifier.of(MOD_ID, id));
    }

    private static <T extends Block> @NotNull T registerBlock(String name, Function<AbstractBlock.Settings, T> function) {
        var block = function.apply(AbstractBlock.Settings.create().registryKey(id(BLOCK, name)).mapColor(Blocks.COPPER_BLOCK.getDefaultMapColor()).strength(3.0F, 6.0F).sounds(BlockSoundGroup.COPPER_BULB).requiresTool().solidBlock(Blocks::never));
        var item = new BlockItem(block, new Item.Settings().registryKey(id(ITEM, name)));
        items.add(Registry.register(ITEM, id(ITEM, name), item));
        return Registry.register(BLOCK, id(BLOCK, name), block);
    }

    public static final CopperMemoryBlock MEMORY_BLOCK = registerBlock("memory_block", s -> new OxidizableCopperMemoryBlock(s, Oxidizable.OxidationLevel.UNAFFECTED));
    public static final CopperMemoryBlock EXPOSED_MEMORY_BLOCK = registerBlock("exposed_memory_block", s -> new OxidizableCopperMemoryBlock(s, Oxidizable.OxidationLevel.EXPOSED));
    public static final CopperMemoryBlock WEATHERED_MEMORY_BLOCK = registerBlock("weathered_memory_block", s -> new OxidizableCopperMemoryBlock(s, Oxidizable.OxidationLevel.WEATHERED));
    public static final CopperMemoryBlock OXIDIZED_MEMORY_BLOCK = registerBlock("oxidized_memory_block", s -> new OxidizableCopperMemoryBlock(s, Oxidizable.OxidationLevel.OXIDIZED));
    public static final CopperMemoryBlock WAXED_MEMORY_BLOCK = registerBlock("waxed_memory_block", CopperMemoryBlock::new);
    public static final CopperMemoryBlock WAXED_EXPOSED_MEMORY_BLOCK = registerBlock("waxed_exposed_memory_block", CopperMemoryBlock::new);
    public static final CopperMemoryBlock WAXED_WEATHERED_MEMORY_BLOCK = registerBlock("waxed_weathered_memory_block", CopperMemoryBlock::new);
    public static final CopperMemoryBlock WAXED_OXIDIZED_MEMORY_BLOCK = registerBlock("waxed_oxidized_memory_block", CopperMemoryBlock::new);
    public static final BlockEntityType<CopperMemoryBlockEntity> MEMORY_BLOCK_ENTITY = Registry.register(
            BLOCK_ENTITY_TYPE,
            id(BLOCK_ENTITY_TYPE, "memory"),
            FabricBlockEntityTypeBuilder.create(
                    CopperMemoryBlockEntity::new,
                    MEMORY_BLOCK,
                    EXPOSED_MEMORY_BLOCK,
                    WEATHERED_MEMORY_BLOCK,
                    OXIDIZED_MEMORY_BLOCK,
                    WAXED_MEMORY_BLOCK,
                    WAXED_EXPOSED_MEMORY_BLOCK,
                    WAXED_WEATHERED_MEMORY_BLOCK,
                    WAXED_OXIDIZED_MEMORY_BLOCK
            ).build()
    );
    public static final MemoryModuleType<BlockPos> SHARED_MEMORY = Registry.register(MEMORY_MODULE_TYPE, id(MEMORY_MODULE_TYPE, "shared_memory"), new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));
    public static final MemoryModuleType<Boolean> TRIED_SHARED_MEMORY = Registry.register(MEMORY_MODULE_TYPE, id(MEMORY_MODULE_TYPE, "tried_shared_memory"), new MemoryModuleType<>(Optional.empty()));
    public static final MemoryModuleType<Iterator<BlockPos>> REMAINING_MEMORY_HITS = Registry.register(MEMORY_MODULE_TYPE, id(MEMORY_MODULE_TYPE, "remaining_memory_hits"), new MemoryModuleType<>(Optional.empty()));

    @Override
    public void onInitialize() {
        OxidizableBlocksRegistry.registerCopperBlockSet(new CopperBlockSet(
                MEMORY_BLOCK,
                EXPOSED_MEMORY_BLOCK,
                WEATHERED_MEMORY_BLOCK,
                OXIDIZED_MEMORY_BLOCK,
                WAXED_MEMORY_BLOCK,
                WAXED_EXPOSED_MEMORY_BLOCK,
                WAXED_WEATHERED_MEMORY_BLOCK,
                WAXED_OXIDIZED_MEMORY_BLOCK
        ));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(g -> {
            g.addAll(items.stream().map(ItemStack::new).toList());
        });
    }
}
