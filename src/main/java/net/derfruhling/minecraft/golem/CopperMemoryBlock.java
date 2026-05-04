package net.derfruhling.minecraft.golem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class CopperMemoryBlock extends BaseEntityBlock {
    public CopperMemoryBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(CopperMemoryBlock::new);
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new CopperMemoryBlockEntity(pos, state);
    }
}
