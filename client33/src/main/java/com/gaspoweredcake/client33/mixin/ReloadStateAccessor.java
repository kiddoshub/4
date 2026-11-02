
package com.gaspoweredcake.client33.mixin;

import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResourceLoadStateTracker.ReloadState.class)
public interface ReloadStateAccessor {
    @Accessor("finished")
    boolean client33$isFinished();
}
