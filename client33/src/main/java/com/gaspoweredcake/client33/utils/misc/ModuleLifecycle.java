package com.gaspoweredcake.client33.utils.misc;

import java.util.function.Consumer;

/** Main-thread lifecycle with explicit enabled intent, runtime availability, and reentrant hooks. */
public final class ModuleLifecycle {
    private final Runnable start, stop;
    private final Consumer<Boolean> changed;
    private final Consumer<RuntimeException> failure;
    private volatile boolean enabled;
    private boolean available, running, transitioning;

    public ModuleLifecycle(Runnable start, Runnable stop, Consumer<Boolean> changed, Consumer<RuntimeException> failure) {
        this.start = start;
        this.stop = stop;
        this.changed = changed;
        this.failure = failure;
    }

    public boolean enabled() { return enabled; }
    public boolean running() { return running; }

    public void request(boolean enabled, boolean available) {
        this.available = available;
        setIntent(enabled);
        reconcile();
    }

    public void availability(boolean available) {
        this.available = available;
        reconcile();
    }

    private void setIntent(boolean value) {
        if (enabled == value) return;
        enabled = value;
        changed.accept(value);
    }

    private void reconcile() {
        if (transitioning) return;
        transitioning = true;
        try {
            int transitions = 0;
            while (running != (enabled && available)) {
                if (++transitions > 16) {
                    // A hook that endlessly enables/disables itself must not freeze the client.
                    setIntent(false);
                    if (running) {
                        running = false;
                        try { stop.run(); } catch (RuntimeException ex) { failure.accept(ex); }
                    }
                    setIntent(false);
                    failure.accept(new IllegalStateException("Module lifecycle hooks did not settle"));
                    return;
                }
                running = enabled && available;
                try {
                    if (running) start.run();
                    else stop.run();
                } catch (RuntimeException ex) {
                    setIntent(false);
                    failure.accept(ex);
                    // A failed start gets one cleanup pass, even after partially acquiring resources.
                }
            }
        } finally {
            transitioning = false;
        }
    }
}
