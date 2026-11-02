package com.gaspoweredcake.client33.events.packets;
import net.minecraft.network.protocol.Packet;
/** Raw server inventory packet after dispatch to the client thread, regardless of screen state. */
public record SlotPacketObservedEvent(Packet<?> packet) {}
