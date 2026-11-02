package com.gaspoweredcake.client33.utils.player;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounds a client-side interaction attempt; the server decides whether to accept it. */
public final class ContainerReach {
    private ContainerReach() {}
    public static boolean withinAttemptRange(AABB bounds, Vec3 eye, double range) {
        return Double.isFinite(range) && range > 0 && bounds.distanceToSqr(eye) < range * range;
    }
}
