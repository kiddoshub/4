
package com.gaspoweredcake.client33.mixin;

import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MapTextureManager.class)
public interface MapTextureManagerAccessor {
    @Invoker("getOrCreateMapInstance")
    MapTextureManager.MapInstance client33$invokeGetOrCreateMapInstance(MapId id, MapItemSavedData state);
}
