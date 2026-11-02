package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.render.StorageESP;
import de.johni0702.minecraft.bobby.FakeChunk;
import de.johni0702.minecraft.bobby.FakeChunkManager;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FakeChunkManager.class, remap = false)
public abstract class BobbyFakeChunkManagerMixin {
    @Inject(method = "load", at = @At("TAIL"))
    private void client33$indexLoadedCache(int x, int z, LevelChunk chunk, CallbackInfo ci) {
        if (chunk instanceof FakeChunk fake) Modules.get().get(StorageESP.class).indexCachedChunk(fake);
    }
}
