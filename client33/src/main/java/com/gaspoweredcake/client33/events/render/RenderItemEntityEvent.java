
package com.gaspoweredcake.client33.events.render;

import com.gaspoweredcake.client33.events.Cancellable;
import com.gaspoweredcake.client33.mixininterface.IEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.item.ItemEntity;
import org.jspecify.annotations.Nullable;

public class RenderItemEntityEvent extends Cancellable {
    private static final RenderItemEntityEvent INSTANCE = new RenderItemEntityEvent();

    @Nullable
    public ItemEntity itemEntity;
    public ItemEntityRenderState renderState;
    public float tickDelta;
    public PoseStack matrixStack;
    public int light;
    public ItemModelResolver itemModelManager;
    public SubmitNodeCollector renderCommandQueue;

    public static RenderItemEntityEvent get(ItemEntityRenderState renderState, float tickDelta, PoseStack matrixStack, int light, ItemModelResolver itemModelManager, SubmitNodeCollector renderCommandQueue) {
        INSTANCE.setCancelled(false);
        INSTANCE.itemEntity = (ItemEntity) ((IEntityRenderState) renderState).client33$getEntity();
        INSTANCE.renderState = renderState;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.matrixStack = matrixStack;
        INSTANCE.light = light;
        INSTANCE.itemModelManager = itemModelManager;
        INSTANCE.renderCommandQueue = renderCommandQueue;
        return INSTANCE;
    }
}
