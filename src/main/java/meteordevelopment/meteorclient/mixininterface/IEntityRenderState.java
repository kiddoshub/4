
package meteordevelopment.meteorclient.mixininterface;

import meteordevelopment.meteorclient.mixin.EntityRenderManagerMixin;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface IEntityRenderState {
    @Nullable(value = "EntityCulling mod can prevent the code that sets the entity from running")
    Entity meteor$getEntity();

    void meteor$setEntity(Entity entity);
}
