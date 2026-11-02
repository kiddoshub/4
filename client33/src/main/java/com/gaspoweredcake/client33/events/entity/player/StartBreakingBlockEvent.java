
package com.gaspoweredcake.client33.events.entity.player;

import com.gaspoweredcake.client33.events.Cancellable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class StartBreakingBlockEvent extends Cancellable {
    private static final StartBreakingBlockEvent INSTANCE = new StartBreakingBlockEvent();

    public BlockPos blockPos;
    public Direction direction;

    public static StartBreakingBlockEvent get(BlockPos blockPos, Direction direction) {
        INSTANCE.setCancelled(false);
        INSTANCE.blockPos = blockPos;
        INSTANCE.direction = direction;
        return INSTANCE;
    }
}
