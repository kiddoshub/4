package com.gaspoweredcake.client33.utils.render;

import com.gaspoweredcake.client33.events.render.Render3DEvent;
import com.gaspoweredcake.client33.renderer.Renderer3D;
import com.gaspoweredcake.client33.systems.modules.render.ESP;
import com.gaspoweredcake.client33.utils.render.color.Color;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import com.gaspoweredcake.client33.mixininterface.IEntityRenderState;
import java.util.*;
import static com.gaspoweredcake.client33.Client33.mc;

/** Model-surface effects with bounded, detached historical poses. Client/render thread only. */
public final class SurfaceEspRenderer {
    private final Map<UUID, PoseTrail> trails = new HashMap<>();
    private final Map<Entity,EntityRenderState> extractedStates=new IdentityHashMap<>();
    private final Set<UUID> seen = new HashSet<>();
    private final AnimationPhase clock = new AnimationPhase(System.nanoTime());
    private Object world;
    private ESP.Mode mode;
    private double phase;
    private SurfaceMesh.Point viewPosition;
    private double strokePixels;

    public void clear() { trails.clear(); seen.clear(); extractedStates.clear(); world=null; }
    public void begin(ESP esp) {
        if(world!=mc.level || mode!=esp.mode.get() || !esp.animate.get()) clear();
        world=mc.level; mode=esp.mode.get(); seen.clear();
        extractedStates.clear();
        for(var state:mc.gameRenderer.gameRenderState().levelRenderState.entityRenderStates) {
            Entity entity=((IEntityRenderState)state).client33$getEntity();
            if(entity!=null)extractedStates.put(entity,state);
            if(extractedStates.size()>=512)break;
        }
        phase=clock.sample(System.nanoTime(),esp.animate.get()?esp.surfaceSpeed.get():0);
    }
    public void end() { trails.keySet().retainAll(seen); extractedStates.clear(); }

