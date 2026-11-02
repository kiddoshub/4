
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.renderer.MeshUniforms;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.misc.InventoryTweaks;
import com.gaspoweredcake.client33.utils.render.postprocess.ChamsShader;
import com.gaspoweredcake.client33.utils.render.postprocess.OutlineUniforms;
import com.gaspoweredcake.client33.utils.render.postprocess.PostProcessShader;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.gaspoweredcake.client33.Client33.mc;

@Mixin(Minecraft.class)
public abstract class MinecraftRenderFrameMixin {
    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;endFrame()V", shift = At.Shift.AFTER))
    private void client33$afterRenderFrame(boolean advanceGameTime, CallbackInfo ci) {
        MeshUniforms.flipFrame();
        PostProcessShader.flipFrame();
        ChamsShader.flipFrame();
        OutlineUniforms.flipFrame();

        Modules modules = Modules.get();
        if (modules == null || mc.player == null) return;

        InventoryTweaks inventoryTweaks = modules.get(InventoryTweaks.class);
        if (inventoryTweaks != null && inventoryTweaks.frameInput()) {
            ((MinecraftAccessor) mc).client33$handleInputEvents();
        }
    }
}
