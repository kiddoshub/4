
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.mixininterface.IOptionInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(OptionInstance.class)
public abstract class OptionInstanceMixin<T> implements IOptionInstance {

    @Shadow
    private T value;

    @Shadow
    @Final
    private OptionInstance.ValueUpdateListener<? super T> onValueUpdate;

    @Override
    public void client33$set(Object value) {
        @SuppressWarnings("unchecked")
        T cast = (T) value;

        if (!Minecraft.getInstance().isRunning()) {
            this.value = cast;
        } else {
            if (!Objects.equals(this.value, cast)) {
                this.value = cast;
                this.onValueUpdate.valueChanged(cast);
            }
        }
    }
}
