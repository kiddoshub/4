
package com.gaspoweredcake.client33.utils.network;

import com.gaspoweredcake.client33.utils.PreInit;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** Capes are disabled until 33 has its own cape provider. */
public final class Capes {
    private Capes() {
    }

    @PreInit
    public static void init() {
    }

    @Nullable
    public static Identifier get(Player player) {
        return null;
    }
}
