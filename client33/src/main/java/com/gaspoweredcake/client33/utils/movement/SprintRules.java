package com.gaspoweredcake.client33.utils.movement;

/** Eligibility for Strict sprint after movement and GUI checks. */
public final class SprintRules {
    private SprintRules() {}

    public static boolean strictEligible(boolean shallowWater, boolean restricted, boolean passenger,
                                         boolean vehicleEligible, boolean enoughFood, boolean collisionClear) {
        if (shallowWater || restricted) return false;
        return passenger ? vehicleEligible : enoughFood && collisionClear;
    }
}
