package com.gaspoweredcake.client33.utils.render;

import java.util.ArrayDeque;
import java.util.List;

/** Bounded historical model poses; stationary samples and teleports never create streaks. */
public final class PoseTrail {
    public record Sample(SurfaceMesh mesh, long nanos, SurfaceMesh.Point position) { }
    private final ArrayDeque<Sample> samples = new ArrayDeque<>();

    public List<Sample> visible(long now, long lifetime, int limit, SurfaceMesh.Point current) {
        while (!samples.isEmpty() && now-samples.getFirst().nanos()>=lifetime) samples.removeFirst();
        while (samples.size()>limit) samples.removeFirst();
        if (!samples.isEmpty() && samples.getLast().position().distance(current)>16) samples.clear();
        return List.copyOf(samples);
    }
    public void capture(SurfaceMesh mesh,long now,SurfaceMesh.Point position,int limit) {
        Sample last=samples.peekLast();
        if (last!=null && (position.distance(last.position())<=0.035 || now-last.nanos()<80_000_000L)) return;
        samples.addLast(new Sample(mesh,now,position));
        while(samples.size()>limit) samples.removeFirst();
    }
}
