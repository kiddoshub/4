package com.gaspoweredcake.client33.utils.inventory;

import java.util.function.BooleanSupplier;

/** Runs ordered menu operations only while their originating session is still valid. */
public final class GuardedActions {
    private GuardedActions() {}

    public static void run(BooleanSupplier valid, Runnable... actions) {
        for (Runnable action : actions) {
            if (!valid.getAsBoolean()) return;
            action.run();
        }
    }
}
