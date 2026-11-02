
package com.gaspoweredcake.client33.mixin;

import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResourceLoadStateTracker.class)
public interface ResourceLoadStateTrackerAccessor {
    @Accessor("reloadState")
    ResourceLoadStateTracker.ReloadState client33$getReloadState();
}
