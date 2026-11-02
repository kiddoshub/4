
package com.gaspoweredcake.client33.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("destroyProgress")
    float client33$getBreakingProgress();

    @Accessor("destroyProgress")
    void client33$setDestroyProgress(float progress);

    @Accessor("destroyBlockPos")
    BlockPos client33$getCurrentBreakingBlockPos();
}
