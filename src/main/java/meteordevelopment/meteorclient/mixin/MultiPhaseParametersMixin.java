
package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IMultiPhaseParameters;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderLayer.MultiPhaseParameters.class)
public abstract class MultiPhaseParametersMixin implements IMultiPhaseParameters {
    @Shadow
    @Final
    RenderPhase.Target target;

    @Override
    public RenderPhase.Target meteor$getTarget() {
        return this.target;
    }
}
