package com.gaspoweredcake.client33.events.packets;

import net.minecraft.network.protocol.Packet;

/** A chunk, chunk unload, or block-entity update applied on the client thread. */
public record AppliedSpawnerDataEvent(Packet<?> packet) {}
