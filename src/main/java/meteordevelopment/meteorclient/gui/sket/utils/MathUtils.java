package meteordevelopment.meteorclient.gui.sket.utils;

public class MathUtils implements IUtil {
    public static double clamp(double min, double max, double val) {
        return Math.min(max, Math.max(min, val));
    }
}
