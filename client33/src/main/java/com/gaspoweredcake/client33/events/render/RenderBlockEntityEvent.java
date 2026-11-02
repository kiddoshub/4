
package com.gaspoweredcake.client33.events.render;

import com.gaspoweredcake.client33.events.Cancellable;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class RenderBlockEntityEvent extends Cancellable {
    private static final RenderBlockEntityEvent INSTANCE = new RenderBlockEntityEvent();

    public BlockEntityRenderState blockEntityState;

    public static RenderBlockEntityEvent get(BlockEntityRenderState blockEntityState) {
        INSTANCE.setCancelled(false);
        INSTANCE.blockEntityState = blockEntityState;
        return INSTANCE;
    }
}
