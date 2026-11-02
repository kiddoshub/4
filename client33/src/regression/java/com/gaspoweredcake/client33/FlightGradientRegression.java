package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.movement.FlightGradient;

public final class FlightGradientRegression {
    public static void run() {
        double speed = 0;
        for (int i = 0; i < 4; i++) speed = FlightGradient.step(speed, 1, 0.25);
        check(speed == 1, "Speed reaches target in bounded steps");
        check(FlightGradient.step(0.9, 1, 0.25) == 1, "Acceleration does not overshoot");
        check(FlightGradient.step(1, -1, 0.25) == 0.75, "Direction changes decelerate first");
        check(FlightGradient.step(0, -0.03, 0.1) == -0.03, "Slow descent remains bounded");
        check(FlightGradient.step(Double.NaN, 1, 0.25) == 0.25, "Invalid prior velocity resets");
        check(FlightGradient.step(1, Double.NaN, 0.25) == 0, "Invalid target does not propagate");
        var motion = new FlightGradient.Velocity(0, 0, 0);
        for (int i = 0; i < 20; i++) {
            motion = FlightGradient.next(motion, new FlightGradient.Velocity(2, 2, -2), 0.08, 1.0, 0.5);
            check(Math.hypot(motion.x(), motion.z()) <= 1.000000001, "Horizontal speed stays within its cap");
            check(Math.abs(motion.y()) <= 0.500000001, "Vertical speed stays within its cap");
        }
        check(Math.abs(Math.hypot(motion.x(), motion.z()) - 1.0) < 0.000001 && motion.y() == 0.5, "Independent caps can both be reached");
        var reversed = FlightGradient.next(motion, new FlightGradient.Velocity(-1, -1, 1), 0.08, 0.1, 0.2);
        check(Math.hypot(reversed.x(), reversed.z()) <= 0.100000001 && Math.abs(reversed.y()) <= 0.200000001, "Lower caps apply immediately during a turn");
        var fast = FlightGradient.next(new FlightGradient.Velocity(0, 0, 0), new FlightGradient.Velocity(8, 0, 0), 10, 10, 10);
        check(fast.x() == 8, "Raised caps permit speeds above the previous limit");
        var max = FlightGradient.next(new FlightGradient.Velocity(0, 0, 0), new FlightGradient.Velocity(10, 10, 10), 10, 10, 10);
        check(Math.hypot(Math.hypot(max.x(), max.z()), max.y()) <= 10.000000001, "Combined speed respects the new limit");
        check(FlightGradient.next(max, new FlightGradient.Velocity(10, -10, 10), 10, 10, 10).y() < 0, "The total cap handles descent as well as ascent");
        var invalid = FlightGradient.next(motion, new FlightGradient.Velocity(1, 1, 1), 0.08, Double.NaN, 0.5);
        check(invalid.x() == 0 && invalid.y() == 0 && invalid.z() == 0, "Invalid cap stops motion");
        System.out.println("Flight gradient regressions passed.");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
