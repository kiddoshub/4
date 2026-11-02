package com.gaspoweredcake.client33.utils.combat;

public final class ShieldEstimate {
    private ShieldEstimate() {}
    public static boolean ready(double clientUseTicks, int blockDelayTicks, double pingMillis, double maxLeadMillis, double serverTickMillis) {
        if (!Double.isFinite(clientUseTicks) || !Double.isFinite(serverTickMillis) || serverTickMillis < 50) return false;
        double lead = Math.max(0, Math.min(maxLeadMillis, pingMillis / 2));
        return (Math.max(0, clientUseTicks) * 50 + lead) / serverTickMillis >= Math.max(0, blockDelayTicks);
    }
}
