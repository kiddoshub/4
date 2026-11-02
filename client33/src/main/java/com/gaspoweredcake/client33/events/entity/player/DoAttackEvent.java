
package com.gaspoweredcake.client33.events.entity.player;

import com.gaspoweredcake.client33.events.Cancellable;

public class DoAttackEvent extends Cancellable {
    private static final DoAttackEvent INSTANCE = new DoAttackEvent();

    public static DoAttackEvent get() {
        return INSTANCE;
    }
}
