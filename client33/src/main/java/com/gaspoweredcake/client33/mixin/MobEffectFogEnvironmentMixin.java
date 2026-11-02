
package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.render.NoRender;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobEffectFogEnvironment.class)
public abstract class MobEffectFogEnvironmentMixin {
    @Shadow
    public abstract Holder<MobEffect> getMobEffect();

    @ModifyReturnValue(method = "isApplicable", at = @At("RETURN"))
    private boolean modifyShouldApply(boolean original) {
        NoRender noRender = Modules.get().get(NoRender.class);
        if (getMobEffect() == MobEffects.BLINDNESS) return original && !noRender.noBlindness();
        if (getMobEffect() == MobEffects.DARKNESS) return original && !noRender.noDarkness();
        return original;
    }
}
