
package com.gaspoweredcake.client33.mixin;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSignEditScreen.class)
public interface AbstractSignEditScreenAccessor {
    @Accessor("sign")
    SignBlockEntity client33$getSign();
}
