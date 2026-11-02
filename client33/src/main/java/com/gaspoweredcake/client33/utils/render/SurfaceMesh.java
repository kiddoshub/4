package com.gaspoweredcake.client33.utils.render;

import java.util.*;

/** Detached model geometry. No live world or entity references are retained. */
public record SurfaceMesh(List<Face> faces) {
    public static final int MAX_FACES = 512;
    public SurfaceMesh { faces = List.copyOf(faces.subList(0, Math.min(faces.size(), MAX_FACES))); }
    public record Point(double x, double y, double z) {
        public Point lerp(Point other, double t) { return new Point(x + (other.x-x)*t, y + (other.y-y)*t, z + (other.z-z)*t); }
        public double distance(Point p) { return Math.sqrt((x-p.x)*(x-p.x)+(y-p.y)*(y-p.y)+(z-p.z)*(z-p.z)); }
    }
    public record Face(Point a, Point b, Point c, Point d) {
        public List<Point> points() { return List.of(a,b,c,d); }
    }
    public record Edge(Point a, Point b) { public double length() { return a.distance(b); } }
    public double minY() { return faces.stream().flatMap(f -> f.points().stream()).mapToDouble(Point::y).min().orElse(0); }
    public double maxY() { return faces.stream().flatMap(f -> f.points().stream()).mapToDouble(Point::y).max().orElse(0); }

    /** Intersects actual model faces with a horizontal contour plane. */
    public List<Edge> contour(double height) {
        List<Edge> lines = new ArrayList<>();
        for (Face face : faces) {
            List<Point> hits = new ArrayList<>(4), points = face.points();
            for (int i=0; i<4; i++) {
                Point a=points.get(i), b=points.get((i+1)%4);
                if ((a.y<=height && b.y>height) || (b.y<=height && a.y>height))
                    hits.add(a.lerp(b,(height-a.y)/(b.y-a.y)));
            }
            for (int i=0; i+1<hits.size(); i+=2) if (hits.get(i).distance(hits.get(i+1))>1e-6)
                lines.add(new Edge(hits.get(i),hits.get(i+1)));
        }
        return lines;
    }

    /** Clips a face to the moving radar band, including faces spanning both band edges. */
    public static List<Point> band(Face face, double bottom, double top) {
        return clip(clip(face.points(),bottom,true),top,false);
    }
    private static List<Point> clip(List<Point> input, double y, boolean above) {
        List<Point> out=new ArrayList<>();
        if (input.isEmpty()) return out;
        Point previous=input.getLast();
        boolean wasInside=above ? previous.y>=y : previous.y<=y;
        for (Point point:input) {
            boolean inside=above ? point.y>=y : point.y<=y;
            if (inside!=wasInside) out.add(previous.lerp(point,(y-previous.y)/(point.y-previous.y)));
            if (inside) out.add(point);
            previous=point; wasInside=inside;
        }
        return out;
    }

    // Weld independently emitted face corners, then keep front/back boundaries only.
    private record Key(long x,long y,long z) implements Comparable<Key> {
        Key(Point p) { this(Math.round(p.x*100000),Math.round(p.y*100000),Math.round(p.z*100000)); }
        public int compareTo(Key k) { int c=Long.compare(x,k.x); if(c==0)c=Long.compare(y,k.y); return c==0?Long.compare(z,k.z):c; }
    }
    private record Pair(Key a,Key b) { }
    private static final class Adjacent { Edge edge; int count; boolean front,back; }
    public List<Edge> silhouette(Point camera) {
        Map<Pair,Adjacent> edges=new LinkedHashMap<>();
        for (Face f:faces) {
            double ux=f.b.x-f.a.x,uy=f.b.y-f.a.y,uz=f.b.z-f.a.z;
            double vx=f.c.x-f.a.x,vy=f.c.y-f.a.y,vz=f.c.z-f.a.z;
            double dot=(uy*vz-uz*vy)*(camera.x-f.a.x)+(uz*vx-ux*vz)*(camera.y-f.a.y)+(ux*vy-uy*vx)*(camera.z-f.a.z);
            List<Point> ps=f.points();
            for(int i=0;i<4;i++) {
                Point a=ps.get(i),b=ps.get((i+1)%4); if(a.distance(b)<1e-6)continue;
                Key ka=new Key(a),kb=new Key(b); Pair key=ka.compareTo(kb)<=0?new Pair(ka,kb):new Pair(kb,ka);
                Adjacent edge=edges.computeIfAbsent(key,k->new Adjacent());
                edge.edge=new Edge(a,b); edge.count++; edge.front|=dot>1e-10; edge.back|=dot<=1e-10;
            }
        }
        List<Edge> boundary=edges.values().stream().filter(e->e.count==1 || e.front&&e.back).map(e->e.edge).toList();
        Map<Key,List<Edge>> connected=new HashMap<>();
        for(Edge edge:boundary) {
            connected.computeIfAbsent(new Key(edge.a),k->new ArrayList<>()).add(edge);
            connected.computeIfAbsent(new Key(edge.b),k->new ArrayList<>()).add(edge);
        }
        Set<Edge> visited=new HashSet<>();
        List<Edge> ordered=new ArrayList<>();
        for(Edge start:boundary) {
            if(visited.contains(start))continue;
            Edge edge=start;
            while(edge!=null) {
                visited.add(edge);ordered.add(edge);
                Key end=new Key(edge.b); Edge next=null;
                for(Edge candidate:connected.get(end))if(!visited.contains(candidate)) {
                    next=new Key(candidate.a).equals(end)?candidate:new Edge(candidate.b,candidate.a);
                    // Membership uses the original edge orientation.
                    visited.add(candidate);break;
                }
                edge=next;
            }
        }
        return ordered;
    }
    public static Point orbit(List<Edge> edges,double progress) {
        double total=edges.stream().mapToDouble(Edge::length).sum();
        if(total<=1e-8)return null;
        double distance=(progress-Math.floor(progress))*total;
        for(Edge edge:edges) { double length=edge.length(); if(distance<length)return edge.a.lerp(edge.b,distance/length); distance-=length; }
        return edges.getLast().b;
    }
}
