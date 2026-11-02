package com.gaspoweredcake.client33.utils.player;

import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;

import static com.gaspoweredcake.client33.Client33.mc;

/** Keeps the item-defined local animation and the 26.3 punch notification together. */
public final class SwingUtils {
    private SwingUtils() {}

    public static void swing(InteractionHand hand) {
        mc.player.swing(hand, mc.player.getItemInHand(hand).getAttackAnimation(), false);
        mc.player.connection.send(ServerboundPunchPacket.INSTANCE);
    }
}
