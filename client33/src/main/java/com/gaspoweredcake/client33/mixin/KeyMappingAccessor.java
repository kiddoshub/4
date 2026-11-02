
package com.gaspoweredcake.client33.mixin;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("ALL")
    static Map<String, KeyMapping> getKeysById() {
        return null;
    }

    @Accessor("key")
    InputConstants.Key client33$getKey();

    @Accessor("clickCount")
    int client33$getClickCount();

    @Accessor("clickCount")
    void client33$setClickCount(int timesPressed);

    @Invoker("release")
    void client33$invokeRelease();
}
