package net.derfruhling.minecraft.golem;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CopperMemoryBlockEntity extends BlockEntity {
    private final Hashtable<Integer, List<BlockPos>> items = new Hashtable<>();
    private final Hashtable<BlockPos, Set<Integer>> containers = new Hashtable<>();

    private static final Codec<Map<Identifier, List<BlockPos>>> MAP_CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.list(BlockPos.CODEC));

    public CopperMemoryBlockEntity(BlockPos pos, BlockState state) {
        super(Golem.MEMORY_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.put("items", MAP_CODEC, ImmutableMap.copyOf(items.entrySet().stream().map(e -> new Map.Entry<Identifier, List<BlockPos>>() {
            @Override
            public Identifier getKey() {
                return Registries.ITEM.getId(Registries.ITEM.get(e.getKey()));
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
    protected void readData(ReadView view) {
        super.readData(view);

        items.clear();
        var map = view.read("items", MAP_CODEC);

        if(map.isPresent()) {
            for(var e : map.get().entrySet()) {
                if(!Registries.ITEM.containsId(e.getKey())) continue;
                int item = Registries.ITEM.getRawId(Registries.ITEM.get(e.getKey()));
                items.put(item, e.getValue());

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
        return get(Registries.ITEM.getRawId(item));
    }

    public synchronized void found(Item item, BlockPos pos) {
        int itemId = Registries.ITEM.getRawId(item);
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
        markDirty();
    }

    public synchronized void dead(Item item, BlockPos pos) {
        int itemId = Registries.ITEM.getRawId(item);
        List<BlockPos> loc = get(itemId);

        if(loc != null) {
            loc.remove(pos);
            if(loc.isEmpty()) {
                items.remove(itemId);
            }
        }

        removeContainer(itemId, pos);
        markDirty();
    }
}
