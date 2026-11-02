package com.gaspoweredcake.client33.systems.modules.render;

import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import com.gaspoweredcake.client33.events.render.Render3DEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.friends.Friends;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.combat.CombatTelemetry;
import com.gaspoweredcake.client33.utils.recon.MovementHeat;
import com.gaspoweredcake.client33.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;

public class MovementHeatmap extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Integer> cellSize = sg.add(new IntSetting.Builder().name("cell-size").description("Horizontal aggregation size in blocks. Changing this clears the map.").defaultValue(8).range(4, 32).sliderRange(4, 32).build());
    private final Setting<Integer> halfLife = sg.add(new IntSetting.Builder().name("half-life-seconds").description("Time for movement intensity to halve.").defaultValue(120).range(10, 1800).sliderRange(10, 600).build());
    private final Setting<Integer> expire = sg.add(new IntSetting.Builder().name("expire-seconds").description("Delete old cells from this session.").defaultValue(600).range(30, 3600).sliderRange(30, 3600).build());
    private final Setting<Integer> minVisits = sg.add(new IntSetting.Builder().name("minimum-samples").description("Required movement observations before showing a cell. Heat is not proof of a base.").defaultValue(3).range(1, 50).sliderRange(1, 50).build());
    private final Setting<Boolean> includeSelf = sg.add(new BoolSetting.Builder().name("include-self").description("Include your own movement, useful for testing the overlay.").defaultValue(false).build());
    private final Setting<Boolean> includeFriends = sg.add(new BoolSetting.Builder().name("include-friends").description("Include visible friends in aggregate activity.").defaultValue(false).build());
    private final Setting<Integer> range = sg.add(new IntSetting.Builder().name("render-distance").description("Maximum distance to render stored movement cells.").defaultValue(128).range(16, 512).sliderRange(16, 512).build());
    private final MovementHeat heat = new MovementHeat(4096);
    private Object world;
    private int tick, size;
    public MovementHeatmap() { super(Categories.Render, "movement-heatmap", "Session heatmap of received player movement. Repeated activity can guide scouting; it does not confirm a base."); }
    private void clear() { heat.clear(); world = mc.level; tick = 0; size = cellSize.get(); }
    @Override public void onActivate() { clear(); }
    @Override public void onDeactivate() { clear(); }
    @EventHandler private void onLeave(GameLeftEvent event) { clear(); }
    @EventHandler private void onTick(TickEvent.Post event) {
        if (world != mc.level || size != cellSize.get()) clear();
        if (++tick % 10 != 0) return;
        long now = CombatTelemetry.now();
        heat.expire(now, expire.get() * 1000L);
        for (var player : mc.level.players()) {
            if (!player.isAlive() || player.isSpectator() || (player == mc.player ? !includeSelf.get() : !includeFriends.get() && Friends.get().isFriend(player))) continue;
            heat.sample(player.getUUID(), player.getX(), player.getY(), player.getZ(), now, size, halfLife.get() * 1000.0);
        }
    }
    @EventHandler private void onRender(Render3DEvent event) {
        if (mc.player == null || world != mc.level) return;
        int count = 0;
        for (var cell : heat.cells()) {
            if (cell.visits() < minVisits.get()) continue;
            double x = cell.cell().x() * (double) size, z = cell.cell().z() * (double) size;
            if (mc.player.distanceToSqr(x + size / 2.0, cell.y(), z + size / 2.0) > range.get() * range.get()) continue;
            double energy = MovementHeat.decayed(cell, CombatTelemetry.now(), halfLife.get() * 1000.0);
            if (energy < 0.1) continue;
            if (++count > 512) break;
            double strength = Math.min(1, Math.log1p(energy) / Math.log(31));
            Color color = new Color(255, (int) (210 * (1 - strength)), 65, (int) (25 + strength * 100));
            event.renderer.quadHorizontal(x, cell.y() + 0.03, z, x + size, z + size, color);
            event.renderer.boxLines(x, cell.y() + 0.04, z, x + size, cell.y() + 0.08, z + size, new Color(color).a(160), 0);
        }
    }
    @Override public WWidget getWidget(GuiTheme theme) {
        var button = theme.button("Clear session movement"); button.action = this::clear; return button;
    }
    @Override public String getInfoString() { return heat.cells().size() + " activity cells"; }
}
