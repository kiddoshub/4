package com.gaspoweredcake.client33.utils.combat;

/** Shared finite-value and health boundaries used again immediately before crystal actions. */
public final class ExplosionRisk {
    private ExplosionRisk() {}
    public static boolean safeSelf(double damage, double maximum, double health, double reserve, boolean antiSuicide) {
        return Double.isFinite(damage) && damage >= 0 && damage <= maximum
            && (!antiSuicide || damage + reserve < health);
    }
    public static boolean advantage(double targetDamage, double selfDamage, double required) {
        return Double.isFinite(targetDamage) && Double.isFinite(selfDamage)
            && (required <= 0 || targetDamage - selfDamage >= required);
    }
    public static boolean safeFriend(double damage, double maximum, double health) {
        return Double.isFinite(damage) && damage >= 0 && damage <= maximum && damage < health;
    }
}
