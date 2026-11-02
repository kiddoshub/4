#version 330 core
#extension GL_ARB_separate_shader_objects : require

layout (location = 0) in vec2 v_TexCoord;
layout (location = 1) in vec2 v_OneTexel;
uniform sampler2D u_Texture;
uniform sampler2D u_Material;
layout (std140) uniform FractureData {
    vec4 u_CellColor;
    vec4 u_SeamColor;
    vec4 u_Parameters; // cell size, seam width, halo intensity, cyclic phase
    vec4 u_Finish; // bevel depth, cell deformation, seam flow, pattern
    vec4 u_Composition; // secondary density, roughness, light angle (radians), tonal variation
    vec4 u_Print; // engraving strength, spacing (pixels), ink contrast, ochre wash
    vec4 u_Palette; // custom metal accent RGB, palette (custom / print / neon / ivory)
    vec4 u_Upload; // image blend, tiling, image aspect, viewport aspect
};
layout (location = 0) out vec4 color;
const float TAU = 6.283185307;

vec2 seedFor(vec2 cell) {
    return fract(sin(vec2(dot(cell, vec2(127.1,311.7)),dot(cell,vec2(269.5,183.3))))*43758.5453);
}
vec2 cellPoint(vec2 cell) {
    vec2 seed=seedFor(cell);
    return 0.20+0.60*seed+0.18*u_Finish.y*sin(vec2(u_Parameters.w*TAU,u_Parameters.w*TAU+1.5707963)+seed*TAU);
}
struct CellSurface { vec2 cell; vec2 normal; float border; float radius; };
CellSurface cellSurface(vec2 p) {
    vec2 grid=floor(p),local=fract(p),nearest=vec2(0),nearestCell=grid;
    float best=100.0;
    for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++) {
        vec2 cell=grid+vec2(x,y);
        vec2 delta=vec2(x,y)+cellPoint(cell)-local;
        float d=dot(delta,delta);
        if(d<best) {best=d;nearest=delta;nearestCell=cell;}
    }
    // Distance to actual Voronoi bisectors, rather than second-minus-first distance.
    float border=100.0;
    vec2 borderNormal=vec2(1,0);
    for(int y=-2;y<=2;y++)for(int x=-2;x<=2;x++) {
        vec2 cell=nearestCell+vec2(x,y);
        vec2 other=cell+cellPoint(cell)-p;
        vec2 separation=other-nearest;
        float lengthSquared=dot(separation,separation);
        if(lengthSquared>0.00001) {
            vec2 normal=separation*inversesqrt(lengthSquared);
            float distanceToEdge=dot(0.5*(nearest+other),normal);
            if(distanceToEdge<border) {border=distanceToEdge;borderNormal=normal;}
        }
    }
    return CellSurface(nearestCell,borderNormal,border,sqrt(best));
}
// Distance and gradient for a continuous quadratic curve, approximated with six segments.
// Only Branching evaluates this path; other materials pay no curve iteration cost.
vec3 curve(vec2 p, vec2 a, vec2 b, vec2 c) {
    vec2 previous=a, nearest=p-a;
    float best=dot(nearest,nearest);
    for(int i=1;i<=6;i++) {
        float t=float(i)/6.0;
        vec2 next=mix(mix(a,b,t),mix(b,c,t),t);
        vec2 segment=next-previous;
        vec2 delta=p-previous-segment*clamp(dot(p-previous,segment)/max(dot(segment,segment),0.00001),0.0,1.0);
        float squared=dot(delta,delta);
        if(squared<best) {best=squared;nearest=delta;}
        previous=next;
    }
    float distance=sqrt(best);
    return vec3(nearest/max(distance,0.00001),distance);
}

