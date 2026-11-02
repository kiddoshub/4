
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.systems.hud.Hud;
import com.gaspoweredcake.client33.systems.hud.HudElement;
import com.gaspoweredcake.client33.systems.hud.elements.TextHud;
import com.gaspoweredcake.client33.systems.modules.Category;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import net.minecraft.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CrashReport.class)
public abstract class CrashReportMixin {
    @Inject(method = "getDetails(Ljava/lang/StringBuilder;)V", at = @At("TAIL"))
    private void onAddDetails(StringBuilder builder, CallbackInfo ci) {
        builder.append("\n\n-- 33 --\n\n");
        builder.append("Version: ").append(Client33.VERSION).append("\n");
        if (!Client33.BUILD_NUMBER.isEmpty()) {
            builder.append("Build: ").append(Client33.BUILD_NUMBER).append("\n");
        }

        if (Modules.get() != null) {
            boolean modulesActive = false;
            for (Category category : Modules.loopCategories()) {
                List<Module> modules = Modules.get().getGroup(category);
                boolean categoryActive = false;

                for (Module module : modules) {
                    if (module == null || !module.isActive()) continue;

                    if (!modulesActive) {
                        modulesActive = true;
                        builder.append("\n[[ Active Modules ]]\n");
                    }

                    if (!categoryActive) {
                        categoryActive = true;
                        builder.append("\n[")
                            .append(category)
                            .append("]:\n");
                    }

                    builder.append(module.name).append("\n");
                }

            }

        }

        if (Hud.get() != null && Hud.get().active) {
            boolean hudActive = false;
            for (HudElement element : Hud.get()) {
                if (element == null || !element.isActive()) continue;

                if (!hudActive) {
                    hudActive = true;
                    builder.append("\n[[ Active Hud Elements ]]\n");
                }

                if (!(element instanceof TextHud textHud)) builder.append(element.info.name).append("\n");
                else {
                    builder.append("Text\n{")
                        .append(textHud.text.get())
                        .append("}\n");
                    if (textHud.shown.get() != TextHud.Shown.Always) {
                        builder.append("(")
                            .append(textHud.shown.get())
                            .append(textHud.condition.get())
                            .append(")\n");
                    }
                }
            }
        }
    }
}
