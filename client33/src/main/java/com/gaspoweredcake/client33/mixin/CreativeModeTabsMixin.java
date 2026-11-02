
package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.render.BetterTooltips;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CreativeModeTabs.class)
public abstract class CreativeModeTabsMixin {
    @ModifyReturnValue(method = "tryRebuildTabContents", at = @At("RETURN"))
    private static boolean modifyReturn(boolean original) {
        return original || Modules.get().get(BetterTooltips.class).updateTooltips();
    }
}
