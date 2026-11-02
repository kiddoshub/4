
package com.gaspoweredcake.client33.events.entity.player;

import com.gaspoweredcake.client33.events.Cancellable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;

public class InteractEntityEvent extends Cancellable {
    private static final InteractEntityEvent INSTANCE = new InteractEntityEvent();

    public Entity entity;
    public InteractionHand hand;

    public static InteractEntityEvent get(Entity entity, InteractionHand hand) {
        INSTANCE.setCancelled(false);
        INSTANCE.entity = entity;
        INSTANCE.hand = hand;
        return INSTANCE;
    }
}
