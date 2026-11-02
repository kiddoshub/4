
package com.gaspoweredcake.client33.systems.modules.render;

import com.gaspoweredcake.client33.events.entity.EntityAddedEvent;
import com.gaspoweredcake.client33.events.render.Render2DEvent;
import com.gaspoweredcake.client33.events.render.Render3DEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.renderer.Renderer2D;
import com.gaspoweredcake.client33.renderer.ShapeMode;
import com.gaspoweredcake.client33.renderer.text.TextRenderer;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.player.PlayerUtils;
import com.gaspoweredcake.client33.utils.render.NametagUtils;
import com.gaspoweredcake.client33.utils.render.color.Color;
import com.gaspoweredcake.client33.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LogoutSpots extends Module {
    private static final Color GREEN = new Color(103, 224, 181);
    private static final Color ORANGE = new Color(255, 195, 110);
    private static final Color RED = new Color(255, 112, 135);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Setting<Boolean> fullHeight = sgGeneral.add(new BoolSetting.Builder()
        .name("full-height")
        .description("Displays the height as the player's full height.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(157, 111, 255, 55))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(157, 111, 255))
        .build()
    );

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
        .name("name-color")
        .description("The name color.")
        .defaultValue(new SettingColor(242, 238, 255))
        .build()
    );

    private final Setting<SettingColor> nameBackgroundColor = sgRender.add(new ColorSetting.Builder()
        .name("name-background-color")
        .description("The name background color.")
        .defaultValue(new SettingColor(20, 18, 32, 175))
        .build()
    );

    private final List<Entry> players = new ArrayList<>();

    private final List<PlayerInfo> lastPlayerList = new ArrayList<>();
    private final List<Player> lastPlayers = new ArrayList<>();

    private int timer;
    private DimensionType lastDimension;

    public LogoutSpots() {
        super(Categories.Render, "logout-spots", "Displays a box where another player has logged out at.");
        lineColor.onChanged();
    }

    @Override
    public void onActivate() {
        lastPlayerList.addAll(mc.getConnection().getOnlinePlayers());
        updateLastPlayers();

        timer = 10;
        lastDimension = mc.level.dimensionType();
    }

    @Override
    public void onDeactivate() {
        players.clear();
        lastPlayerList.clear();
    }

    private void updateLastPlayers() {
        lastPlayers.clear();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player player) lastPlayers.add(player);
        }
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (event.entity instanceof Player) {
            int toRemove = -1;

            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).uuid.equals(event.entity.getUUID())) {
                    toRemove = i;
                    break;
                }
            }

            if (toRemove != -1) {
                players.remove(toRemove);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.getConnection().getOnlinePlayers().size() != lastPlayerList.size()) {
            for (PlayerInfo entry : lastPlayerList) {
                if (mc.getConnection().getOnlinePlayers().stream().anyMatch(playerListEntry -> playerListEntry.getProfile().equals(entry.getProfile())))
                    continue;

                for (Player player : lastPlayers) {
                    if (player.getUUID().equals(entry.getProfile().id())) {
                        add(new Entry(player));
                    }
                }
            }

            lastPlayerList.clear();
            lastPlayerList.addAll(mc.getConnection().getOnlinePlayers());
            updateLastPlayers();
        }

        if (timer <= 0) {
            updateLastPlayers();
            timer = 10;
        } else {
            timer--;
        }

        DimensionType dimension = mc.level.dimensionType();
        if (dimension != lastDimension) players.clear();
        lastDimension = dimension;
    }

    private void add(Entry entry) {
        players.removeIf(player -> player.uuid.equals(entry.uuid));
        players.add(entry);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        for (Entry player : players) player.render3D(event);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        for (Entry player : players) player.render2D(event.graphics);
    }

    @Override
    public String getInfoString() {
        return Integer.toString(players.size());
    }

    private static final Vector3d pos = new Vector3d();

    private class Entry {
        public final double x, y, z;
        public final double xWidth, zWidth, halfWidth, height;

        public final UUID uuid;
        public final String name;
        public final int health, maxHealth;
        public final String healthText;

        public Entry(Player entity) {
            halfWidth = entity.getBbWidth() / 2;
            x = entity.getX() - halfWidth;
            y = entity.getY();
            z = entity.getZ() - halfWidth;

            xWidth = entity.getBoundingBox().getXsize();
            zWidth = entity.getBoundingBox().getZsize();
            height = entity.getBoundingBox().getYsize();

            uuid = entity.getUUID();
            name = entity.getName().getString();
            health = Math.round(entity.getHealth() + entity.getAbsorptionAmount());
            maxHealth = Math.round(entity.getMaxHealth() + entity.getAbsorptionAmount());

            healthText = " " + health;
        }

        public void render3D(Render3DEvent event) {
            if (fullHeight.get())
                event.renderer.box(x, y, z, x + xWidth, y + height, z + zWidth, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            else
                event.renderer.sideHorizontal(x, y, z, x + xWidth, z, sideColor.get(), lineColor.get(), shapeMode.get());
        }

        public void render2D(GuiGraphicsExtractor graphics) {
            if (!PlayerUtils.isWithinCamera(x, y, z, mc.options.renderDistance().get() * 16)) return;

            TextRenderer text = TextRenderer.get();
            double scale = LogoutSpots.this.scale.get();
            pos.set(x + halfWidth, y + height + 0.5, z + halfWidth);

            if (!NametagUtils.to2D(pos, scale)) return;

            NametagUtils.begin(pos, graphics);

            // Compute health things
            double healthPercentage = (double) health / maxHealth;

            // Get health color
            Color healthColor;
            if (healthPercentage <= 0.333) healthColor = RED;
            else if (healthPercentage <= 0.666) healthColor = ORANGE;
            else healthColor = GREEN;

            // Render background
            double i = text.getWidth(name) / 2.0 + text.getWidth(healthText) / 2.0;
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(-i, 0, i * 2, text.getHeight(), nameBackgroundColor.get());
            Renderer2D.COLOR.render();

            // Render name and health texts
            text.beginBig(graphics);
            double hX = text.render(name, -i, 0, nameColor.get());
            text.render(healthText, hX, 0, healthColor);
            text.end();

            NametagUtils.end(graphics);
        }
    }
}
