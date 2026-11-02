package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.render.StorageSignals;

public final class StorageSignalsRegression {
    public static void main(String[] args) { run(); }
    public static void run() {
        var signals=new StorageSignals();
        signals.hear("minecraft:block.chest.open",1_000_000,64,1_000_000,100);
        signals.hear("minecraft:block.chest.close",1_000_000,64,1_000_000,200);
        check(signals.recent(250,1000).size()==1,"Open and close at the same position deduplicate without a distance limit");
        check(signals.recent(250,1000).getFirst().heardAt()==200,"Newest activity time replaces the old time");
        signals.hear("custom:block.chest.open",0,0,0,300);
        signals.hear("minecraft:entity.pig.ambient",0,0,0,300);
        signals.hear("minecraft:block.barrel.open",Double.NaN,0,0,300);
        check(signals.recent(350,1000).size()==1,"Unrecognized namespaces, unrelated sounds and invalid positions are excluded");
        check(signals.recent(1300,1000).isEmpty(),"Sounds expire and never become confirmed storage");
        signals.hear("minecraft:block.shulker_box.open",0,0,0,1500);signals.clear();
        check(signals.recent(1600,1000).isEmpty(),"World reset removes audio history");
        System.out.println("Storage sound evidence regressions passed.");
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
