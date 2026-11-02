
package com.gaspoweredcake.client33.mixin.sodium;

import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.render.Xray;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
public abstract class SodiumChunkBuilderMeshingTaskMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, name = "sortBehavior")
    private static SortBehavior modifySortBehavior(SortBehavior sortBehavior) {
        return Modules.get().isActive(Xray.class) && Modules.get().get(Xray.class).opacity.get() > 0 && Modules.get().get(Xray.class).opacity.get() < 255 ? SortBehavior.STATIC : sortBehavior;
    }
}
