
package com.gaspoweredcake.client33.systems.modules.world;

import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.mixin.BlockHitResultAccessor;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;

public class BuildHeight extends Module {
    public BuildHeight() {
        super(Categories.World, "build-height", "Allows you to interact with objects at the build limit.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundUseItemOnPacket p)) return;
        if (mc.level == null) return;
        if (p.hitResult().getLocation().y >= mc.level.getHeight() && p.hitResult().getDirection() == Direction.UP) {
            ((BlockHitResultAccessor) p.hitResult()).client33$setDirection(Direction.DOWN);
        }
    }
}
