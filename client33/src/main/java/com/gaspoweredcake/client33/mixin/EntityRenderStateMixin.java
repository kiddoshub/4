
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.mixininterface.IEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements IEntityRenderState {
    @Unique
    private Entity entity;

    @Override
    @Nullable // "EntityCulling mod can prevent the code that sets the entity from running"
    public Entity client33$getEntity() {
        return entity;
    }

    @Override
    public void client33$setEntity(Entity entity) {
        this.entity = entity;
    }
}
