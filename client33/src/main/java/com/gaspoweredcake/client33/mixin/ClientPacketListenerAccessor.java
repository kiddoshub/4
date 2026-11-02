
package com.gaspoweredcake.client33.mixin;

import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {
    @Accessor("serverChunkRadius")
    int client33$getServerChunkRadius();

    @Accessor("signedMessageEncoder")
    SignedMessageChain.Encoder client33$getSignedMessageEncoder();

    @Accessor("lastSeenMessages")
    LastSeenMessagesTracker client33$getLastSeenMessages();

    @Accessor("registryAccess")
    RegistryAccess.Frozen client33$getRegistryAccess();

    @Accessor("enabledFeatures")
    FeatureFlagSet client33$getEnabledFeatures();

    @Accessor("COMMAND_NODE_BUILDER")
    static ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider> client33$getCommandNodeFactory() {
        return null;
    }
}
