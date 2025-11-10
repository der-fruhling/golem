package net.derfruhling.minecraft.golem.mixin;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.derfruhling.minecraft.golem.CopperMemoryBlockEntity;
import net.derfruhling.minecraft.golem.EmptyIterator;
import net.derfruhling.minecraft.golem.Golem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.MoveItemsTask;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
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

@Mixin(MoveItemsTask.class)
public abstract class MoveItemsTaskMixin extends MultiTickTask<PathAwareEntity> {
    @Shadow
    private static boolean canPickUpItem(PathAwareEntity entity) {
        return false;
    }

    @Unique
    private final Map<Item, BlockPos> deadItemCache = new HashMap<>();

    private MoveItemsTaskMixin(Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryState) {
        super(requiredMemoryState);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false, target = "Lcom/google/common/collect/ImmutableMap;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableMap;"))
    private static ImmutableMap<?, ?> addMemoryStates(Object k1, Object v1, Object k2, Object v2, Object k3, Object v3, Object k4, Object v4) {
        return ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4, Golem.SHARED_MEMORY, MemoryModuleState.REGISTERED, Golem.TRIED_SHARED_MEMORY, MemoryModuleState.REGISTERED, Golem.REMAINING_MEMORY_HITS, MemoryModuleState.REGISTERED);
    }

    @Unique
    private Iterator<BlockPos> getTryNext(PathAwareEntity entity) {
        return entity.getBrain().getOptionalRegisteredMemory(Golem.REMAINING_MEMORY_HITS).orElse(EmptyIterator.INSTANCE);
    }

    @Unique
    private boolean getTriedMemoryBlock(PathAwareEntity entity) {
        return entity.getBrain().getOptionalRegisteredMemory(Golem.TRIED_SHARED_MEMORY).orElse(false);
    }

    @Unique
    @Nullable
    private CopperMemoryBlockEntity getMemory(ServerWorld world, PathAwareEntity entity) {
        BlockEntity e = entity.getBrain().getOptionalRegisteredMemory(Golem.SHARED_MEMORY)
                .map(world::getBlockEntity)
                .orElse(null);
        if(e instanceof CopperMemoryBlockEntity c) {
            return c;
        } else return null;
    }

    @ModifyArg(method = {"transitionToInteracting", "tickInteracting"}, index = 4, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/brain/task/MoveItemsTask;selectInteractionState(Lnet/minecraft/entity/mob/PathAwareEntity;Lnet/minecraft/inventory/Inventory;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;)V"))
    private BiConsumer<PathAwareEntity, Inventory> markFound(
            BiConsumer<PathAwareEntity, Inventory> pickupItemCallback,
            @Local(argsOnly = true) MoveItemsTask.Storage storage
    ) {
        return (entity, inv) -> {
            CopperMemoryBlockEntity memory = getMemory((ServerWorld) entity.getEntityWorld(), entity);
            if(memory != null) {
                memory.found(entity.getMainHandStack().getItem(), storage.pos());
            }

            pickupItemCallback.accept(entity, inv);
        };
    }

    @ModifyArg(method = {"transitionToInteracting", "tickInteracting"}, index = 5, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/brain/task/MoveItemsTask;selectInteractionState(Lnet/minecraft/entity/mob/PathAwareEntity;Lnet/minecraft/inventory/Inventory;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;)V"))
    private BiConsumer<PathAwareEntity, Inventory> markNotFound(
            BiConsumer<PathAwareEntity, Inventory> pickupNoItemCallback,
            @Local(argsOnly = true) MoveItemsTask.Storage storage
    ) {
        return (entity, inv) -> {
            CopperMemoryBlockEntity memory = getMemory((ServerWorld) entity.getEntityWorld(), entity);
            var item = entity.getMainHandStack().getItem();
            if(memory != null && !inv.containsAny(s -> s.getItem() == item)) {
                memory.dead(item, storage.pos());
            }

            pickupNoItemCallback.accept(entity, inv);
        };
    }

    @Inject(method = "findStorage", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", ordinal = 0), cancellable = true)
    private void findStorage(
            ServerWorld world,
            PathAwareEntity entity,
            CallbackInfoReturnable<Optional<MoveItemsTask.Storage>> cir
    ) {
        CopperMemoryBlockEntity memory = getMemory(world, entity);
        if(memory != null && !canPickUpItem(entity)) {
            Iterator<BlockPos> tryNext = getTryNext(entity);
            ItemStack stack = entity.getMainHandStack();

            if(!getTriedMemoryBlock(entity) && !tryNext.hasNext()) {
                List<BlockPos> loc = memory.get(stack.getItem());
                if(loc != null) {
                    tryNext = loc.iterator();
                    entity.getBrain().remember(Golem.REMAINING_MEMORY_HITS, tryNext);
                    entity.getBrain().remember(Golem.TRIED_SHARED_MEMORY, true);
                } else {
                    return;
                }
            }

            while(tryNext.hasNext()) {
                BlockPos pos = tryNext.next();
                BlockEntity newBlockEntity = world.getBlockEntity(pos);
                if(newBlockEntity instanceof ChestBlockEntity) {
                    cir.setReturnValue(Optional.ofNullable(MoveItemsTask.Storage.forContainer(newBlockEntity, world)));
                    return;
                } else {
                    deadItemCache.put(stack.getItem(), pos);
                }
            }
            deadItemCache.forEach(memory::dead);
        }
    }

    @Unique
    private double dist(BlockPos pos1, BlockPos pos2) {
        var a = pos2.toCenterPos().subtract(pos1.toCenterPos());
        return Math.sqrt(a.getX() * a.getX() + a.getY() * a.getY() + a.getZ() * a.getZ());
    }

    @WrapOperation(method = "findStorage", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Iterator;next()Ljava/lang/Object;", ordinal = 1))
    Object findStorageNext(
            Iterator<?> instance,
            Operation<Object> original,
            ServerWorld world,
            PathAwareEntity entity
    ) {
        Object obj = original.call(instance);

        if(obj instanceof CopperMemoryBlockEntity e) {
            CopperMemoryBlockEntity mem = getMemory(world, entity);
            if(mem == null || dist(entity.getBlockPos(), mem.getPos()) > dist(entity.getBlockPos(), entity.getBlockPos())) {
                entity.getBrain().remember(Golem.SHARED_MEMORY, e.getPos());
            }
        }

        return obj;
    }

    @Inject(method = {"resetVisitedPositions", "cooldown"}, at = @At("TAIL"))
    void resetMemoryState(PathAwareEntity entity, CallbackInfo ci) {
        entity.getBrain().forget(Golem.SHARED_MEMORY);
        entity.getBrain().forget(Golem.TRIED_SHARED_MEMORY);
        entity.getBrain().forget(Golem.REMAINING_MEMORY_HITS);
    }
}
