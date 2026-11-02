
package com.gaspoweredcake.client33.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.gaspoweredcake.client33.commands.Command;
import com.gaspoweredcake.client33.renderer.Fonts;
import com.gaspoweredcake.client33.systems.Systems;
import com.gaspoweredcake.client33.systems.friends.Friend;
import com.gaspoweredcake.client33.systems.friends.Friends;
import com.gaspoweredcake.client33.utils.network.Capes;
import com.gaspoweredcake.client33.utils.network.Client33Executor;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class ReloadCommand extends Command {
    public ReloadCommand() {
        super("reload", "Reloads many systems.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
            warning("Reloading systems, this may take a while.");

            Systems.load();
            Capes.init();
            Fonts.refresh();
            Client33Executor.execute(() -> Friends.get().forEach(Friend::updateInfo));

            return SINGLE_SUCCESS;
        });
    }
}
