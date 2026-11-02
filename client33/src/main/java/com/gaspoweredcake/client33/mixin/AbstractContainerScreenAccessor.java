
package com.gaspoweredcake.client33.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.world.inventory.ContainerInput;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Invoker("slotClicked")
    void client33$clickSlot(Slot slot, int slotId, int button, ContainerInput input);

    @Accessor("hoveredSlot")
    Slot client33$getHoveredSlot();
}
