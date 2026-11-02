package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.render.MaterialImage;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.io.*;
import java.util.Comparator;

public final class MaterialImageRegression {
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path directory=Files.createTempDirectory("client33-material-regression-");
        try {
            Path png=directory.resolve("image.png");
            var source=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
            source.setRGB(0,0,0xffff0000);source.setRGB(1,0,0);source.setRGB(0,1,0xff0000ff);source.setRGB(1,1,0xff00ff00);
            ImageIO.write(source,"png",png.toFile());
            var image=MaterialImage.read(png);
            check(image.width()==2&&image.height()==2&&image.frameCount()==1,"PNG dimensions preserved");
            check(pixel(image,0,0,0)==0xffff0000&&pixel(image,0,0,1)==0xff0000ff&&pixel(image,0,1,0)==0,"RGBA orientation and transparency match OpenGL");
            Path gif=directory.resolve("animation.gif");writeGif(gif,4);
            var animation=MaterialImage.read(gif);
            check(animation.frameCount()==4&&animation.width()==4&&animation.height()==3,"GIF logical canvas preserved");
            check(pixel(animation,1,1,1)==0xff0000ff&&pixel(animation,1,0,0)==0xffff0000,"Offset frame composes over existing canvas");
            check(pixel(animation,2,1,1)==0xffff0000&&pixel(animation,2,2,1)==0xff00ff00,"Restore-to-previous removes only the temporary frame");
            check(pixel(animation,3,2,1)==0,"Restore-to-background clears transparent patch");
            check(animation.durationMillis()==220&&animation.frameAt(39)==0&&animation.frameAt(40)==1
                &&animation.frameAt(110)==2&&animation.frameAt(200)==3&&animation.frameAt(220)==0&&animation.frameAt(-1)==3,"GIF timing, minimum delay and looping remain deterministic");
            Path wide=directory.resolve("wide.png");ImageIO.write(new BufferedImage(2048,1024,BufferedImage.TYPE_INT_ARGB),"png",wide.toFile());
            var resized=MaterialImage.read(wide);check(resized.width()==1024&&resized.height()==512,"Large PNG resizes with unchanged aspect ratio");
            Path oversized=directory.resolve("oversized.png");ImageIO.write(new BufferedImage(2049,1,BufferedImage.TYPE_INT_ARGB),"png",oversized.toFile());
            rejected(oversized,"Oversized source dimensions rejected before canvas allocation");
            Path invalid=directory.resolve("invalid.png");Files.writeString(invalid,"not an image");rejected(invalid,"Invalid input rejected");
            Path big=directory.resolve("large.png");try(var file=new RandomAccessFile(big.toFile(),"rw")) {file.setLength(MaterialImage.MAX_FILE_BYTES+1);}
            rejected(big,"Oversized input rejected before decoding");
            Path tooMany=directory.resolve("many.gif");writeGif(tooMany,513);rejected(tooMany,"Excessive animation frames rejected without truncation");
            System.out.println("PNG/GIF orientation, transparency, disposal, playback and resource-bound regressions passed.");
        } finally {try(var paths=Files.walk(directory)) {for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
    private static int pixel(MaterialImage image,int frame,int x,int y) {
        var buffer=ByteBuffer.allocate(image.width()*image.height()*4);image.frame(frame).writeRgba(buffer);
        int offset=((image.height()-1-y)*image.width()+x)*4;
        return (buffer.get(offset+3)&255)<<24|(buffer.get(offset)&255)<<16|(buffer.get(offset+1)&255)<<8|buffer.get(offset+2)&255;
    }
    private static void rejected(Path file,String message) throws Exception {
        try {MaterialImage.read(file);throw new AssertionError(message);}catch(IOException expected) { }
    }
    private static void writeGif(Path path,int count) throws IOException {
        byte[] r={(byte)255,0,0,0},g={0,0,(byte)255,0},b={0,(byte)255,0,0};
        var palette=new IndexColorModel(8,4,r,g,b,3);
        var writer=ImageIO.getImageWritersByFormatName("gif").next();
        try(var output=ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(output);writer.prepareWriteSequence(null);
            for(int i=0;i<count;i++) {
                int width=i==0?4:1,height=i==0?3:1;
                var patch=new BufferedImage(width,height,BufferedImage.TYPE_BYTE_INDEXED,palette);
                for(int y=0;y<height;y++)for(int x=0;x<width;x++)patch.getRaster().setSample(x,y,0,i==1?1:i==2?2:0);
                var metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(patch),null);
                var tree=(IIOMetadataNode)metadata.getAsTree("javax_imageio_gif_image_1.0");
                var descriptor=(IIOMetadataNode)tree.getElementsByTagName("ImageDescriptor").item(0);
                descriptor.setAttribute("imageLeftPosition",Integer.toString(i==1?1:i==2?2:0));
                descriptor.setAttribute("imageTopPosition",i==1||i==2?"1":"0");
                var control=(IIOMetadataNode)tree.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("disposalMethod",i==1?"restoreToPrevious":i==2?"restoreToBackgroundColor":"none");
                control.setAttribute("delayTime",i==0?"4":i==1?"7":i==2?"9":"0");
                control.setAttribute("transparentColorFlag","TRUE");control.setAttribute("transparentColorIndex","3");
                metadata.setFromTree("javax_imageio_gif_image_1.0",tree);
                writer.writeToSequence(new IIOImage(patch,null,metadata),null);
            }
            writer.endWriteSequence();
        } finally {writer.dispose();}
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
