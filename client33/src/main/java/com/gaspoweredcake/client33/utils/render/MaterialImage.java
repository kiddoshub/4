package com.gaspoweredcake.client33.utils.render;

import org.w3c.dom.Node;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** PNG/GIF decoding with composed disposal frames and a bounded decoded working set. No GPU access. */
public final class MaterialImage {
    public static final long MAX_FILE_BYTES=16L*1024*1024, MAX_FRAME_BYTES=64L*1024*1024;
    public static final int MAX_SOURCE_SIDE=2048, MAX_TEXTURE_SIDE=1024, MAX_FRAMES=512;
    public static final class Frame {
        private final byte[] rgba;
        private final int durationMillis;
        private Frame(byte[] rgba,int durationMillis) {this.rgba=rgba;this.durationMillis=durationMillis;}
        public int durationMillis() {return durationMillis;}
        public void writeRgba(ByteBuffer target) {target.put(rgba);}
    }
    private final int width,height;
    private final List<Frame> frames;
    private final long durationMillis;
    private MaterialImage(int width,int height,List<Frame> frames) {
        this.width=width;this.height=height;this.frames=List.copyOf(frames);
        this.durationMillis=frames.stream().mapToLong(Frame::durationMillis).sum();
    }
    public int width() {return width;}
    public int height() {return height;}
    public int frameCount() {return frames.size();}
    public long durationMillis() {return durationMillis;}
    public Frame frame(int index) {return frames.get(index);}
    public int frameAt(long elapsedMillis) {
        long time=Math.floorMod(elapsedMillis,durationMillis);
        for(int i=0;i<frames.size();i++) {time-=frames.get(i).durationMillis;if(time<0)return i;}
        return frames.size()-1;
    }
    public static MaterialImage read(Path file) throws IOException {
        if(Files.size(file)>MAX_FILE_BYTES)throw new IOException("Image exceeds 16 MiB.");
        try(ImageInputStream input=ImageIO.createImageInputStream(file.toFile())) {
            if(input==null)throw new IOException("Cannot open image.");
            var candidates=ImageIO.getImageReaders(input);
            if(!candidates.hasNext())throw new IOException("Choose a PNG or GIF image.");
            ImageReader reader=candidates.next();
            try {
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!format.equals("png")&&!format.equals("gif"))throw new IOException("Only PNG and GIF are supported.");
                reader.setInput(input,false,false);
                int count=reader.getNumImages(true);
                if(count<1||count>MAX_FRAMES)throw new IOException("GIF must contain 1–512 frames.");
                int width=reader.getWidth(0),height=reader.getHeight(0),background=0;
                if(format.equals("gif")) {
                    Node stream=reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
                    Node size=child(stream,"LogicalScreenDescriptor");
                    width=number(size,"logicalScreenWidth",width);height=number(size,"logicalScreenHeight",height);
                    Node colors=child(stream,"GlobalColorTable");int backgroundIndex=number(colors,"backgroundColorIndex",-1);
                    if(colors!=null)for(Node c=colors.getFirstChild();c!=null;c=c.getNextSibling())
                        if(number(c,"index",-2)==backgroundIndex)background=0xff000000|(number(c,"red",0)<<16)|(number(c,"green",0)<<8)|number(c,"blue",0);
                    Node first=reader.getImageMetadata(0).getAsTree("javax_imageio_gif_image_1.0");
                    Node control=child(first,"GraphicControlExtension");
                    if("TRUE".equals(attribute(control,"transparentColorFlag","FALSE")))background=0;
                }
                if(width<1||height<1||width>MAX_SOURCE_SIDE||height>MAX_SOURCE_SIDE)
                    throw new IOException("Source image must be at most 2048 pixels per side.");
                double scale=Math.min(1,Math.min((double)MAX_TEXTURE_SIDE/Math.max(width,height),
                    Math.sqrt((double)MAX_FRAME_BYTES/(4L*width*height*count))));
                int outWidth=Math.max(1,(int)Math.floor(width*scale)),outHeight=Math.max(1,(int)Math.floor(height*scale));
                var canvas=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                fill(canvas,0,0,width,height,background);
                var frames=new ArrayList<Frame>(count);
                for(int i=0;i<count;i++) {
                    int x=0,y=0,delay=100;String disposal="none";boolean transparent=false;
                    if(format.equals("gif")) {
                        Node metadata=reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                        Node descriptor=child(metadata,"ImageDescriptor"),control=child(metadata,"GraphicControlExtension");
                        x=number(descriptor,"imageLeftPosition",0);y=number(descriptor,"imageTopPosition",0);
                        delay=Math.max(20,number(control,"delayTime",10)*10);
                        disposal=attribute(control,"disposalMethod","none");
                        transparent="TRUE".equals(attribute(control,"transparentColorFlag","FALSE"));
                    }
                    int patchWidth=reader.getWidth(i),patchHeight=reader.getHeight(i);
                    if(x<0||y<0||patchWidth<1||patchHeight<1||x+patchWidth>width||y+patchHeight>height)
                        throw new IOException("GIF frame lies outside the canvas.");
                    BufferedImage previous=null;
                    if(disposal.equals("restoreToPrevious")) {
                        previous=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                        var g=previous.createGraphics();g.setComposite(AlphaComposite.Src);g.drawImage(canvas,0,0,null);g.dispose();
                    }
                    BufferedImage patch=reader.read(i);
                    var g=canvas.createGraphics();g.setComposite(AlphaComposite.SrcOver);g.drawImage(patch,x,y,null);g.dispose();patch.flush();
                    var output=new BufferedImage(outWidth,outHeight,BufferedImage.TYPE_INT_ARGB);
                    g=output.createGraphics();g.setComposite(AlphaComposite.Src);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(canvas,0,0,outWidth,outHeight,null);g.dispose();
                    byte[] rgba=new byte[outWidth*outHeight*4];int offset=0;
                    // Bottom row first: same OpenGL orientation as the existing Chams image loader.
                    for(int row=outHeight-1;row>=0;row--)for(int column=0;column<outWidth;column++) {
                        int argb=output.getRGB(column,row);
                        rgba[offset++]=(byte)(argb>>16);rgba[offset++]=(byte)(argb>>8);rgba[offset++]=(byte)argb;rgba[offset++]=(byte)(argb>>>24);
                    }
                    output.flush();frames.add(new Frame(rgba,delay));
                    if(disposal.equals("restoreToBackgroundColor"))fill(canvas,x,y,patchWidth,patchHeight,transparent?0:background);
                    else if(previous!=null) {canvas.flush();canvas=previous;}
                }
                canvas.flush();return new MaterialImage(outWidth,outHeight,frames);
            } finally {reader.dispose();}
        } catch(IllegalArgumentException|IndexOutOfBoundsException error) {throw new IOException("Invalid image metadata.",error);}
    }
    private static void fill(BufferedImage image,int x,int y,int width,int height,int color) {
        var g=image.createGraphics();g.setComposite(AlphaComposite.Src);g.setColor(new java.awt.Color(color,true));g.fillRect(x,y,width,height);g.dispose();
    }
    private static Node child(Node parent,String name) {
        if(parent!=null)for(Node node=parent.getFirstChild();node!=null;node=node.getNextSibling())if(node.getNodeName().equals(name))return node;
        return null;
    }
    private static String attribute(Node node,String name,String fallback) {
        if(node==null||node.getAttributes()==null)return fallback;
        var value=node.getAttributes().getNamedItem(name);return value==null?fallback:value.getNodeValue();
    }
    private static int number(Node node,String name,int fallback) {return Integer.parseInt(attribute(node,name,Integer.toString(fallback)));}
}
