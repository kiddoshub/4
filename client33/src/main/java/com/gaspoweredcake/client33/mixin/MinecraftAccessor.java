
package com.gaspoweredcake.client33.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("fps")
    static int client33$getFps() {
        return 0;
    }

    @Accessor("reloadStateTracker")
    ResourceLoadStateTracker client33$getReloadStateTracker();

    @Accessor("missTime")
    int client33$getMissTime();

    @Accessor("missTime")
    void client33$setMissTime(int attackCooldown);

    @Invoker("startAttack")
    boolean client33$leftClick();

    @Invoker("handleKeybinds")
    void client33$handleInputEvents();
}
