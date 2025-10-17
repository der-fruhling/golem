package net.derfruhling.minecraft.golem.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.derfruhling.minecraft.golem.Golem;
import net.minecraft.entity.passive.CopperGolemBrain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;
import java.util.stream.Stream;

@Mixin(CopperGolemBrain.class)
public class CopperGolemBrainMixin {
    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", remap = false, target = "Lcom/google/common/collect/ImmutableList;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;"))
    private static ImmutableList<?> addMemories(Object e1, Object e2, Object e3, Object e4, Object e5, Object e6, Object e7, Object e8, Object e9, Object e10, Object e11, Object e12, Object[] others, Operation<ImmutableList<?>> original) {
        return original.call(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, Stream.of(others, new Object[]{
                Golem.SHARED_MEMORY,
                Golem.REMAINING_MEMORY_HITS,
                Golem.TRIED_SHARED_MEMORY
        }).flatMap(Arrays::stream).toArray());
    }
}
