package com.gaspoweredcake.client33.utils.intel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** A block entity entry advertised by a loaded chunk or an update packet. */
public record BlockEntityPresence(BlockPos pos, BlockEntityType<?> type, long observedMillis, boolean hasNbt) {
    public BlockEntityPresence { pos = pos.immutable(); }
}
