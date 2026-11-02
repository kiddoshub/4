package com.gaspoweredcake.client33.utils.render;

/** Camera-facing stroke geometry with approximately constant pixel width. */
public final class RibbonStroke {
    private RibbonStroke() { }
    public record Offsets(SurfaceMesh.Point a,SurfaceMesh.Point b) { }
    public static Offsets offsets(SurfaceMesh.Edge edge,SurfaceMesh.Point camera,double projectionY,int viewportHeight,double pixels) {
        double dx=edge.b().x()-edge.a().x(),dy=edge.b().y()-edge.a().y(),dz=edge.b().z()-edge.a().z();
        var mid=edge.a().lerp(edge.b(),0.5);
        double vx=camera.x()-mid.x(),vy=camera.y()-mid.y(),vz=camera.z()-mid.z();
        double nx=dy*vz-dz*vy,ny=dz*vx-dx*vz,nz=dx*vy-dy*vx;
        double length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(length<1e-9 || !Double.isFinite(length))return null;
        double scale=2*pixels/(Math.max(0.1,Math.abs(projectionY))*Math.max(1,viewportHeight));
        double a=Math.min(2,Math.max(0.001,edge.a().distance(camera))*scale)/length;
        double b=Math.min(2,Math.max(0.001,edge.b().distance(camera))*scale)/length;
        return new Offsets(new SurfaceMesh.Point(nx*a,ny*a,nz*a),new SurfaceMesh.Point(nx*b,ny*b,nz*b));
    }
    public static SurfaceMesh.Point offset(SurfaceMesh.Point point,SurfaceMesh.Point normal,double amount) {
        return new SurfaceMesh.Point(point.x()+normal.x()*amount,point.y()+normal.y()*amount,point.z()+normal.z()*amount);
    }
}
