package net.derfruhling.minecraft.golem;

import net.minecraft.util.math.BlockPos;

import java.util.Iterator;

public class EmptyIterator implements Iterator<BlockPos> {
    public static final EmptyIterator INSTANCE = new EmptyIterator();

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public BlockPos next() {
        return null;
    }
}
