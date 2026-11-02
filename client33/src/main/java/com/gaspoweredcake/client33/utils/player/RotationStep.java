package com.gaspoweredcake.client33.utils.player;

import net.minecraft.util.Mth;

/** One bounded yaw/pitch step toward a target. */
public record RotationStep(double yaw, double pitch, boolean ready) {
    public static RotationStep toward(double fromYaw, double fromPitch, double yaw, double pitch, double limit) {
        double dyaw = Mth.wrapDegrees(yaw - fromYaw), dpitch = pitch - fromPitch;
        boolean ready = Math.abs(dyaw) <= limit && Math.abs(dpitch) <= limit;
        return new RotationStep(fromYaw + Mth.clamp(dyaw, -limit, limit),
            Mth.clamp(fromPitch + Mth.clamp(dpitch, -limit, limit), -90, 90), ready);
    }
}
