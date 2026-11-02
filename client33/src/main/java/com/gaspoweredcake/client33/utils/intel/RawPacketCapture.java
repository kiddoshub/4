package com.gaspoweredcake.client33.utils.intel;

import com.gaspoweredcake.client33.mixin.PacketDecoderAccessor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.PacketDecoder;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayDeque;
import io.netty.channel.Channel;

/** Observes framed, decompressed clientbound bytes without changing their reader index or ownership. */
public final class RawPacketCapture extends ChannelInboundHandlerAdapter {
    private static final AtomicLong NEXT_CONNECTION = new AtomicLong();
    private static volatile int prefixBytes = 32;
    private static volatile boolean enabled = true;
    private final long connection = NEXT_CONNECTION.incrementAndGet();
    private final ArrayDeque<PacketForensics.Packet> pending = new ArrayDeque<>();

    public static void setEnabled(boolean value) { enabled = value; }

    public static void setPrefixBytes(int count) {
        if (count < 0 || count > 256) throw new IllegalArgumentException("Prefix must be between 0 and 256 bytes");
        prefixBytes = count;
    }

    /** Publish only after the decoded sign guard has run for this frame or bundle. */
    public static void publish(Channel channel) {
        var handler = channel.pipeline().get("client33-intel");
        if (!(handler instanceof RawPacketCapture capture)) return;
        PacketForensics.Packet packet;
        while ((packet = capture.pending.pollFirst()) != null) IntelBus.rawPacket(packet);
    }

    @Override public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
        if (enabled && message instanceof ByteBuf bytes) {
            try {
                int start = bytes.readerIndex(), length = bytes.readableBytes(), id = 0, shift = 0;
                boolean complete = false;
                for (int n = 0; n < Math.min(length, 5); n++) {
                    int value = bytes.getUnsignedByte(start + n);
                    id |= (value & 127) << shift;
                    if ((value & 128) == 0) { complete = true; break; }
                    shift += 7;
                }
                if (!complete || id < 0) id = -1;
                byte[] prefix = new byte[Math.min(length, prefixBytes)];
                bytes.getBytes(start, prefix);
                var decoder = ctx.pipeline().get("decoder");
                String phase = decoder instanceof PacketDecoder<?> packetDecoder
                    ? ((PacketDecoderAccessor) packetDecoder).client33$getProtocolInfo().id().toString() : "unconfigured";
                if (pending.size() >= 512) { pending.removeFirst(); IntelBus.rawFrameDropped(); }
                pending.addLast(new PacketForensics.Packet(connection, phase, id, length, System.nanoTime(), prefix));
            } catch (RuntimeException failure) {
                // Observation must never change packet delivery.
            }
        }
        ctx.fireChannelRead(message);
    }
}
