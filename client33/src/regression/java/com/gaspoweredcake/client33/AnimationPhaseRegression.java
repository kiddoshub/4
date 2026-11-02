package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.render.AnimationPhase;

public final class AnimationPhaseRegression {
    public static void main(String[] args) { run(); }
    public static void run() {
        for (int frames : new int[] {30, 60, 144}) {
            var clock = new AnimationPhase(0);
            float phase = 0;
            for (int frame = 1; frame <= frames; frame++)
                phase = clock.sample(1_000_000_000L * frame / frames, 0.35);
            near(phase, 0.35f, "One second must advance equally at every frame rate");
        }
        var clock = new AnimationPhase(0);
        near(clock.sample(500_000_000L, 0.5), 0.25f, "Elapsed time advances the pattern");
        near(clock.sample(1_000_000_000L, 0), 0.25f, "Zero speed freezes the pattern");
        near(clock.sample(1_500_000_000L, 1), 0.75f, "Changing speed preserves accumulated phase");
        near(clock.sample(2_000_000_000L, 1), 0.25f, "Phase wraps continuously");
        near(clock.sample(3_000_000_000L, Double.NaN), 0.25f, "Invalid speeds cannot poison uniforms");
        var rollover = new AnimationPhase(Long.MAX_VALUE - 99);
        near(rollover.sample(Long.MIN_VALUE + 100, 1_000_000), 0.2f, "Monotonic clock rollover preserves elapsed time");
        System.out.println("Animation phase regressions passed.");
    }
    private static void near(float actual, float expected, String message) {
        if (Math.abs(actual - expected) > 0.00001f) throw new AssertionError(message + ": " + actual);
    }
}
