package com.gaspoweredcake.client33.systems.modules.render;

import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.events.render.Render2DEvent;
import com.gaspoweredcake.client33.events.render.Render3DEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.renderer.ShapeMode;
import com.gaspoweredcake.client33.renderer.text.TextRenderer;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.combat.CombatTelemetry;
import com.gaspoweredcake.client33.utils.recon.BlockLightSample;
import com.gaspoweredcake.client33.utils.render.NametagUtils;
import com.gaspoweredcake.client33.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.joml.Vector3d;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/** Bounded, session-only visualisation of observations actually received from the server. */
public class ActivityESP extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> sounds = sg.add(new BoolSetting.Builder().name("sounds").description("Mark received block place, break, step and container sounds.").defaultValue(true).build());
    private final Setting<Boolean> particles = sg.add(new BoolSetting.Builder().name("particles").description("Mark server-sent flame, smoke and note particles; locally generated particles are not captured.").defaultValue(true).build());
    private final Setting<Boolean> light = sg.add(new BoolSetting.Builder().name("block-light").description("Show received block-light samples in 8-block regions. Natural light sources can match.").defaultValue(true).build());
    private final Setting<Integer> maxY = sg.add(new IntSetting.Builder().name("light-max-y").description("Only show light samples at or below this Y coordinate.").defaultValue(0).range(-2048, 2048).sliderRange(-64, 128).build());
    private final Setting<Integer> minLight = sg.add(new IntSetting.Builder().name("minimum-block-light").description("Minimum block light level.").defaultValue(8).range(1, 15).sliderRange(1, 15).build());
    private final Setting<Integer> expiry = sg.add(new IntSetting.Builder().name("expiry-seconds").description("Discard observations after this many seconds.").defaultValue(120).range(5, 1800).sliderRange(5, 600).build());
    private final Setting<Integer> distance = sg.add(new IntSetting.Builder().name("render-distance").description("Maximum distance for markers.").defaultValue(128).range(8, 512).sliderRange(8, 256).build());
    private final Setting<Boolean> labels = sg.add(new BoolSetting.Builder().name("labels").description("Show observation type and light level.").defaultValue(true).build());
    private record Pending(Object packet, Object world) {}
    private record LightPacket(int x, int z, ClientboundLightUpdatePacketData data) {}
    private record Section(int x, int y, int z) {}
    private record LightWork(Section section, byte[] data, long seen) {}
    private record Mark(BlockPos pos, String label, long seen, boolean light) {}
    private final ArrayBlockingQueue<Pending> packets = new ArrayBlockingQueue<>(128);
    private final ArrayDeque<LightWork> lightQueue = new ArrayDeque<>();
    private final LinkedHashMap<Section, List<Mark>> lit = new LinkedHashMap<>();
    private final ArrayDeque<Mark> events = new ArrayDeque<>();
    private List<Mark> visibleMarks = List.of();
    private static final Color LIGHT_LINE = new Color(255, 193, 72, 190), LIGHT_FILL = new Color(255, 193, 72, 18);
    private static final Color EVENT_LINE = new Color(100, 215, 240, 210), EVENT_FILL = new Color(100, 215, 240, 18);
    private Object world;
    private int configuredY, configuredLight;

    public ActivityESP() { super(Categories.Render, "activity-esp", "Shows received sound, particle and block-light observations. Activity is not proof of a base."); }
    private void clear() { packets.clear(); lightQueue.clear(); lit.clear(); events.clear(); visibleMarks = List.of(); world = mc.level; configuredY = maxY.get(); configuredLight = minLight.get(); }
    @Override public void onActivate() { clear(); }
    @Override public void onDeactivate() { clear(); }
    @EventHandler private void onLeave(GameLeftEvent event) { clear(); }

    @EventHandler private void onPacket(PacketEvent.Receive event) {
        if (event.isCancelled()) return;
        Object p = event.packet;
        if (p instanceof ClientboundLevelChunkWithLightPacket chunk) {
            if (!light.get()) return;
            p = new LightPacket(chunk.x(), chunk.z(), chunk.lightData());
        } else if (p instanceof ClientboundLightUpdatePacket update) {
            if (!light.get()) return;
            p = new LightPacket(update.x(), update.z(), update.lightData());
        } else if (p instanceof ClientboundSoundPacket sound) {
            if (!sounds.get() || soundLabel(sound.getSound().value().location().getPath()) == null) return;
        } else if (p instanceof ClientboundSoundEntityPacket sound) {
            if (!sounds.get() || soundLabel(sound.getSound().value().location().getPath()) == null) return;
        } else if (p instanceof ClientboundLevelParticlesPacket particle) {
            if (!particles.get() || !trackedParticle(particle)) return;
        } else if (!(p instanceof ClientboundForgetLevelChunkPacket)) return;
        packets.offer(new Pending(p, mc.level));
    }

    @EventHandler private void onTick(TickEvent.Post ignored) {
        if (mc.level == null || mc.player == null) return;
        if (world != mc.level || configuredY != maxY.get() || configuredLight != minLight.get()) clear();
        long now = CombatTelemetry.now();
        for (int n = 0; n < 32; n++) {
            Pending pending = packets.poll();
            if (pending == null) break;
            if (pending.world != world) continue;
            Object p = pending.packet;
            if (p instanceof ClientboundSoundPacket sound && sounds.get()) {
                String label = soundLabel(sound.getSound().value().location().getPath());
                if (label != null) add(sound.getX(), sound.getY(), sound.getZ(), label + " / " + sound.getSource(), now);
            } else if (p instanceof ClientboundSoundEntityPacket sound && sounds.get()) {
                String label = soundLabel(sound.getSound().value().location().getPath());
                Entity entity = mc.level.getEntity(sound.getId());
                if (label != null && entity != null) add(entity.getX(), entity.getY(), entity.getZ(), label + " / " + sound.getSource(), now);
            } else if (p instanceof ClientboundLevelParticlesPacket packet && particles.get()) {
                var type = packet.particle().getType();
                add(packet.x(), packet.y(), packet.z(), "Particle / " + BuiltInRegistries.PARTICLE_TYPE.getKey(type).getPath(), now);
            } else if (p instanceof LightPacket update && light.get()) enqueueLight(update.x, update.z, update.data, now);
            else if (p instanceof ClientboundForgetLevelChunkPacket unload) {
                ChunkPos chunk = unload.pos();
                lit.keySet().removeIf(key -> key.x == chunk.x() && key.z == chunk.z());
                lightQueue.removeIf(item -> item.section.x == chunk.x() && item.section.z == chunk.z());
            }
        }
        if (!light.get()) { lightQueue.clear(); lit.clear(); }
        for (int n = 0; n < 2 && !lightQueue.isEmpty(); n++) {
            LightWork item = lightQueue.removeFirst();
            List<Mark> marks = new ArrayList<>(8);
            for (BlockLightSample.Result result : BlockLightSample.regions(item.data, item.section.y, maxY.get(), minLight.get()))
                marks.add(new Mark(new BlockPos(item.section.x * 16 + result.x(), result.y(), item.section.z * 16 + result.z()), "Block light / " + result.level(), item.seen, true));
            lit.remove(item.section);
            if (!marks.isEmpty()) lit.put(item.section, marks);
        }
        while (lit.size() > 1024) lit.remove(lit.keySet().iterator().next());
        long cutoff = now - expiry.get() * 1000L;
        events.removeIf(mark -> mark.seen < cutoff || (mark.label.startsWith("Particle / ") ? !particles.get() : !sounds.get()));
        lit.values().removeIf(marks -> marks.getFirst().seen < cutoff);
        visibleMarks = visible();
    }

    private void enqueueLight(int x, int z, ClientboundLightUpdatePacketData data, long now) {
        int base = mc.level.getChunkSource().getLightEngine().getMinLightSection();
        var empty = data.emptyBlockYMask();
        for (int bit = empty.nextSetBit(0); bit >= 0; bit = empty.nextSetBit(bit + 1)) {
            Section key = new Section(x, base + bit, z);
            lit.remove(key); lightQueue.removeIf(item -> item.section.equals(key));
        }
        var mask = data.blockYMask(); int index = 0;
        for (int bit = mask.nextSetBit(0); bit >= 0; bit = mask.nextSetBit(bit + 1)) {
            if (index >= data.blockUpdates().size()) break;
            byte[] bytes = data.blockUpdates().get(index++);
            Section key = new Section(x, base + bit, z);
            lightQueue.removeIf(item -> item.section.equals(key));
            lit.remove(key); // A newer update supersedes the old observation even if the queue is full.
            if ((long) key.y * 16 <= maxY.get()) {
                if (lightQueue.size() >= 128) lightQueue.removeFirst();
                lightQueue.addLast(new LightWork(key, bytes, now));
            }
        }
    }
    private static boolean trackedParticle(ClientboundLevelParticlesPacket packet) {
        var type = packet.particle().getType();
        return type == ParticleTypes.FLAME || type == ParticleTypes.SMALL_FLAME || type == ParticleTypes.SMOKE
            || type == ParticleTypes.CAMPFIRE_COSY_SMOKE || type == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE || type == ParticleTypes.NOTE;
    }
    private static String soundLabel(String path) {
        if (path.startsWith("block.") && path.endsWith(".break")) return "Break sound";
        if (path.startsWith("block.") && path.endsWith(".place")) return "Place sound";
        if (path.endsWith(".step")) return "Step sound";
        if ((path.contains("chest") || path.contains("barrel") || path.contains("shulker_box")) && (path.endsWith(".open") || path.endsWith(".close"))) return "Container sound";
        return null;
    }
    private void add(double x, double y, double z, String label, long now) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return;
        if (events.size() >= 512) events.removeFirst();
        events.addLast(new Mark(BlockPos.containing(x, y, z), label, now, false));
    }
    private boolean near(Mark mark) { return mc.player.distanceToSqr(mark.pos.getX() + 0.5, mark.pos.getY() + 0.5, mark.pos.getZ() + 0.5) <= (double) distance.get() * distance.get(); }
    private List<Mark> visible() {
        List<Mark> result = new ArrayList<>(256);
        Set<BlockPos> eventPositions = new HashSet<>();
        Iterator<Mark> observations = events.descendingIterator();
        Iterator<Mark> lighting = lit.values().stream().flatMap(List::stream).iterator();
        int soundCount = 0, lightCount = 0;
        while ((observations.hasNext() && soundCount < 128) || (lighting.hasNext() && lightCount < 128)) {
            if (observations.hasNext() && soundCount < 128) {
                Mark mark = observations.next();
                if (near(mark) && eventPositions.add(mark.pos)) { result.add(mark); soundCount++; }
            }
            if (lighting.hasNext() && lightCount < 128) {
                Mark mark = lighting.next();
                if (near(mark)) { result.add(mark); lightCount++; }
            }
        }
        return result;
    }
    @EventHandler private void onRender(Render3DEvent event) {
        if (mc.player == null || world != mc.level) return;
        int count = 0;
        for (Mark mark : visibleMarks) {
            if (++count > 256) break;
            Color line = mark.light ? LIGHT_LINE : EVENT_LINE;
            event.renderer.box(mark.pos, mark.light ? LIGHT_FILL : EVENT_FILL, line, ShapeMode.Both, 0);
        }
    }
    @EventHandler private void onLabels(Render2DEvent event) {
        if (!labels.get() || mc.player == null || world != mc.level) return;
        TextRenderer renderer = TextRenderer.get(); renderer.begin(event.graphics, 0.8);
        int count = 0;
        for (Mark mark : visibleMarks) {
            if (++count > 32) break;
            var pos = new Vector3d(mark.pos.getX() + 0.5, mark.pos.getY() + 1.1, mark.pos.getZ() + 0.5);
            if (NametagUtils.to2D(pos, 1, false)) renderer.render(mark.label, pos.x, pos.y, Color.WHITE, true);
        }
        renderer.end();
    }
    @Override public String getInfoString() { return events.size() + " events / " + lit.size() + " light sections"; }
}
