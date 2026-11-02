package com.gaspoweredcake.client33.systems.modules.combat;

import com.gaspoweredcake.client33.events.render.Render2DEvent;
import com.gaspoweredcake.client33.renderer.text.TextRenderer;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.utils.combat.CombatTelemetry;
import com.gaspoweredcake.client33.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

public class ShieldPredict extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Boolean> integrate = sg.add(new BoolSetting.Builder().name("aura-integration")
        .description("Let KillAura's shield rules use estimated raise timing and facing.").defaultValue(true).build());
    private final Setting<Integer> lead = sg.add(new IntSetting.Builder().name("max-lead-ms")
        .description("Cap half-ping prediction of the remote raise animation. Equipment alone never counts as blocking.")
        .defaultValue(150).range(0, 500).sliderRange(0, 500).build());
    private final Setting<Boolean> hud = sg.add(new BoolSetting.Builder().name("target-readout")
        .description("Show observed hands and estimated blocking state for your target.").defaultValue(true).build());

    public ShieldPredict() { super(Categories.Combat, "shield-predict", "Estimate shield raise timing from received item-use state, blocking components and latency."); }

    public boolean blocks(LivingEntity entity) {
        if (!isActive() || !integrate.get()) return entity.isBlocking();
        if (!entity.isUsingItem()) return false;
        var component = entity.getUseItem().get(DataComponents.BLOCKS_ATTACKS);
        if (component == null) return false;
        double towardX = mc.player.getX() - entity.getX(), towardZ = mc.player.getZ() - entity.getZ();
        var view = entity.getViewVector(1);
        if (towardX * view.x + towardZ * view.z <= 0) return false;
        return com.gaspoweredcake.client33.utils.combat.ShieldEstimate.ready(entity.getTicksUsingItem(), component.blockDelayTicks(),
            CombatTelemetry.ping(), lead.get(), CombatTelemetry.CLOCK.tickMillis());
    }

    @EventHandler private void onRender(Render2DEvent event) {
        if (!hud.get() || mc.player == null) return;
        KillAura aura = Modules.get().get(KillAura.class);
        Entity target = aura.isActive() ? aura.getTarget() : null;
        if (target == null && mc.hitResult instanceof EntityHitResult hit) target = hit.getEntity();
        if (!(target instanceof LivingEntity living)) return;
        String state = blocks(living) ? "blocking (estimated)" : living.isUsingItem() ? "using/raising " + living.getUsedItemHand() : "idle";
        TextRenderer text = TextRenderer.get();
        text.begin(event.graphics, 0.8);
        text.render("Shield / " + state, 12, 170, new Color(92, 220, 255), true);
        text.render("Main: " + living.getMainHandItem().getHoverName().getString() + "  |  Offhand: "
            + living.getOffhandItem().getHoverName().getString(), 12, 190, Color.WHITE, true);
        text.end();
    }
}