vec3 branchChannels(vec2 p, float width) {
    // Large, asymmetric growth systems: main stem, unequal forks and finer offshoots.
    vec2 tile=floor(p/4.8), seed=seedFor(tile);
    vec2 q=mod(p,4.8)-2.4;
    // Periodic warp joins the main stem continuously at tile boundaries.
    float warpPhase=q.y*TAU/4.8+u_Parameters.w*TAU;
    q.x+=0.23*sin(warpPhase)*u_Finish.y;
    float warpSlope=0.23*TAU/4.8*cos(warpPhase)*u_Finish.y;
    if(seed.x>0.5)q.x=-q.x;
    vec3 nearest=vec3(sign(q.x),0,abs(q.x));
    nearest.z-=width*1.25;
    for(int i=0;i<4;i++) {
        float f=float(i), side=mod(f,2.0)*2.0-1.0;
        float y=-1.45+f*0.88;
        vec2 a=vec2(0,y);
        vec2 b=vec2(side*(0.9+0.2*seed.y),y+0.3);
        vec2 c=vec2(side*(1.35+0.55*seed.y),y+1.0);
        vec3 fork=curve(q,a,b,c);
        fork.z-=width*mix(0.75,0.35,f/3.0);
        if(fork.z<nearest.z)nearest=fork;
        // Smaller subordinate offshoots appear only in selected growth systems.
        if(u_Composition.x>0.0 && seed.y<u_Composition.x && i<2) {
            vec2 junction=mix(mix(a,b,0.45),mix(b,c,0.45),0.45);
            vec3 twig=curve(q,junction,b+vec2(-side*0.15,0.5),c+vec2(-side*0.4,0.65));
            twig.z-=width*0.24;
            if(twig.z<nearest.z)nearest=twig;
        }
    }
    if(seed.x>0.5)nearest.x=-nearest.x;
    nearest.xy=normalize(vec2(nearest.x,nearest.y+nearest.x*warpSlope)+vec2(0.000001));
    return nearest;
}

// Controlled eight-point inlay. Shared proportions keep repeated subdivisions coherent.
vec3 geometricChannels(vec2 p, float width) {
    vec2 q=fract(p*1.4)-0.5;
    vec2 rotated=mat2(0.70710678,0.70710678,-0.70710678,0.70710678)*q;
    float square=max(abs(q.x),abs(q.y));
    float diagonal=max(abs(rotated.x),abs(rotated.y));
    bool direct=square<diagonal;
    vec2 local=direct?q:rotated;
    vec2 normal=abs(local.x)>abs(local.y)?vec2(sign(local.x),0):vec2(0,sign(local.y));
    if(!direct)normal=mat2(0.70710678,-0.70710678,0.70710678,0.70710678)*normal;
    float star=min(square,diagonal)-0.285;
    return vec3(normal*sign(star),abs(star)/1.4-width*0.38);
}

