package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.render.SurfaceMesh;
import com.gaspoweredcake.client33.utils.render.PoseTrail;
import com.gaspoweredcake.client33.utils.render.RibbonStroke;
import java.util.*;

public final class SurfaceMeshRegression {
    private static SurfaceMesh.Point p(double x,double y,double z) { return new SurfaceMesh.Point(x,y,z); }
    public static void main(String[] args) { run(); }
    public static void run() {
        var wall=new SurfaceMesh.Face(p(0,0,0),p(1,0,0),p(1,2,0),p(0,2,0));
        var mesh=new SurfaceMesh(List.of(wall));
        var contour=mesh.contour(1);
        check(contour.size()==1 && Math.abs(contour.getFirst().length()-1)<1e-6,"Contour follows the model face");
        check(mesh.contour(3).isEmpty(),"Contours never extend outside the silhouette");
        var band=SurfaceMesh.band(wall,0.8,1.2);
        check(band.size()==4,"Radar clips a face crossing both sides of the band");
        check(band.stream().allMatch(v->v.y()>=0.8-1e-6 && v.y()<=1.2+1e-6),"Band never reveals geometry outside scan bounds");
        check(SurfaceMesh.band(wall,3,4).isEmpty(),"Off-model sweeps disappear");
        var faces=new ArrayList<SurfaceMesh.Face>();
        faces.add(new SurfaceMesh.Face(p(0,0,0),p(0,1,0),p(1,1,0),p(1,0,0)));
        faces.add(new SurfaceMesh.Face(p(0,0,1),p(1,0,1),p(1,1,1),p(0,1,1)));
        faces.add(new SurfaceMesh.Face(p(0,0,0),p(0,0,1),p(0,1,1),p(0,1,0)));
        faces.add(new SurfaceMesh.Face(p(1,0,0),p(1,1,0),p(1,1,1),p(1,0,1)));
        faces.add(new SurfaceMesh.Face(p(0,0,0),p(1,0,0),p(1,0,1),p(0,0,1)));
        faces.add(new SurfaceMesh.Face(p(0,1,0),p(0,1,1),p(1,1,1),p(1,1,0)));
        var cube=new SurfaceMesh(faces);faces.clear();
        check(cube.faces().size()==6,"Historical poses are detached from mutable collections");
        var silhouette=cube.silhouette(p(3,3,3));
        check(silhouette.size()==6,"Cube silhouette excludes internal model seams");
        for(int i=0;i<silhouette.size();i++)check(silhouette.get(i).b().distance(silhouette.get((i+1)%silhouette.size()).a())<1e-6,"Orbit follows connected silhouette edges without jumping between faces");
        var nearEdge=new SurfaceMesh.Edge(p(-1,0,10),p(1,0,10));
        var farEdge=new SurfaceMesh.Edge(p(-1,0,100),p(1,0,100));
        var near=RibbonStroke.offsets(nearEdge,p(0,0,0),1,1000,1.5);
        var far=RibbonStroke.offsets(farEdge,p(0,0,0),1,1000,1.5);
        double nearWidth=near.a().distance(p(0,0,0));double farWidth=far.a().distance(p(0,0,0));
        check(farWidth/nearWidth>9.9 && farWidth/nearWidth<10.1,"Distant ribbon width scales to preserve pixel readability");
        check(RibbonStroke.offsets(new SurfaceMesh.Edge(p(0,0,1),p(0,0,2)),p(0,0,0),1,1000,1)==null,"View-aligned lines cannot create invalid ribbon normals");
        var line=List.of(new SurfaceMesh.Edge(p(0,0,0),p(2,0,0)));
        check(Math.abs(SurfaceMesh.orbit(line,0.25).x()-0.5)<1e-6,"Particles trace actual silhouette edges");
        check(SurfaceMesh.orbit(line,1.25).equals(SurfaceMesh.orbit(line,0.25)),"Particle orbit wraps");
        check(SurfaceMesh.orbit(List.of(),0)==null,"Empty models have no particles");
        check(new SurfaceMesh(Collections.nCopies(1000,wall)).faces().size()==SurfaceMesh.MAX_FACES,"Capture storage is bounded");
        var trail=new PoseTrail();
        trail.capture(cube,0,p(0,0,0),4);
        trail.capture(cube,100_000_000L,p(0,0,0),4);
        check(trail.visible(100_000_000L,600_000_000L,4,p(0,0,0)).size()==1,"Stationary entities do not accumulate poses");
        for(int i=1;i<=8;i++)trail.capture(cube,i*100_000_000L,p(i,0,0),4);
        check(trail.visible(800_000_000L,600_000_000L,4,p(8,0,0)).size()==4,"Moving trail retains only its configured pose budget");
        check(trail.visible(800_000_000L,600_000_000L,2,p(8,0,0)).size()==2,"Lowering the live budget trims existing poses");
        check(trail.visible(1_500_000_000L,600_000_000L,4,p(8,0,0)).isEmpty(),"Expired poses disappear without new movement");
        trail.capture(cube,2_000_000_000L,p(0,0,0),4);
        check(trail.visible(2_100_000_000L,600_000_000L,4,p(30,0,0)).isEmpty(),"Teleports clear old silhouettes");
        System.out.println("Surface mesh and pose trail regressions passed.");
    }
    private static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
}
