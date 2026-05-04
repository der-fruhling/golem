package net.derfruhling.minecraft.golem;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class CopperMemoryBlockEntity extends BlockEntity {
    private final Hashtable<Integer, List<BlockPos>> items = new Hashtable<>();
    private final Hashtable<BlockPos, Set<Integer>> containers = new Hashtable<>();

    private static final Codec<Map<Identifier, List<BlockPos>>> MAP_CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.list(BlockPos.CODEC));

    public CopperMemoryBlockEntity(BlockPos pos, BlockState state) {
        super(Golem.MEMORY_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected synchronized void saveAdditional(@NonNull ValueOutput out) {
        super.saveAdditional(out);
        out.store("items", MAP_CODEC, ImmutableMap.copyOf(items.entrySet().stream().map(e -> new Map.Entry<Identifier, List<BlockPos>>() {
            @Override
            public Identifier getKey() {
                return BuiltInRegistries.ITEM.get(e.getKey()).map(v -> v.key().identifier()).orElse(null);
            }

            @Override
            public List<BlockPos> getValue() {
                return e.getValue();
            }

            @Override
            public List<BlockPos> setValue(List<BlockPos> value) {
                return e.setValue(value);
            }
        }).toList()));
    }

    private void addContainer(int item, BlockPos pos) {
        containers.compute(pos, (k, v) -> {
            if (v == null) return new TreeSet<>(List.of(item));

            v.add(item);
            return v;
        });
    }

    private void removeContainer(int item, BlockPos pos) {
        containers.computeIfPresent(pos, (k, v) -> {
            v.remove(item);
            return v;
        });
    }

    @Override
    protected synchronized void loadAdditional(@NonNull ValueInput in) {
        super.loadAdditional(in);

        items.clear();
        var map = in.read("items", MAP_CODEC);

        if(map.isPresent()) {
            for(var e : map.get().entrySet()) {
                if(!BuiltInRegistries.ITEM.containsKey(e.getKey())) continue;
                int item = BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.getValue(e.getKey()));
                items.put(item, new ArrayList<>(e.getValue()));

                for(BlockPos pos : e.getValue()) {
                    addContainer(item, pos);
                }
            }
        }
    }

    private List<BlockPos> get(int item) {
        return items.get(item);
    }

    @Nullable
    public synchronized List<BlockPos> get(Item item) {
        return get(BuiltInRegistries.ITEM.getId(item));
    }

    public synchronized void found(Item item, BlockPos pos) {
        int itemId = BuiltInRegistries.ITEM.getId(item);
        List<BlockPos> loc = get(itemId);

        if(loc != null) {
            if(!loc.contains(pos)) loc.add(pos);

            while(loc.size() > 10) {
                removeContainer(itemId, loc.removeFirst());
            }
        } else {
            items.put(itemId, new ArrayList<>(List.of(pos)));
        }

        addContainer(itemId, pos);
        setChanged();
    }

    public synchronized void dead(Item item, BlockPos pos) {
        int itemId = BuiltInRegistries.ITEM.getId(item);
        List<BlockPos> loc = get(itemId);

        if(loc != null) {
            loc.remove(pos);
            if(loc.isEmpty()) {
                items.remove(itemId);
            }
        }

        removeContainer(itemId, pos);
        setChanged();
    }
}
