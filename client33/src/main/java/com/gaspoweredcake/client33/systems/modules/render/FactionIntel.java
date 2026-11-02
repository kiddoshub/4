package com.gaspoweredcake.client33.systems.modules.render;

import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import com.gaspoweredcake.client33.events.game.ReceiveMessageEvent;
import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.events.render.Render2DEvent;
import com.gaspoweredcake.client33.events.render.Render3DEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.renderer.text.TextRenderer;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.combat.CombatTelemetry;
import com.gaspoweredcake.client33.utils.recon.ReconParser;
import com.gaspoweredcake.client33.utils.recon.TextObservations;
import com.gaspoweredcake.client33.utils.render.NametagUtils;
import com.gaspoweredcake.client33.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Locale;

public class FactionIntel extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final SettingGroup grammar = settings.createGroup("Server text grammar");
    private final Setting<Boolean> boundaries = sg.add(new BoolSetting.Builder().name("claim-boundaries").description("Render parsed chunk claims. Unknown chunks stay unknown.").defaultValue(true).build());
    private final Setting<Boolean> labels = sg.add(new BoolSetting.Builder().name("claim-labels").description("Show owner, source and observation age over nearby claims.").defaultValue(true).build());
    private final Setting<Boolean> powerHud = sg.add(new BoolSetting.Builder().name("power-tracker").description("Fuse faction power observations while flagging source disagreement.").defaultValue(true).build());
    private final Setting<Integer> ttl = sg.add(new IntSetting.Builder().name("expire-seconds").description("Discard stale claims and power readings.").defaultValue(300).range(10, 3600).sliderRange(10, 600).build());
    private final Setting<Integer> range = sg.add(new IntSetting.Builder().name("render-distance").description("Maximum claim render distance in blocks.").defaultValue(192).range(16, 512).sliderRange(16, 512).build());
    private final Setting<Double> height = sg.add(new DoubleSetting.Builder().name("boundary-height").description("Boundary wall height above your current feet level.").defaultValue(4).range(0, 32).sliderRange(0, 32).build());
    private final Setting<String> claimRegex = grammar.add(new StringSetting.Builder().name("claim-pattern").description("Full-line regex with exactly 3 groups: chunk X, chunk Z, owner. Coordinates must be explicit.").defaultValue(ReconParser.CLAIM).wide().build());
    private final Setting<String> powerRegex = grammar.add(new StringSetting.Builder().name("power-pattern").description("Full-line regex with exactly 3 groups: faction, power, max power. Also accepts Faction:/Power: pairs.").defaultValue(ReconParser.POWER).wide().build());
    private final Setting<String> mapHeaderRegex = grammar.add(new StringSetting.Builder().name("map-header-pattern").description("Full-line text-map regex: 2 groups for origin chunk X/Z.").defaultValue(ReconParser.MAP_HEADER).wide().build());
    private final Setting<String> mapRowRegex = grammar.add(new StringSetting.Builder().name("map-row-pattern").description("Full-line regex: 1 group for grid cells, excluding compass/prefix. Plus marks origin.").defaultValue(ReconParser.MAP_ROW).wide().build());
    private final Setting<String> mapLegendRegex = grammar.add(new StringSetting.Builder().name("map-legend-pattern").description("Repeated regex matches: 2 groups for one symbol and faction. Legend follows rows.").defaultValue(ReconParser.MAP_LEGEND).wide().build());
    private final ReconParser parser = new ReconParser();
    private final TextObservations observations = new TextObservations();
    private Object world;
    private String configured = "";
    private boolean valid = true;
    private int tick;

    public FactionIntel() { super(Categories.Render, "faction-intel", "Claim boundaries and faction power from observed chat, /f who, text maps, tab and sidebar data."); }
    private String signature() { return String.join("\n", claimRegex.get(), powerRegex.get(), mapHeaderRegex.get(), mapRowRegex.get(), mapLegendRegex.get()); }
    private boolean configureParser() {
        return parser.configure(claimRegex.get(), powerRegex.get()) && parser.configureMap(mapHeaderRegex.get(), mapRowRegex.get(), mapLegendRegex.get());
    }
    private void clear() { parser.clear(); observations.clear(); world = mc.level; tick = 0; }
    @Override public void onActivate() {
        clear();
        configured = signature();
        valid = configureParser();
    }
    @Override public void onDeactivate() { clear(); }
    @EventHandler private void onLeave(GameLeftEvent event) { clear(); }
    @EventHandler private void onChat(ReceiveMessageEvent event) {
        if (world != mc.level) clear();
        observations.offer(event.getMessage().getString(), ReconParser.Source.Chat);
    }
    @EventHandler private void onPacket(PacketEvent.Receive event) {
        if (!event.isCancelled() && event.packet instanceof ClientboundTabListPacket packet) {
            observations.offer(packet.header().getString() + "\n" + packet.footer().getString(), ReconParser.Source.Tab);
        }
    }
    @EventHandler private void onTick(TickEvent.Post event) {
        if (world != mc.level) clear();
        String signature = signature();
        if (!signature.equals(configured)) {
            configured = signature; valid = configureParser();
            parser.clear(); observations.clear();
        }
        long now = CombatTelemetry.now();
        parser.expire(now, ttl.get() * 1000L);
        if (++tick % 10 == 0 && valid) observations.drain(parser, now);
    }
    private boolean nearby(ReconParser.Claim claim) {
        double dx = claim.cell().x() * 16.0 + 8 - mc.player.getX(), dz = claim.cell().z() * 16.0 + 8 - mc.player.getZ();
        return dx * dx + dz * dz <= range.get() * range.get();
    }
    private Color color(String owner, int alpha) {
        int hash = owner.toLowerCase(Locale.ROOT).hashCode();
        return new Color(90 + (hash & 127), 90 + ((hash >>> 8) & 127), 90 + ((hash >>> 16) & 127), alpha);
    }
    @EventHandler private void onRender(Render3DEvent event) {
        if (!boundaries.get() || mc.player == null || world != mc.level) return;
        var owners = new HashMap<ReconParser.Cell, String>();
        parser.claims().forEach(c -> owners.put(c.cell(), c.owner()));
        int rendered = 0;
        for (var claim : parser.claims()) {
            if (!nearby(claim)) continue;
            if (++rendered > 256) break;
            double x = claim.cell().x() * 16.0, z = claim.cell().z() * 16.0, y = mc.player.getY() + 0.05;
            Color line = color(claim.owner(), 210), fill = color(claim.owner(), 22);
            int[] dx = {0, 1, 0, -1}, dz = {-1, 0, 1, 0};
            double[] xs = {x, x + 16, x + 16, x}, zs = {z, z, z + 16, z + 16};
            for (int edge = 0; edge < 4; edge++) {
                String neighbor = owners.get(new ReconParser.Cell(claim.cell().x() + dx[edge], claim.cell().z() + dz[edge]));
                if (claim.owner().equalsIgnoreCase(neighbor)) continue;
                int next = (edge + 1) % 4;
                event.renderer.line(xs[edge], y, zs[edge], xs[next], y, zs[next], line);
                event.renderer.quad(xs[edge], y, zs[edge], xs[next], y, zs[next], xs[next], y + height.get(), zs[next], xs[edge], y + height.get(), zs[edge], fill);
            }
        }
    }
    @EventHandler private void onHud(Render2DEvent event) {
        if (mc.player == null || world != mc.level) return;
        TextRenderer text = TextRenderer.get();
        text.begin(event.graphics, 0.8);
        long now = CombatTelemetry.now();
        if (labels.get() && boundaries.get()) {
            int count = 0;
            for (var claim : parser.claims()) {
                if (!nearby(claim)) continue;
                if (++count > 32) break;
                var pos = new Vector3d(claim.cell().x() * 16.0 + 8, mc.player.getY() + 1, claim.cell().z() * 16.0 + 8);
                if (NametagUtils.to2D(pos, 1, false)) text.render(claim.owner() + " / " + claim.source() + " / " + (now - claim.seen()) / 1000 + "s", pos.x, pos.y, color(claim.owner(), 255), true);
            }
        }
        if (powerHud.get()) {
            double y = 45;
            text.render(valid ? "Faction intel / observed data" : "Faction intel / invalid grammar / check capture groups", 12, y, Color.WHITE, true);
            y += 18;
            int count = 0;
            for (var readings : parser.powers().values()) {
                if (++count > 6) break;
                var newest = ReconParser.newest(readings.values());
                boolean conflict = readings.values().stream().anyMatch(p -> p.current() != newest.current() || p.maximum() != newest.maximum());
                String sources = readings.keySet().toString();
                text.render(String.format(Locale.ROOT, "%s %.1f/%.1f %s %ds%s", newest.faction(), newest.current(), newest.maximum(), sources,
                    (now - newest.seen()) / 1000, conflict ? " [disagreement]" : ""), 12, y, conflict ? new Color(255, 185, 90) : new Color(92, 220, 255), true);
                y += 18;
            }
            if (count == 0) text.render("No power observations. Open /f who or configure the server patterns.", 12, y, new Color(170, 170, 180), true);
        }
        text.end();
    }
    @Override public WWidget getWidget(GuiTheme theme) {
        var list = theme.verticalList();
        list.add(theme.label("Session observations only. Text maps require an explicit chunk origin and legend.", 450));
        list.add(theme.button("Clear observations")).expandX().widget().action = this::clear;
        list.add(theme.button("Request /f who")).expandX().widget().action = () -> { if (mc.player != null) mc.player.connection.sendCommand("f who"); };
        return list;
    }
    @Override public String getInfoString() { return parser.claims().size() + " claims / " + parser.powers().size() + " factions"; }
}
