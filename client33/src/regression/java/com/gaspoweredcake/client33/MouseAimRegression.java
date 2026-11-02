package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.combat.MouseAim;
import com.gaspoweredcake.client33.utils.combat.MouseTurnBudget;
import java.util.Random;

final class MouseAimRegression {
    static void run() {
        MouseAim aim = new MouseAim(new Random(33));
        double yaw = 179, pitch = 0;
        for (int i = 0; i < 40; i++) {
            var delta = aim.next(yaw, pitch, -160, 25, .5, false, true, false, 9);
            double dyaw = delta.turnX(true) * .15, dpitch = delta.turnY(false) * .15;
            check(Math.hypot(dyaw, dpitch) <= 9.000001, "combined turn rate must stay bounded");
            check(Math.abs(dyaw / (delta.gain() * .15) - Math.rint(dyaw / (delta.gain() * .15))) < 1e-8, "movement follows mouse-count steps");
            yaw += dyaw; pitch = Math.max(-90, Math.min(90, pitch + dpitch));
        }
        check(Math.abs(wrap(-160 - yaw)) < .2 && Math.abs(25 - pitch) < .2, "crossing yaw boundary converges");
        for (double sensitivity : new double[] {0, .5, 1}) {
            for (boolean scoped : new boolean[] {false, true}) {
                for (boolean inverted : new boolean[] {false, true}) {
                    MouseAim path = new MouseAim(new Random(99));
                    double x = -175, y = 80;
                    for (int tick = 0; tick < 100; tick++) {
                        double desiredYaw = tick < 12 ? 155 : 130;
                        var step = path.next(x, y, desiredYaw, -70, sensitivity, scoped, inverted, inverted, 9);
                        double dx = step.turnX(inverted) * .15, dy = step.turnY(inverted) * .15;
                        check(Math.hypot(dx, dy) <= 9.000001, "turn budget must apply at all sensitivities");
                        x += dx; y = Math.max(-90, Math.min(90, y + dy));
                    }
                    double unit = MouseAim.gain(sensitivity, scoped) * .15;
                    check(Math.abs(wrap(130 - x)) < 2 * unit + 1e-6 && Math.abs(-70 - y) < 2 * unit + 1e-6,
                        "retargeted paths converge for scope and inversion options");
                }
            }
        }
        var tiny = new MouseAim(new Random(1)).next(0, 0, .001, .001, 1, false, false, false, 9);
        check(tiny.rawX() == 0 && tiny.rawY() == 0, "sub-count movement is skipped");
        check(Math.abs(MouseAim.gain(.5, false) / MouseAim.gain(.5, true) - 8) < 1e-9, "scope sensitivity is accounted for");
        aim.reset();
        var invalid = aim.next(0, 0, Double.NaN, 0, .5, false, false, false, 9);
        check(invalid.rawX() == 0 && invalid.rawY() == 0, "invalid targets produce no input");
        MouseAim one = new MouseAim(new Random(2)), two = new MouseAim(new Random(3));
        var a = one.next(0, 0, 90, 30, .5, false, false, false, 9);
        var b = two.next(0, 0, 90, 30, .5, false, false, false, 9);
        check(a.rawX() != b.rawX() || a.rawY() != b.rawY(), "different seeds create different curves");
        MouseTurnBudget budget = new MouseTurnBudget();
        check(budget.advance(1, 180) == 0, "first turn must establish a clock");
        check(Math.abs(budget.advance(1_000_001, 180) - .18) < 1e-9, "fast ticks earn only elapsed-time budget");
        budget.consume(.1);
        check(Math.abs(budget.advance(2_000_001, 180) - .26) < 1e-9, "unused fractional budget carries forward");
        check(budget.advance(1_000_000_001, 180) == 9, "long pauses cannot accumulate unbounded turns");
        budget.reset();
        check(budget.advance(1_100_000_001, 180) == 0, "reset discards old budget");
        System.out.println("Mouse aim regressions passed.");
    }
    private static double wrap(double value) { return ((value + 180) % 360 + 360) % 360 - 180; }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
