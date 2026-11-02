
package com.gaspoweredcake.client33.utils.world;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.game.GameJoinedEvent;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.combat.CombatTelemetry;
import meteordevelopment.orbit.EventHandler;

/** Compatibility facade over the shared monotonic server-tick model. */
public final class TickRate {
    public static final TickRate INSTANCE = new TickRate();
    private volatile long joinedAt = CombatTelemetry.now();

    private TickRate() { Client33.EVENT_BUS.subscribe(this); }

    @EventHandler private void onJoin(GameJoinedEvent event) { joinedAt = CombatTelemetry.now(); }

    public float getTickRate() {
        return Utils.canUpdate() ? (float) (1000 / CombatTelemetry.CLOCK.tickMillis()) : 0;
    }

    public float getTimeSinceLastTick() {
        if (!Utils.canUpdate()) return 0;
        long now = CombatTelemetry.now();
        long connected = Math.max(0, now - joinedAt);
        if (connected < 4000) return 0;
        return Math.min(connected, CombatTelemetry.CLOCK.ageMillis(now)) / 1000f;
    }
}