    public void render(Render3DEvent event, Entity entity, ESP esp) {
        SurfaceMesh mesh=WireframeEntityRenderer.capture(event,entity,extractedStates.get(entity));
        if(mesh.faces().isEmpty())return;
        Color color=esp.getColor(entity); if(color==null)return;
        Color accent=new Color(esp.signatureAccent.get()).a(Math.min(color.a,esp.signatureAccent.get().a));
        Renderer3D renderer=esp.throughWalls.get()?event.renderer:event.depthRenderer;
        var camera=mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.pos;
        var cameraPoint=new SurfaceMesh.Point(camera.x,camera.y,camera.z);
        viewPosition=cameraPoint; strokePixels=esp.strokeWidth.get();
        double distance=new SurfaceMesh.Point(entity.getX(),entity.getY(),entity.getZ()).distance(cameraPoint);
        int detailDivisor=distance>128?3:distance>48?2:1;
        List<SurfaceMesh.Edge> edges=mesh.silhouette(cameraPoint);
        switch(esp.mode.get()) {
            case FlowingContours -> {
                drawEdges(renderer,edges,new Color(color).a(color.a/4));
                double min=mesh.minY(),height=Math.max(0.01,mesh.maxY()-min);
                int count=Math.max(3,esp.contourCount.get()/detailDivisor);
                for(int i=0;i<count;i++) {
                    double t=(i+phase)/count;
                    for(var edge:mesh.contour(min+t*height)) line(renderer,edge,accent);
                }
            }
            case RadarSweep -> {
                drawEdges(renderer,edges,new Color(color).a(color.a/5));
                double min=mesh.minY(),height=Math.max(0.01,mesh.maxY()-min);
                double half=height*esp.sweepWidth.get()/2;
                // Enter and leave fully outside the mesh, avoiding a visible wrap jump.
                double scan=min-half+phase*(height+2*half);
                for(var face:mesh.faces()) {
                    for(int halfBand=0;halfBand<2;halfBand++) {
                    List<SurfaceMesh.Point> polygon=SurfaceMesh.band(face,halfBand==0?scan-half:scan,halfBand==0?scan:scan+half);
                    for(int i=1;i+1<polygon.size();i++) {
                        var a=polygon.getFirst();var b=polygon.get(i);var c=polygon.get(i+1);
                        renderer.quad(a.x(),a.y(),a.z(),b.x(),b.y(),b.z(),c.x(),c.y(),c.z(),c.x(),c.y(),c.z(),
                            sweepColor(accent,a.y(),scan,half),sweepColor(accent,b.y(),scan,half),
                            sweepColor(accent,c.y(),scan,half),sweepColor(accent,c.y(),scan,half));
                    }
                }
                }
                for(var edge:mesh.contour(scan))line(renderer,edge,accent);
            }
            case GhostTrails -> {
                UUID id=entity.getUUID();seen.add(id);
                var history=trails.computeIfAbsent(id,k->new PoseTrail());
                long now=System.nanoTime();
                long lifetime=esp.trailLifetime.get()*1_000_000L;
                var position=new SurfaceMesh.Point(entity.getX(),entity.getY(),entity.getZ());
                for(var ghost:history.visible(now,lifetime,esp.trailSamples.get(),position)) {
                    double fade=1-(double)(now-ghost.nanos())/lifetime;
                    Color faint=new Color(color).a((int)(color.a*0.24*fade*fade));
                    for(var face:ghost.mesh().faces())fill(renderer,face,faint);
                    drawEdges(renderer,ghost.mesh().silhouette(cameraPoint),new Color(accent).a((int)(accent.a*0.35*fade)));
                }
                if(esp.animate.get()) history.capture(mesh,now,position,esp.trailSamples.get());
                drawEdges(renderer,edges,color);
            }
            case OrbitingParticles -> {
                drawEdges(renderer,edges,new Color(color).a(color.a/6));
                int count=Math.max(3,esp.particleCount.get()/detailDivisor);
                for(int i=0;i<count;i++) {
                    var p=SurfaceMesh.orbit(edges,phase+(double)i/count); if(p==null)continue;
                    double size=Math.max(0.012,Math.min(0.7,distance*3/(Math.max(0.1,Math.abs(RenderUtils.projection.m11()))*Math.max(1,mc.getWindow().getHeight()))));
                    line(renderer,new SurfaceMesh.Edge(new SurfaceMesh.Point(p.x()-size,p.y(),p.z()),new SurfaceMesh.Point(p.x()+size,p.y(),p.z())),accent);
                    line(renderer,new SurfaceMesh.Edge(new SurfaceMesh.Point(p.x(),p.y()-size,p.z()),new SurfaceMesh.Point(p.x(),p.y()+size,p.z())),accent);
                    line(renderer,new SurfaceMesh.Edge(new SurfaceMesh.Point(p.x(),p.y(),p.z()-size),new SurfaceMesh.Point(p.x(),p.y(),p.z()+size)),accent);
                }
            }
            case TwoLayer -> {
                if(esp.throughWalls.get()) for(var edge:edges) {
                    int steps=Math.max(1,(int)Math.ceil(edge.length()/0.10));
                    for(int i=0;i<steps;i++) {
                        var a=edge.a().lerp(edge.b(),(double)i/steps);
                        var b=edge.a().lerp(edge.b(),Math.min(1,(i+0.52)/steps));
                        line(event.renderer,new SurfaceMesh.Edge(a,b),new Color(accent).a((int)(accent.a*esp.hiddenOpacity.get())));
                    }
                }
                drawEdges(event.depthRenderer,edges,color);
            }
            default -> { }
        }
    }
    private static Color sweepColor(Color color,double y,double scan,double half) {
        double strength=Math.max(0,1-Math.abs(y-scan)/half);
        return new Color(color).a((int)(color.a*strength*0.7));
    }
    private static void fill(Renderer3D r,SurfaceMesh.Face f,Color color) {
        r.quad(f.a().x(),f.a().y(),f.a().z(),f.b().x(),f.b().y(),f.b().z(),f.c().x(),f.c().y(),f.c().z(),f.d().x(),f.d().y(),f.d().z(),color);
    }
    private void drawEdges(Renderer3D r,List<SurfaceMesh.Edge> edges,Color color) { for(var edge:edges)line(r,edge,color); }
    private void line(Renderer3D r,SurfaceMesh.Edge edge,Color color) {
        var offsets=RibbonStroke.offsets(edge,viewPosition,RenderUtils.projection.m11(),mc.getWindow().getHeight(),strokePixels);
        if(offsets==null)return;
        Color faint=new Color(color).a(color.a/5),clear=new Color(color).a(0);
        strip(r,edge,offsets,-1.8,-0.5,clear,faint);
        strip(r,edge,offsets,0.5,1.8,faint,clear);
        strip(r,edge,offsets,-0.5,0.5,color,color);
    }
    private static void strip(Renderer3D r,SurfaceMesh.Edge edge,RibbonStroke.Offsets offsets,double left,double right,Color lc,Color rc) {
        var a=RibbonStroke.offset(edge.a(),offsets.a(),left);var b=RibbonStroke.offset(edge.b(),offsets.b(),left);
        var c=RibbonStroke.offset(edge.b(),offsets.b(),right);var d=RibbonStroke.offset(edge.a(),offsets.a(),right);
        r.quad(a.x(),a.y(),a.z(),b.x(),b.y(),b.z(),c.x(),c.y(),c.z(),d.x(),d.y(),d.z(),lc,lc,rc,rc);
    }
}
