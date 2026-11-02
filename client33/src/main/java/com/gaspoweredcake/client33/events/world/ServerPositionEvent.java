package com.gaspoweredcake.client33.events.world;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** A server packet's absolute position, captured before client interpolation. */
public record ServerPositionEvent(Entity entity, Vec3 position, String source, long capturedNanos) {}
