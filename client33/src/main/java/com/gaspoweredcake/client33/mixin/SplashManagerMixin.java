
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.systems.config.Config;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(SplashManager.class)
public abstract class SplashManagerMixin {
    @Unique
    private boolean override = true;
    @Unique
    private static final Random random = new Random();
    @Unique
    private final List<String> client33Splashes = getClient33Splashes();

    @Inject(method = "getSplash", at = @At("HEAD"), cancellable = true)
    private void onApply(CallbackInfoReturnable<SplashRenderer> cir) {
        if (Config.get() == null || !Config.get().titleScreenSplashes.get()) return;

        if (override)
            cir.setReturnValue(new SplashRenderer(Component.literal(client33Splashes.get(random.nextInt(client33Splashes.size())))));
        override = !override;
    }

    @Unique
    private static List<String> getClient33Splashes() {
        return List.of(
            "33 on Crack!",
            "Star 33 on GitHub!",
            "Based utility mod.",
            "§6MineGame159 §fbased god",
            "§a33",
            "§433 on Crack!",
            "§633 on Crack!"
        );
    }

}
