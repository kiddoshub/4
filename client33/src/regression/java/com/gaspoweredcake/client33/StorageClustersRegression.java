package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.render.StorageClusters;
import com.gaspoweredcake.client33.utils.render.StorageClusters.Point;
import java.util.*;

public final class StorageClustersRegression {
    public static void main(String[] args) { run(); }
    public static void run() {
        var points=new ArrayList<Point>();
        for(int i=0;i<12_000;i++)points.add(new Point(i%32,64+i/1024,(i/32)%32));
        points.add(new Point(1_000_000,64,1_000_000));points.add(new Point(1_000_001,64,1_000_000));
        points.add(new Point(0,-200,0));points.add(new Point(1,-200,0));
        var leads=StorageClusters.group(points,2);
        check(leads.size()==3,"Far and vertically separated storage remain distinct");
        check(leads.getFirst().containers()==12_000,"Dense storage retains every entry beyond the former cache cap");
        check(leads.stream().mapToInt(StorageClusters.Lead::containers).sum()==12_004,"Grouping drops no eligible containers");
        for(var lead:leads)check(points.contains(lead.anchor()),"Lead coordinates refer to observed storage, not empty centroid space");
        check(StorageClusters.group(List.of(new Point(-1,0,-1),new Point(0,0,0)),2).size()==1,"Negative boundary and diagonal neighbours join");
        check(StorageClusters.group(List.of(new Point(Double.NaN,0,0)),1).isEmpty(),"Invalid positions excluded");
        check(StorageClusters.group(points,3).size()==1,"Minimum filters weak leads without a distance cutoff");
        check(StorageClusters.bearing(0,-100).equals("N")&&StorageClusters.bearing(100,0).equals("E")
            &&StorageClusters.bearing(0,100).equals("S")&&StorageClusters.bearing(-100,-100).equals("NW"),"Cardinal bearings match world axes");
        System.out.println("Storage cluster coverage and bearing regressions passed.");
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
