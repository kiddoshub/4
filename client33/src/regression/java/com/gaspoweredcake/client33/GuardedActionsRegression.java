package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.inventory.GuardedActions;
import java.util.ArrayList;
import java.util.List;

public final class GuardedActionsRegression {
    public static void run() {
        List<String> executed = new ArrayList<>();
        boolean[] active = {true}, sameMenu = {true}, cursorEmpty = {true};
        GuardedActions.run(() -> active[0] && sameMenu[0] && cursorEmpty[0],
            () -> executed.add("fuel"), () -> executed.add("result"), () -> executed.add("input"));
        check(executed.equals(List.of("fuel", "result", "input")), "Valid session runs all actions in order");
        for (int scenario = 0; scenario < 3; scenario++) {
            executed.clear(); active[0] = sameMenu[0] = cursorEmpty[0] = true;
            int change = scenario;
            GuardedActions.run(() -> active[0] && sameMenu[0] && cursorEmpty[0],
                () -> { executed.add("fuel"); if (change == 0) active[0] = false; else if (change == 1) sameMenu[0] = false; else cursorEmpty[0] = false; },
                () -> executed.add("result"), () -> executed.add("input"));
            check(executed.equals(List.of("fuel")), "Invalid session stops subsequent actions");
        }
        executed.clear();
        GuardedActions.run(() -> false, () -> executed.add("unexpected"));
        check(executed.isEmpty(), "Invalid session never starts");
        System.out.println("Guarded menu action regressions passed.");
    }

    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
