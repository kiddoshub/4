package com.gaspoweredcake.client33.events.world;

import net.minecraft.network.protocol.Packet;

/** Block updates after application on the client thread. */
public record AppliedBlockUpdateEvent(Packet<?> packet) {}
