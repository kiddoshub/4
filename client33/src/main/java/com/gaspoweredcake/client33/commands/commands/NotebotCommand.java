
package com.gaspoweredcake.client33.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.commands.Command;
import com.gaspoweredcake.client33.commands.arguments.NotebotSongArgumentType;
import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.misc.Notebot;
import com.gaspoweredcake.client33.utils.notebot.song.Note;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NotebotCommand extends Command {
    private static final SimpleCommandExceptionType INVALID_SONG = new SimpleCommandExceptionType(Component.literal("Invalid song."));
    private static final DynamicCommandExceptionType INVALID_PATH = new DynamicCommandExceptionType(object -> Component.literal("'%s' is not a valid path.".formatted(object)));

    private int ticks = -1;
    private final Int2ObjectMap<List<Note>> song = new Int2ObjectOpenHashMap<>(); // tick -> notes

    public NotebotCommand() {
        super("notebot", "Allows you load notebot files");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(literal("help").executes(_ -> {
            com.mojang.blaze3d.Blaze3D.openUri(java.net.URI.create("https://github.com/GasPoweredCake/33"));
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("status").executes(_ -> {
            Notebot notebot = Modules.get().get(Notebot.class);
            info(notebot.getStatus());
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("pause").executes(_ -> {
            Notebot notebot = Modules.get().get(Notebot.class);
            notebot.pause();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("resume").executes(_ -> {
            Notebot notebot = Modules.get().get(Notebot.class);
            notebot.pause();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("stop").executes(_ -> {
            Notebot notebot = Modules.get().get(Notebot.class);
            notebot.stop();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("randomsong").executes(_ -> {
            Notebot notebot = Modules.get().get(Notebot.class);
            notebot.playRandomSong();
            return SINGLE_SUCCESS;
        }));

        builder.then(
            literal("play").then(
                argument("song", NotebotSongArgumentType.create()).executes(ctx -> {
                    Notebot notebot = Modules.get().get(Notebot.class);
                    Path songPath = ctx.getArgument("song", Path.class);
                    if (songPath == null || !Files.exists(songPath)) {
                        throw INVALID_SONG.create();
                    }
                    notebot.loadSong(songPath.toFile());
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(
            literal("preview").then(
                argument("song", NotebotSongArgumentType.create()).executes(ctx -> {
                    Notebot notebot = Modules.get().get(Notebot.class);
                    Path songPath = ctx.getArgument("song", Path.class);
                    if (songPath == null || !Files.exists(songPath)) {
                        throw INVALID_SONG.create();
                    }
                    notebot.previewSong(songPath.toFile());
                    return SINGLE_SUCCESS;
                })));

        builder.then(literal("record").then(literal("start").executes(_ -> {
            ticks = -1;
            song.clear();
            Client33.EVENT_BUS.subscribe(this);
            info("Recording started");
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("record").then(literal("cancel").executes(_ -> {
            Client33.EVENT_BUS.unsubscribe(this);
            info("Recording cancelled");
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("record").then(literal("save").then(argument("name", StringArgumentType.greedyString()).executes(ctx -> {
            String name = ctx.getArgument("name", String.class);
            if (name == null || name.isEmpty()) {
                throw INVALID_PATH.create(name);
            }
            Path notebotFolder = Client33.FOLDER.toPath().resolve("notebot");
            Path path = notebotFolder.resolve(String.format("%s.txt", name)).normalize();
            if (!path.startsWith(notebotFolder)) {
                throw INVALID_PATH.create(path);
            }
            saveRecording(path);
            return SINGLE_SUCCESS;
        }))));
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (ticks == -1) return;
        ticks++;
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundSoundPacket sound && sound.getSound().value().location().getPath().contains("note_block")) {
            if (ticks == -1) ticks = 0;
            List<Note> notes = song.computeIfAbsent(ticks, _ -> new ArrayList<>());
            var note = getNote(sound);
            if (note != null) {
                notes.add(note);
            }
        }
    }

    private void saveRecording(Path path) {
        if (song.isEmpty()) {
            Client33.EVENT_BUS.unsubscribe(this);
            return;
        }
        Client33.EVENT_BUS.unsubscribe(this);
        try (var file = java.nio.file.Files.newBufferedWriter(path, java.nio.charset.StandardCharsets.UTF_8)) {
            for (var entry : song.int2ObjectEntrySet()) {
                int tick = entry.getIntKey();
                List<Note> notes = entry.getValue();

                for (var note : notes) {
                    NoteBlockInstrument instrument = note.getInstrument();
                    int noteLevel = note.getNoteLevel();

                    file.write(String.format("%d:%d:%d\n", tick, noteLevel, instrument.ordinal()));
                }
            }

            info("Song saved.");
        } catch (IOException _) {
            info("Couldn't create the file.");
            Client33.EVENT_BUS.unsubscribe(this);
        }

    }

    private Note getNote(ClientboundSoundPacket soundPacket) {
        float pitch = soundPacket.getPitch();

        // Bruteforce note level
        int noteLevel = -1;
        for (int n = 0; n < 25; n++) {
            if ((float) Math.pow(2.0D, (n - 12) / 12.0D) - 0.01 < pitch &&
                (float) Math.pow(2.0D, (n - 12) / 12.0D) + 0.01 > pitch) {
                noteLevel = n;
                break;
            }
        }

        if (noteLevel == -1) {
            error("Error while bruteforcing a note level! Sound: " + soundPacket.getSound().value() + " Pitch: " + pitch);
            return null;
        }

        NoteBlockInstrument instrument = getInstrumentFromSound(soundPacket.getSound().value());
        if (instrument == null) {
            error("Can't find the instrument from sound! Sound: " + soundPacket.getSound().value());
            return null;
        }

        return new Note(instrument, noteLevel);
    }

    private NoteBlockInstrument getInstrumentFromSound(SoundEvent sound) {
        String path = sound.location().getPath();
        if (path.contains("harp"))
            return NoteBlockInstrument.HARP;
        else if (path.contains("basedrum"))
            return NoteBlockInstrument.BASEDRUM;
        else if (path.contains("snare"))
            return NoteBlockInstrument.SNARE;
        else if (path.contains("hat"))
            return NoteBlockInstrument.HAT;
        else if (path.contains("bass"))
            return NoteBlockInstrument.BASS;
        else if (path.contains("flute"))
            return NoteBlockInstrument.FLUTE;
        else if (path.contains("bell"))
            return NoteBlockInstrument.BELL;
        else if (path.contains("guitar"))
            return NoteBlockInstrument.GUITAR;
        else if (path.contains("chime"))
            return NoteBlockInstrument.CHIME;
        else if (path.contains("xylophone"))
            return NoteBlockInstrument.XYLOPHONE;
        else if (path.contains("iron_xylophone"))
            return NoteBlockInstrument.IRON_XYLOPHONE;
        else if (path.contains("cow_bell"))
            return NoteBlockInstrument.COW_BELL;
        else if (path.contains("didgeridoo"))
            return NoteBlockInstrument.DIDGERIDOO;
        else if (path.contains("bit"))
            return NoteBlockInstrument.BIT;
        else if (path.contains("banjo"))
            return NoteBlockInstrument.BANJO;
        else if (path.contains("pling"))
            return NoteBlockInstrument.PLING;
        return null;
    }
}
