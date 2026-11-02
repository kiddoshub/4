
package com.gaspoweredcake.client33.events.entity.player;

import com.gaspoweredcake.client33.events.Cancellable;
import net.minecraft.core.BlockPos;

public class BreakBlockEvent extends Cancellable {
    private static final BreakBlockEvent INSTANCE = new BreakBlockEvent();

    public BlockPos blockPos;

    public static BreakBlockEvent get(BlockPos blockPos) {
        INSTANCE.setCancelled(false);
        INSTANCE.blockPos = blockPos;
        return INSTANCE;
    }
}
