package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.render.StorageHistory;
import com.gaspoweredcake.client33.utils.render.StorageClusters.Point;
import java.util.List;

public final class StorageHistoryRegression {
    public static void main(String[] args) { run(); }
    public static void run() {
        var history=new StorageHistory();
        var far=new Point(1_000_000.5,64.5,1_000_000.5);
        history.observe(62500,62500,List.of(far),100);
        check(history.unloaded((x,z)->true,200,1000).isEmpty(),"Loaded chunks never appear as stale history");
        var seen=history.unloaded((x,z)->false,200,1000);
        check(seen.size()==1&&seen.getFirst().point().equals(far)&&seen.getFirst().lastSeenMillis()==100,"Unloaded positions retain source time without a distance cutoff");
        history.observe(62500,62500,List.of(),300);
        check(history.unloaded((x,z)->false,400,1000).isEmpty(),"Fresh empty chunk scan invalidates old storage");
        history.observe(62500,62500,List.of(far),500);
        check(history.unloaded((x,z)->false,1600,1000).isEmpty(),"Old observations expire");
        history.observe(62500,62500,List.of(far),2000);history.clear();
        check(history.unloaded((x,z)->false,2100,1000).isEmpty(),"World/session reset removes old coordinates");
        System.out.println("Storage observation history regressions passed.");
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
