
package com.gaspoweredcake.client33.events.entity.player;

/**
 * @see net.minecraft.client.entity.ClientAvatarState#updateBob(float)
 */
public class PlayerTickMovementEvent {
    private static final PlayerTickMovementEvent INSTANCE = new PlayerTickMovementEvent();

    public static PlayerTickMovementEvent get() {
        return INSTANCE;
    }
}