void main() {
    vec3 baseTint=u_CellColor.rgb,seamTint=u_SeamColor.rgb,accentTint=u_Palette.rgb;
    if(u_Palette.w>0.5 && u_Palette.w<1.5) {
        baseTint=vec3(0.737,0.545,0.173);seamTint=vec3(1.0,0.933,0.600);accentTint=vec3(0.92,0.70,0.30);
    } else if(u_Palette.w>1.5 && u_Palette.w<2.5) {
        baseTint=vec3(0.13,0.15,0.255);seamTint=vec3(0.66,0.49,0.97);accentTint=vec3(0.96,0.77,0.32);
    } else if(u_Palette.w>2.5) {
        baseTint=vec3(0.84,0.79,0.66);seamTint=vec3(0.78,0.59,0.30);accentTint=vec3(1.0,0.93,0.74);
    }
    vec2 p=gl_FragCoord.xy/max(u_Parameters.x,1.0);
    // Evaluate derivatives before mask branches; seam AA follows the raster footprint.
    float aa=max(length(dFdx(p)),length(dFdy(p)));
    float mask=texture(u_Texture,v_TexCoord).a;
    if(mask<=0.0) {
        if(u_Parameters.z<=0.0)discard;
        float halo=0.0;
        for(int i=0;i<8;i++) {
            float angle=float(i)*TAU/8.0;
            vec2 direction=vec2(cos(angle),sin(angle))*v_OneTexel;
            halo+=texture(u_Texture,v_TexCoord+direction*2.0).a*0.09;
            halo+=texture(u_Texture,v_TexCoord+direction*4.0).a*0.035;
        }
        halo*=0.5*u_Parameters.z*u_SeamColor.a*u_CellColor.a;
        if(halo<=0.0)discard;
        color=vec4(seamTint,halo);
        return;
    }
    if(u_Upload.x>0.0) {
        vec2 imageUv=v_TexCoord-0.5;
        float aspect=u_Upload.w/max(u_Upload.z,0.001);
        if(aspect>1.0)imageUv.y/=aspect;else imageUv.x*=aspect;
        vec4 uploaded=texture(u_Material,imageUv*u_Upload.y+0.5);
        baseTint=mix(baseTint,uploaded.rgb,uploaded.a*u_Upload.x);
    }
    bool sculpted=u_Finish.w<1.5;
    CellSurface plate=CellSurface(floor(p/4.8),vec2(1,0),100.0,0.0);
    if(sculpted)plate=cellSurface(p);
    vec2 seed=seedFor(plate.cell);
    if(!sculpted)seed=vec2(0.5+0.5*sin(p.x*0.22+p.y*0.17),0.5+0.5*cos(p.y*0.31-p.x*0.12));
    float halfWidth=0.5*u_Parameters.y/max(u_Parameters.x,1.0);
    // Broad quiet areas and focal clusters, rather than uniform detail over every plate.
    float region=0.5+0.5*sin(p.x*0.34+p.y*0.21+0.7*sin(p.y*0.38));
    float density=clamp(u_Composition.x,0.0,1.0);
    float detail=smoothstep(1.0-density-0.18,1.0-density+0.18,region*0.65+seed.y*0.35);
    float width=halfWidth*mix(0.65,1.25,seed.y);
    vec3 channel=vec3(-plate.normal,plate.border-width);
    if(u_Finish.w>1.5)channel.z+=0.10+seed.x*0.12; // Long flowing paths lead these materials.
    if(u_Finish.w>0.5 && u_Finish.w<1.5) {
        vec3 micro=geometricChannels(p,width);
        micro.z+=(1.0-detail)*0.07;
        if(micro.z<channel.z)channel=micro;
    } else if(u_Finish.w>1.5 && u_Finish.w<2.5) {
        // Ukiyo-e-like directional flow: broad curved currents with restrained tributaries.
        float phase=u_Parameters.w*TAU;
        vec2 q=mat2(0.88,0.475,-0.475,0.88)*p;
        float wave=q.y+0.42*sin(q.x*0.8+phase)*u_Finish.y+0.16*sin(q.x*1.7);
        float waveSlope=0.336*cos(q.x*0.8+phase)*u_Finish.y+0.272*cos(q.x*1.7);
        float value=sin(wave*2.1);
        float d=abs(value)/2.1-width*0.85;
        vec2 normal=normalize(vec2(waveSlope,1.0))*sign(value*cos(wave*2.1));
        normal=mat2(0.88,-0.475,0.475,0.88)*normal;
        vec3 vein=vec3(normal,d);
        if(vein.z<channel.z)channel=vein;
        float tributary=abs(sin(wave*2.1+q.x*0.65+0.25*sin(q.x)))/2.2-width*0.3+(1.0-detail)*0.12;
        if(tributary<channel.z)channel=vec3(normal,tributary);
    } else if(u_Finish.w>2.5) {
        vec3 branch=branchChannels(p*1.5,width*1.5);
        branch.z/=1.5;
        branch.z+=(1.0-density)*0.018;
        if(branch.z<channel.z)channel=branch;
    }
    float recess=1.0-smoothstep(-aa*0.5,aa*0.5,channel.z);
    float bevelWidth=mix(0.04,0.105,seed.x);
    float bevel=1.0-smoothstep(0.0,bevelWidth,max(channel.z,0.0));
    float depth=clamp(u_Finish.x,0.0,1.0);
    float roughness=clamp(u_Composition.y+(seed.y-0.5)*0.16,0.04,1.0);
    vec3 normal=normalize(vec3(channel.xy*bevel*depth*1.8,1.0));
    vec3 light=normalize(vec3(cos(u_Composition.z),sin(u_Composition.z),0.95));
    vec3 halfLight=normalize(light+vec3(0,0,1));
    float diffuse=max(dot(normal,light),0.0);
    float specular=pow(max(dot(normal,halfLight),0.0),mix(110.0,16.0,roughness));
    float tonal=clamp(u_Composition.w,0.0,1.0);
    vec3 amber=mix(vec3(1.0),vec3(1.0,0.77,0.46),tonal*(0.2+0.5*seed.x));
    // Flowing materials have continuous tonal fields; plate facets lead the other two.
    float surfaceVariation=u_Finish.w>1.5?0.35+0.4*region:seed.x;
    vec3 cell=baseTint*amber*mix(0.65,0.98,surfaceVariation)*(0.48+0.52*diffuse);
    cell=mix(cell,cell*vec3(1.08,1.01,0.82)+vec3(0.035,0.022,0.005),u_Print.w);
    if(u_Palette.w>2.5) {
        float focal=0.5+0.5*sin(dot(p,light.xy)*0.24+0.5);
        cell*=mix(0.32,1.16,smoothstep(0.28,0.82,focal));
    }
    cell*=1.0-depth*0.10*plate.radius;
    vec3 metal=mix(seamTint,accentTint,0.25)*mix(vec3(1.0),vec3(1.0,0.95,0.74),tonal*seed.y);
    vec3 floorColor=mix(cell*0.27,metal*0.20,0.2)*mix(1.0,0.40,u_Print.z);
    vec3 surface=mix(cell,floorColor,recess*depth);
    // Curved engraving follows broad directional currents. Quiet areas retain unmarked wash.
    // Pixel-based spacing and a fixed subpixel transition avoid unstable high-frequency noise.
    float hatch=(dot(gl_FragCoord.xy,vec2(0.64,0.768))
        +u_Parameters.x*(0.22*sin(p.y*0.65)+0.18*sin(p.x*0.42)))/max(u_Print.y,2.0);
    float inkDistance=abs(fract(hatch)-0.5)*max(u_Print.y,2.0);
    float etched=1.0-smoothstep(0.20,0.85,inkDistance);
    float inkRegion=smoothstep(0.2,0.8,region)*(0.35+0.65*(1.0-diffuse));
    // Slow value variation gives an uneven printed impression without frame-random speckle.
    float inkWear=0.74+0.26*sin(p.x*3.1+0.6*sin(p.y*2.2));
    float ink=etched*inkRegion*inkWear*u_Print.x*(0.3+0.7*u_Print.z)*(1.0-recess);
    if(u_Palette.w>2.5)ink*=0.35;
    surface=mix(surface,cell*vec3(0.15,0.13,0.10),ink);
    // Narrow directional metal edges; only occasional glints become focal highlights.
    float glint=pow(0.5+0.5*sin(dot(p,vec2(1.7,0.8))+u_Parameters.w*TAU*2.0+seed.x*TAU),8.0);
    float edge=(1.0-recess)*bevel*depth;
    surface+=metal*edge*(specular*mix(0.48,0.23,roughness)+glint*u_Finish.z*0.13)*u_SeamColor.a;
    if(u_Palette.w>1.5 && u_Palette.w<2.5) {
        // Selective violet channel light and warm marquee glints against indigo negative space.
        float pulse=0.65+0.20*glint*u_Finish.z;
        surface+=seamTint*recess*pulse*u_SeamColor.a;
        vec3 marquee=mix(accentTint,vec3(0.93,0.27,0.23),region*0.5);
        surface+=marquee*edge*specular*0.32*u_SeamColor.a;
    }
    color=vec4(clamp(surface,0.0,1.0),mask*u_CellColor.a);
}
