package com.gaspoweredcake.client33.events.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** A server-provided block entity payload; the NBT returned is detached. */
public final class BlockEntityIntelEvent {
    public final BlockPos pos;
    public final BlockEntityType<?> type;
    public final long observedMillis;
    private final CompoundTag nbt;
    public BlockEntityIntelEvent(BlockPos pos, BlockEntityType<?> type, long observedMillis, CompoundTag nbt) {
        this.pos = pos.immutable(); this.type = type; this.observedMillis = observedMillis; this.nbt = nbt.copy();
    }
    public CompoundTag nbt() { return nbt.copy(); }
}
