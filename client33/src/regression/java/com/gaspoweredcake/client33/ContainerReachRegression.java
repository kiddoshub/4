package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.player.ContainerReach;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class ContainerReachRegression {
    static void run() {
        AABB chest = new AABB(19, 0, 0, 20, 1, 1);
        check(ContainerReach.withinAttemptRange(chest, new Vec3(0, .5, .5), 20), "near face within extended range");
        check(!ContainerReach.withinAttemptRange(chest, new Vec3(-1, .5, .5), 20), "exclusive distance boundary");
        check(!ContainerReach.withinAttemptRange(chest, new Vec3(-2, .5, .5), 20), "out of range rejected");
        check(!ContainerReach.withinAttemptRange(chest, Vec3.ZERO, Double.NaN), "invalid limit rejected");
        System.out.println("Container reach regressions passed.");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
