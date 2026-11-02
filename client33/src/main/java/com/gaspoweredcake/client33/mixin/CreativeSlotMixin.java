
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.mixininterface.ISlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
public abstract class CreativeSlotMixin implements ISlot {
    @Shadow
    @Final
    private Slot target;

    @Override
    public int client33$getIndex() {
        return target.index;
    }

    @Override
    public int client33$getSlot() {
        return target.getContainerSlot();
    }
}
