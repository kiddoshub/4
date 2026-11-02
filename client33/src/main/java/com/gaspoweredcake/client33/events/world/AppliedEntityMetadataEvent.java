package com.gaspoweredcake.client33.events.world;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;

public record AppliedEntityMetadataEvent(ClientboundSetEntityDataPacket packet) {}
