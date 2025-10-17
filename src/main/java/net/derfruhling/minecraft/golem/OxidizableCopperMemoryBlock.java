package net.derfruhling.minecraft.golem;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Oxidizable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class OxidizableCopperMemoryBlock extends CopperMemoryBlock implements Oxidizable {
    private final Oxidizable.OxidationLevel level;

    public OxidizableCopperMemoryBlock(Settings settings, OxidationLevel level) {
        super(settings);
        this.level = level;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(s -> new OxidizableCopperMemoryBlock(s, level));
    }

    @Override
    public Oxidizable.OxidationLevel getDegradationLevel() {
        return level;
    }
}
