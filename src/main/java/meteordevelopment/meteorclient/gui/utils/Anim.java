package meteordevelopment.meteorclient.gui.utils;

public class Anim {
    public static double easeOutCubic(double t) {
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    public static double easeInOutCubic(double t) {
        return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
    }

    public static double easeOutQuad(double t) {
        return 1.0 - (1.0 - t) * (1.0 - t);
    }

    public static double lerp(double from, double to, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return from + (to - from) * easeOutCubic(t);
    }
}
