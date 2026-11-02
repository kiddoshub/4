
package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.commands.*;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Commands {
    public static final List<Command> COMMANDS = new ArrayList<>();
    public static CommandDispatcher<CommandSource> DISPATCHER = new CommandDispatcher<>();

    @PostInit(dependencies = PathManagers.class)
    public static void init() {
        add(new VClipCommand());
        add(new HClipCommand());
        add(new DismountCommand());
        add(new DisconnectCommand());
        add(new DamageCommand());
        add(new DropCommand());
        add(new EnchantCommand());
        add(new FakePlayerCommand());
        add(new FriendsCommand());
        add(new CommandsCommand());
        add(new InventoryCommand());
        add(new NbtCommand());
        add(new NotebotCommand());
        add(new PeekCommand());
        add(new EnderChestCommand());
        add(new ProfilesCommand());
        add(new ReloadCommand());
        add(new ResetCommand());
        add(new SayCommand());
        add(new ServerCommand());
        add(new ToggleCommand());
        add(new SettingCommand());
        add(new SpectateCommand());
        add(new GamemodeCommand());
        add(new SaveMapCommand());
        add(new MacroCommand());
        add(new ModulesCommand());
        add(new BindsCommand());
        add(new GiveCommand());
        add(new NameHistoryCommand());
        add(new BindCommand());
        add(new FovCommand());
        add(new RotationCommand());
        add(new WaypointCommand());
        add(new InputCommand());
        add(new WaspCommand());
        add(new LocateCommand());

        COMMANDS.sort(Comparator.comparing(Command::getName));

        MeteorClient.EVENT_BUS.subscribe(Commands.class);
    }

    public static void add(Command command) {
        COMMANDS.removeIf(existing -> existing.getName().equals(command.getName()));
        COMMANDS.add(command);
    }

    public static void dispatch(String message) throws CommandSyntaxException {
        DISPATCHER.execute(message, mc.getNetworkHandler().getCommandSource());
    }

    public static Command get(String name) {
        for (Command command : COMMANDS) {
            if (command.getName().equals(name)) {
                return command;
            }
        }

        return null;
    }

    @EventHandler
    private static void onJoin(GameJoinedEvent event) {
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        Command.REGISTRY_ACCESS = CommandRegistryAccess.of(networkHandler.getRegistryManager(), networkHandler.getEnabledFeatures());

        DISPATCHER = new CommandDispatcher<>();
        for (Command command : COMMANDS) {
            command.registerTo(DISPATCHER);
        }
    }
}
