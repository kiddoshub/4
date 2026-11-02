package com.gaspoweredcake.client33.utils.render;

/** Reusable projected bounds. Rejects near-plane crossings and non-finite geometry. */
public final class ScreenBounds {
    public double left, top, right, bottom;
    private boolean valid;

    public void reset() {
        left = top = Double.POSITIVE_INFINITY;
        right = bottom = Double.NEGATIVE_INFINITY;
        valid = true;
    }

    public void include(double x, double y, double depth) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(depth) || depth < 0 || depth > 1) {
            valid = false;
            return;
        }
        left = Math.min(left, x);
        right = Math.max(right, x);
        top = Math.min(top, y);
        bottom = Math.max(bottom, y);
    }

    public boolean clip(double width, double height) {
        if (!valid || !Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0) return false;
        left = Math.max(0, left);
        top = Math.max(0, top);
        right = Math.min(width, right);
        bottom = Math.min(height, bottom);
        return right - left > 1 && bottom - top > 1;
    }
}
