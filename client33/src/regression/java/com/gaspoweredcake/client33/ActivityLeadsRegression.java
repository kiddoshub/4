package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.intel.ActivityLeads;

public final class ActivityLeadsRegression {
    public static void run() {
        ActivityLeads<String> scan = new ActivityLeads<>();
        scan.add("a", ActivityLeads.Kind.BOAT);
        scan.add("a", ActivityLeads.Kind.BOAT);
        scan.add("a", ActivityLeads.Kind.ITEM_FRAME);
        scan.addLiving("a");
        scan.addLiving("a");
        scan.addLiving("b");
        var result = scan.finish(2);
        check(result.get("a").size() == 3 && result.get("a").contains(ActivityLeads.Kind.MOB_CLUSTER), "deduplicate reasons and include threshold");
        check(!result.containsKey("b"), "do not report below threshold");
        check(!scan.finish(0).get("a").contains(ActivityLeads.Kind.MOB_CLUSTER), "zero disables clusters");
        result.get("a").clear();
        check(scan.finish(2).get("a").size() == 3, "results must not mutate the scan");
        System.out.println("Activity lead regressions passed.");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
