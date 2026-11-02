package com.gaspoweredcake.client33.utils.render;

import java.util.*;

/** Received sounds are activity evidence, never confirmed container or actor identity. */
public final class StorageSignals {
    public record Signal(StorageClusters.Point point,String label,long heardAt) { }
    private final Map<StorageClusters.Point,Signal> latest=new HashMap<>();
    public static String describeSound(String id) {
        return switch(id) {
            case "minecraft:block.chest.open", "minecraft:block.chest.close" -> "Chest sound";
            case "minecraft:block.ender_chest.open", "minecraft:block.ender_chest.close" -> "Ender chest sound";
            case "minecraft:block.barrel.open", "minecraft:block.barrel.close" -> "Barrel sound";
            case "minecraft:block.shulker_box.open", "minecraft:block.shulker_box.close" -> "Shulker sound";
            default -> null;
        };
    }
    public void hear(String id,double x,double y,double z,long now) {
        String label=describeSound(id);
        if(label==null||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))return;
        var point=new StorageClusters.Point(Math.floor(x)+0.5,Math.floor(y)+0.5,Math.floor(z)+0.5);
        latest.put(point,new Signal(point,label,now));
    }
    public List<Signal> recent(long now,long lifetime) {
        latest.values().removeIf(signal->now-signal.heardAt<0||now-signal.heardAt>lifetime);
        return latest.values().stream().sorted(Comparator.comparingLong(Signal::heardAt).reversed()
            .thenComparingDouble(s->s.point.x()).thenComparingDouble(s->s.point.z())).toList();
    }
    public void clear() { latest.clear(); }
}
