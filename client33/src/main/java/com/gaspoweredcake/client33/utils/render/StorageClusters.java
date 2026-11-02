package com.gaspoweredcake.client33.utils.render;

import java.util.*;

/** Spatial leads from received storage positions; a lead is not proof of a player base. */
public final class StorageClusters {
    private StorageClusters() { }
    public record Point(double x, double y, double z) {
        public double squaredDistance(double x, double y, double z) {
            return (this.x-x)*(this.x-x)+(this.y-y)*(this.y-y)+(this.z-z)*(this.z-z);
        }
    }
    private record Cell(int x, int y, int z) {
        static Cell of(Point p) { return new Cell((int)Math.floor(p.x/16), (int)Math.floor(p.y/16), (int)Math.floor(p.z/16)); }
    }
    public record Lead(int containers, Point anchor) { }

    public static List<Lead> group(Collection<Point> points, int minimum) {
        Map<Cell,List<Point>> cells=new HashMap<>();
        for(Point point:points) {
            if(!Double.isFinite(point.x)||!Double.isFinite(point.y)||!Double.isFinite(point.z))continue;
            cells.computeIfAbsent(Cell.of(point),ignored->new ArrayList<>()).add(point);
        }
        List<Lead> leads=new ArrayList<>();
        ArrayDeque<Cell> pending=new ArrayDeque<>();
        List<Point> group=new ArrayList<>();
        for(Cell start:new ArrayList<>(cells.keySet())) {
            List<Point> first=cells.remove(start);
            if(first==null)continue;
            group.clear();group.addAll(first);pending.add(start);
            while(!pending.isEmpty()) {
                Cell cell=pending.removeFirst();
                for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++) {
                    if(x==0&&y==0&&z==0)continue;
                    Cell next=new Cell(cell.x+x,cell.y+y,cell.z+z);
                    List<Point> neighbours=cells.remove(next);
                    if(neighbours!=null) {group.addAll(neighbours);pending.add(next);}
                }
            }
            if(group.size()<Math.max(1,minimum))continue;
            double x=0,y=0,z=0;
            for(Point point:group) {x+=point.x;y+=point.y;z+=point.z;}
            x/=group.size();y/=group.size();z/=group.size();
            Point anchor=group.getFirst();double best=anchor.squaredDistance(x,y,z);
            for(Point point:group) {
                double distance=point.squaredDistance(x,y,z);
                if(distance<best) {best=distance;anchor=point;}
            }
            leads.add(new Lead(group.size(),anchor));
        }
        leads.sort(Comparator.comparingInt(Lead::containers).reversed()
            .thenComparingDouble(l->l.anchor.x).thenComparingDouble(l->l.anchor.y).thenComparingDouble(l->l.anchor.z));
        return List.copyOf(leads);
    }

    /** Cardinal bearing from observer to lead, independent of camera roll or freecam. */
    public static String bearing(double dx,double dz) {
        double degrees=Math.toDegrees(Math.atan2(dx,-dz));
        int sector=Math.floorMod((int)Math.round(degrees/45),8);
        return new String[]{"N","NE","E","SE","S","SW","W","NW"}[sector];
    }
}
