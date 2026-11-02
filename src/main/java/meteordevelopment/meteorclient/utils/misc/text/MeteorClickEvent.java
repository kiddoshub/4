
package meteordevelopment.meteorclient.utils.misc.text;

import meteordevelopment.meteorclient.mixin.ScreenMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class MeteorClickEvent implements ClickEvent {
    public final String value;

    public MeteorClickEvent(String value) {
        this.value = value;
    }

    @Override
    public Action getAction() {
        return Action.RUN_COMMAND;
    }
}
