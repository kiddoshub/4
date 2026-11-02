
package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.world.PlaySoundEvent;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.misc.SoundBlocker;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow
    public abstract void stop(SoundInstance soundInstance);

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void onPlay(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        PlaySoundEvent event = Client33.EVENT_BUS.post(PlaySoundEvent.get(instance));

        if (event.isCancelled()) cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
    }

    @Inject(method = "tickInGameSound()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/TickableSoundInstance;tick()V", ordinal = 0))
    private void onTick(CallbackInfo ci, @Local(name = "instance") TickableSoundInstance instance) {
        if (Modules.get().get(SoundBlocker.class).shouldBlock(instance)) stop(instance);
    }
}
