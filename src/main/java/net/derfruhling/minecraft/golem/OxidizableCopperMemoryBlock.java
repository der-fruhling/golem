package net.derfruhling.minecraft.golem;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import org.jspecify.annotations.NonNull;

public class OxidizableCopperMemoryBlock extends CopperMemoryBlock implements WeatheringCopper {
    private final WeatheringCopper.WeatherState level;

    public OxidizableCopperMemoryBlock(Properties settings, WeatheringCopper.WeatherState level) {
        super(settings);
        this.level = level;
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(s -> new OxidizableCopperMemoryBlock(s, level));
    }

    @Override
    public WeatheringCopper.@NonNull WeatherState getAge() {
        return level;
    }
}
