
package com.gaspoweredcake.client33.mixin.viafabricplus;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.viaversion.viafabricplus.settings.impl.GeneralSettingsImpl;
import com.viaversion.viafabricplus.api.settings.impl.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GeneralSettingsImpl.class)
public abstract class GeneralSettingsMixin {
    @ModifyExpressionValue(method = "<init>", at = @At(
        value = "FIELD",
        target = "Lcom/viaversion/viafabricplus/api/settings/impl/Orientation;RIGHT_TOP:Lcom/viaversion/viafabricplus/api/settings/impl/Orientation;",
        ordinal = 0
    ), remap = false, require = 0)
    private Orientation moveMultiplayerButton(Orientation original) {
        return Orientation.RIGHT_BOTTOM;
    }
}
