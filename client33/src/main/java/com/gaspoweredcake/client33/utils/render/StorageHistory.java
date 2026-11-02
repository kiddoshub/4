package com.gaspoweredcake.client33.utils.render;

import java.util.*;
import java.util.function.BiPredicate;

/** Detached session observations, retained after unload and invalidated by a fresh chunk scan. */
public final class StorageHistory {
    private record Chunk(int x,int z) { }
    public record Sighting(StorageClusters.Point point,long lastSeenMillis) { }
    private record Observed(List<StorageClusters.Point> points,long time) { }
    private final Map<Chunk,Observed> chunks=new HashMap<>();

    public void observe(int x,int z,Collection<StorageClusters.Point> points,long now) {
        Chunk key=new Chunk(x,z);
        if(points.isEmpty())chunks.remove(key);
        else chunks.put(key,new Observed(List.copyOf(points),now));
    }
    public List<Sighting> unloaded(BiPredicate<Integer,Integer> loaded,long now,long lifetimeMillis) {
        List<Sighting> result=new ArrayList<>();
        var iterator=chunks.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();var value=entry.getValue();
            long age=now-value.time;
            if(age<0||age>lifetimeMillis) {iterator.remove();continue;}
            if(loaded.test(entry.getKey().x,entry.getKey().z))continue;
            for(var point:value.points)result.add(new Sighting(point,value.time));
        }
        return List.copyOf(result);
    }
    public void clear() { chunks.clear(); }
}
