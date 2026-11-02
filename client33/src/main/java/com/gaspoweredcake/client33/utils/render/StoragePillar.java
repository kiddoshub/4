package com.gaspoweredcake.client33.utils.render;

/** Pillars remain visible at every finite received distance. */
public final class StoragePillar {
    private StoragePillar() {}

    public static double opacity(double distance) {
        return Double.isFinite(distance) && distance >= 0 ? 1 : 0;
    }
}
