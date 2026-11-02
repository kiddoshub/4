package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.movement.SprintRules;

final class SprintRulesRegression {
    static void run() {
        check(SprintRules.strictEligible(false, false, false, false, true, true), "normal forward sprint");
        check(!SprintRules.strictEligible(false, true, false, false, true, true), "mobility restriction applies on foot");
        check(!SprintRules.strictEligible(true, false, false, false, true, true), "shallow water applies on foot");
        check(!SprintRules.strictEligible(false, false, false, false, false, true), "food requirement applies on foot");
        check(!SprintRules.strictEligible(false, false, false, false, true, false), "collision blocks sprint on foot");
        check(SprintRules.strictEligible(false, false, true, true, false, false), "eligible vehicle uses vehicle rules");
        check(!SprintRules.strictEligible(false, false, true, false, true, true), "ineligible vehicle cannot use foot rules");
        check(!SprintRules.strictEligible(true, false, true, true, true, true), "shallow water also blocks vehicle sprint");
        System.out.println("Sprint eligibility regressions passed.");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
