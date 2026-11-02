
package com.gaspoweredcake.client33.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {
    @Accessor("quads")
    net.minecraft.client.resources.model.geometry.ItemQuads client33$getQuads();

    @Accessor("itemTransform")
    ItemTransform client33$getTransform();
}
