package net.derfruhling.minecraft.golem.mixin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.derfruhling.minecraft.golem.CopperMemoryBlockEntity;
import net.derfruhling.minecraft.golem.EmptyIterator;
import net.derfruhling.minecraft.golem.Golem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.BiConsumer;

@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin extends Behavior<PathfinderMob> {
    @Shadow
    private static boolean isPickingUpItems(PathfinderMob entity) {
        return false;
    }

    @Unique
    private final Map<Item, BlockPos> deadItemCache = new HashMap<>();

    private TransportItemsBetweenContainersMixin(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState) {
        super(requiredMemoryState);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false, target = "Lcom/google/common/collect/ImmutableMap;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableMap;"))
    private static ImmutableMap<?, ?> addMemoryStates(Object k1, Object v1, Object k2, Object v2, Object k3, Object v3, Object k4, Object v4) {
        return ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4, Golem.SHARED_MEMORY, MemoryStatus.REGISTERED, Golem.TRIED_SHARED_MEMORY, MemoryStatus.REGISTERED, Golem.REMAINING_MEMORY_HITS, MemoryStatus.REGISTERED);
    }

    @Unique
    private Iterator<BlockPos> getTryNext(PathfinderMob entity) {
        return entity.getBrain().getMemory(Golem.REMAINING_MEMORY_HITS).orElse(EmptyIterator.INSTANCE);
    }

    @Unique
    private boolean getTriedMemoryBlock(PathfinderMob entity) {
        return entity.getBrain().getMemory(Golem.TRIED_SHARED_MEMORY).orElse(false);
    }

    @Unique
    @Nullable
    private CopperMemoryBlockEntity getMemory(ServerLevel world, PathfinderMob entity) {
        BlockEntity e = entity.getBrain().getMemory(Golem.SHARED_MEMORY)
            .map(world::getBlockEntity)
            .orElse(null);
        if(e instanceof CopperMemoryBlockEntity c) {
            return c;
        } else return null;
    }

    @ModifyArg(method = {"onReachedTarget", "startOnReachedTargetInteraction"}, index = 4, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers;doReachedTargetInteraction(Lnet/minecraft/world/entity/PathfinderMob;Lnet/minecraft/world/Container;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;)V"))
    private BiConsumer<PathfinderMob, Container> markFound(
            BiConsumer<PathfinderMob, Container> pickupItemCallback,
            @Local(argsOnly = true, name = "target") TransportItemsBetweenContainers.TransportItemTarget target
    ) {
        return (entity, inv) -> {
            CopperMemoryBlockEntity memory = getMemory((ServerLevel) entity.level(), entity);
            if(memory != null) {
                memory.found(entity.getMainHandItem().getItem(), target.pos());
            }

            pickupItemCallback.accept(entity, inv);
        };
    }

    @ModifyArg(method = {"onReachedTarget", "startOnReachedTargetInteraction"}, index = 5, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers;doReachedTargetInteraction(Lnet/minecraft/world/entity/PathfinderMob;Lnet/minecraft/world/Container;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;)V"))
    private BiConsumer<PathfinderMob, Container> markNotFound(
            BiConsumer<PathfinderMob, Container> pickupNoItemCallback,
            @Local(argsOnly = true, name = "target") TransportItemsBetweenContainers.TransportItemTarget target
    ) {
        return (entity, inv) -> {
            CopperMemoryBlockEntity memory = getMemory((ServerLevel) entity.level(), entity);
            var item = entity.getMainHandItem().getItem();
            if(memory != null && !inv.hasAnyMatching(s -> s.getItem() == item)) {
                memory.dead(item, target.pos());
            }

            pickupNoItemCallback.accept(entity, inv);
        };
    }

    @Inject(method = "getTransportTarget", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", ordinal = 0), cancellable = true)
    private void findStorage(
            ServerLevel level,
            PathfinderMob body,
            CallbackInfoReturnable<Optional<TransportItemsBetweenContainers.TransportItemTarget>> cir
    ) {
        CopperMemoryBlockEntity memory = getMemory(level, body);
        if(memory != null && !isPickingUpItems(body)) {
            Iterator<BlockPos> tryNext = getTryNext(body);
            ItemStack stack = body.getMainHandItem();

            if(!getTriedMemoryBlock(body) && !tryNext.hasNext()) {
                List<BlockPos> loc = memory.get(stack.getItem());
                if(loc != null) {
                    tryNext = ImmutableList.copyOf(loc).iterator();
                    body.getBrain().setMemory(Golem.REMAINING_MEMORY_HITS, tryNext);
                    body.getBrain().setMemory(Golem.TRIED_SHARED_MEMORY, true);
                } else {
                    return;
                }
            }

            while(tryNext.hasNext()) {
                BlockPos pos = tryNext.next();
                BlockEntity newBlockEntity = level.getBlockEntity(pos);
                if(newBlockEntity instanceof ChestBlockEntity) {
                    cir.setReturnValue(Optional.ofNullable(TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(newBlockEntity, level)));
                    return;
                } else {
                    deadItemCache.put(stack.getItem(), pos);
                }
            }
            deadItemCache.forEach(memory::dead);
            deadItemCache.clear();
        }
    }

    @Unique
    private double dist(BlockPos pos1, BlockPos pos2) {
        var a = pos2.getCenter().subtract(pos1.getCenter());
        return Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);
    }

    @WrapOperation(method = "getTransportTarget", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Iterator;next()Ljava/lang/Object;", ordinal = 1))
    Object findStorageNext(
            Iterator<?> instance,
            Operation<Object> original,
            ServerLevel level,
            PathfinderMob body
    ) {
        Object obj = original.call(instance);

        if(obj instanceof CopperMemoryBlockEntity e) {
            CopperMemoryBlockEntity mem = getMemory(level, body);
            if(mem == null || dist(body.blockPosition(), mem.getBlockPos()) > dist(body.blockPosition(), e.getBlockPos())) {
                body.getBrain().setMemory(Golem.SHARED_MEMORY, e.getBlockPos());
            }
        }

        return obj;
    }

    @Inject(method = {"clearMemoriesAfterMatchingTargetFound", "enterCooldownAfterNoMatchingTargetFound"}, at = @At("TAIL"))
    void resetMemoryState(PathfinderMob body, CallbackInfo ci) {
        body.getBrain().eraseMemory(Golem.SHARED_MEMORY);
        body.getBrain().eraseMemory(Golem.TRIED_SHARED_MEMORY);
        body.getBrain().eraseMemory(Golem.REMAINING_MEMORY_HITS);
    }
}
