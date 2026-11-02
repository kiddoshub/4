package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.misc.ModuleLifecycle;
import java.util.ArrayList;
import java.util.List;

public final class LifecycleRegression {
    public static void run() {
        List<String> events = new ArrayList<>();
        var lifecycle = new ModuleLifecycle(() -> events.add("start"), () -> events.add("stop"),
            value -> events.add("enabled:" + value), failure -> events.add("error"));
        lifecycle.request(true, false);
        check(lifecycle.enabled() && !lifecycle.running(), "Menu enable preserves intent without world hooks");
        lifecycle.availability(true);
        lifecycle.availability(true);
        check(events.equals(List.of("enabled:true", "start")), "Repeated joins cannot duplicate startup");
        lifecycle.availability(false);
        lifecycle.availability(false);
        check(lifecycle.enabled() && !lifecycle.running(), "World leave suspends without forgetting intent");
        lifecycle.availability(true);
        lifecycle.request(false, true);
        check(events.equals(List.of("enabled:true", "start", "stop", "start", "enabled:false", "stop")), "Join/leave/disable each acquire and release exactly once");

        events.clear();
        ModuleLifecycle[] once = new ModuleLifecycle[1];
        once[0] = new ModuleLifecycle(() -> { events.add("start"); once[0].request(false, true); },
            () -> events.add("stop"), value -> events.add("enabled:" + value), failure -> events.add("error"));
        once[0].request(true, true);
        check(!once[0].enabled() && !once[0].running(), "One-shot modules can disable inside onActivate");
        check(events.equals(List.of("enabled:true", "start", "enabled:false", "stop")), "Reentrant disable performs one cleanup");

        events.clear();
        var broken = new ModuleLifecycle(() -> { throw new IllegalStateException("partial startup"); },
            () -> { events.add("cleanup"); throw new IllegalStateException("cleanup failed"); },
            value -> events.add("enabled:" + value), failure -> events.add("error"));
        broken.request(true, true);
        check(!broken.enabled() && !broken.running(), "Failed hooks cannot leave a ghost active module");
        check(events.equals(List.of("enabled:true", "enabled:false", "error", "cleanup", "error")), "Activation failure attempts cleanup and records both errors");

        events.clear();
        ModuleLifecycle[] cyclic = new ModuleLifecycle[1];
        cyclic[0] = new ModuleLifecycle(() -> cyclic[0].request(false, true), () -> cyclic[0].request(true, true),
            value -> {}, failure -> events.add("error"));
        cyclic[0].request(true, true);
        check(!cyclic[0].enabled() && !cyclic[0].running() && events.size() == 1, "Cyclic hooks stop instead of freezing the client");
        System.out.println("Module lifecycle regressions passed.");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
