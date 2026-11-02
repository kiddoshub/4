
package com.gaspoweredcake.client33.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.commands.Command;
import com.gaspoweredcake.client33.commands.arguments.PlayerArgumentType;
import com.gaspoweredcake.client33.events.client33.KeyInputEvent;
import com.gaspoweredcake.client33.events.client33.MouseClickEvent;
import com.gaspoweredcake.client33.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;

public class SpectateCommand extends Command {

    private final StaticListener shiftListener = new StaticListener();

    public SpectateCommand() {
        super("spectate", "Allows you to spectate nearby players");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(literal("reset").executes(_ -> {
            mc.setCameraEntity(mc.player);
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("player", PlayerArgumentType.create()).executes(context -> {
            mc.setCameraEntity(PlayerArgumentType.get(context));
            mc.player.sendSystemMessage(Component.literal("Sneak to un-spectate."));
            Client33.EVENT_BUS.subscribe(shiftListener);
            return SINGLE_SUCCESS;
        }));
    }

    private static class StaticListener {
        @EventHandler
        private void onKey(KeyInputEvent event) {
            if (Input.isPressed(mc.options.keyShift)) {
                mc.setCameraEntity(mc.player);
                event.cancel();
                Client33.EVENT_BUS.unsubscribe(this);
            }
        }

        @EventHandler
        private void onMouse(MouseClickEvent event) {
            if (Input.isPressed(mc.options.keyShift)) {
                mc.setCameraEntity(mc.player);
                event.cancel();
                Client33.EVENT_BUS.unsubscribe(this);
            }
        }
    }
}
