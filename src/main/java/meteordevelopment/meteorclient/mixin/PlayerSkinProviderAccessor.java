
package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.texture.PlayerSkinProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerSkinProvider.class)
public interface PlayerSkinProviderAccessor {
    @Accessor("skinCache")
    PlayerSkinProvider.FileCache meteor$getSkinCache();
}
