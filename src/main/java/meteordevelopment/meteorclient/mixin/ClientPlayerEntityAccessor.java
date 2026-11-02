
package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor {
    @Accessor("ticksSinceLastPositionPacketSent")
    void meteor$setTicksSinceLastPositionPacketSent(int ticks);

    @Invoker("canSprint")
    boolean meteor$invokeCanSprint();
}
