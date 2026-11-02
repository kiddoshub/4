
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.mixininterface.IAbstractFurnaceMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceMenuMixin implements IAbstractFurnaceMenu {
    @Shadow
    protected abstract boolean canSmelt(ItemStack itemStack);

    @Override
    public boolean client33$canSmelt(ItemStack itemStack) {
        return canSmelt(itemStack);
    }
}
