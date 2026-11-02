
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.mixininterface.IComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Component.class)
public interface ComponentMixin extends IComponent {
    @Override
    default void client33$invalidateCache() {
    }
}
