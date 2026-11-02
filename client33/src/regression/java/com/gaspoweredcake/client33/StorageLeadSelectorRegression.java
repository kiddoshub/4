package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.render.StorageClusters.Point;
import com.gaspoweredcake.client33.utils.render.StorageLeadSelector;
import com.gaspoweredcake.client33.utils.render.StorageLeadSelector.Candidate;
import com.gaspoweredcake.client33.utils.render.StorageLeadSelector.Order;
import com.gaspoweredcake.client33.utils.render.StorageLeadSelector.Source;
import java.util.*;

public final class StorageLeadSelectorRegression {
    public static void run() {
        var current = new Candidate(Source.CURRENT, new Point(5, 64, 0), 5, 0, "");
        var remembered = new Candidate(Source.REMEMBERED, new Point(-10, 64, 0), 6, 100, "");
        var distant = new Candidate(Source.CACHED, new Point(4800, 64, 0), 30, 0, "");
        var nearby = new Candidate(Source.CACHED, new Point(25, 64, 0), 3, 0, "");
        var heard = new Candidate(Source.HEARD, new Point(40, 64, 0), 0, 200, "Chest");
        var all = List.of(distant, current, nearby, remembered, heard);
        var fair = StorageLeadSelector.select(all, 4, Order.Largest, 0, 64, 0);
        check(fair.equals(List.of(current, remembered, distant, heard)), "Four sources each receive a row and largest cache ranks first");
        check(StorageLeadSelector.select(all, 4, Order.Nearest, 0, 64, 0).get(2).equals(nearby), "Nearby cache can be prioritized");
        check(StorageLeadSelector.select(List.of(distant, nearby), 2, Order.Largest, 0, 64, 0).equals(List.of(distant, nearby)), "Multiple cached leads remain accessible");
        check(StorageLeadSelector.select(all, 0, Order.Largest, 0, 64, 0).isEmpty(), "A zero display budget returns no rows");
        var many = new ArrayList<Candidate>();
        for (int i = 100_000; i >= 0; i--) many.add(new Candidate(Source.CACHED, new Point(i * 16, 64, -4800), i, 0, ""));
        var nearest = StorageLeadSelector.select(many, 12, Order.Nearest, 0, 64, -4800);
        check(nearest.size() == 12 && nearest.getFirst().point().x() == 0 && nearest.getLast().point().x() == 176, "Large input selects the closest rows without a discovery cutoff");
        check(many.size() == 100_001, "Selecting panel rows does not remove storage records");
        System.out.println("Storage lead selection regressions passed.");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
