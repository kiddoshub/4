
package com.gaspoweredcake.client33.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
    @Accessor("activeLayerCount")
    int client33$getActiveLayerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] client33$getLayers();
}
