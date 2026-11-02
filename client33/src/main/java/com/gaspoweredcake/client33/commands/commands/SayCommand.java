
package com.gaspoweredcake.client33.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.gaspoweredcake.client33.commands.Command;
import com.gaspoweredcake.client33.mixin.ClientPacketListenerAccessor;
import com.gaspoweredcake.client33.utils.misc.Client33Starscript;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.util.Crypt;
import org.meteordev.starscript.Script;

import java.time.Instant;

public class SayCommand extends Command {
    public SayCommand() {
        super("say", "Sends messages in chat.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            String msg = context.getArgument("message", String.class);
            Script script = Client33Starscript.compile(msg);

            if (script != null) {
                String message = Client33Starscript.run(script);

                if (message != null) {
                    Instant instant = Instant.now();
                    long l = Crypt.SaltSupplier.getLong();
                    ClientPacketListener handler = mc.getConnection();
                    LastSeenMessagesTracker.Update lastSeenMessages = ((ClientPacketListenerAccessor) handler).client33$getLastSeenMessages().generateAndApplyUpdate();
                    MessageSignature messageSignatureData = ((ClientPacketListenerAccessor) handler).client33$getSignedMessageEncoder().pack(new SignedMessageBody(message, instant, l, lastSeenMessages.lastSeen()));
                    handler.send(new ServerboundChatPacket(message, instant, l, java.util.Optional.ofNullable(messageSignatureData), lastSeenMessages.update()));
                }
            }

            return SINGLE_SUCCESS;
        }));
    }
}
