package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.modintegration.BobbyStorageBridge;
import de.johni0702.minecraft.bobby.ChunkSerializer;
import de.johni0702.minecraft.bobby.FakeChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

/** Capture detached metadata before Bobby drops the source NBT after asynchronous disk loading. */
@Mixin(value = ChunkSerializer.class, remap = false)
public abstract class BobbyChunkSerializerMixin {
    @Inject(method = "deserialize", at = @At("RETURN"))
    private static void client33$captureDiskStorage(ChunkPos pos, CompoundTag data, Level world,
            CallbackInfoReturnable<Pair<LevelChunk, Supplier<LevelChunk>>> callback) {
        if (callback.getReturnValue().getLeft() instanceof FakeChunk chunk)
            BobbyStorageBridge.captureDiskRecords(chunk, data.getListOrEmpty("block_entities"));
    }
}
