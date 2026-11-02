
package com.gaspoweredcake.client33.gui.screens;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.WindowScreen;
import com.gaspoweredcake.client33.gui.widgets.containers.WTable;
import com.gaspoweredcake.client33.gui.widgets.input.WTextBox;
import com.gaspoweredcake.client33.gui.widgets.pressable.WButton;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.misc.Notebot;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.notebot.decoder.SongDecoders;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotebotSongsScreen extends WindowScreen {
    private static final Notebot notebot = Modules.get().get(Notebot.class);

    private WTextBox filter;
    private String filterText = "";

    private WTable table;

    public NotebotSongsScreen(GuiTheme theme) {
        super(theme, "Notebot Songs");
    }

    @Override
    public void initWidgets() {
        // Random Song
        WButton randomSong = add(theme.button("Random Song")).minWidth(400).expandX().widget();
        randomSong.action = notebot::playRandomSong;

        // Filter
        filter = add(theme.textBox("", "Search for the songs...")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            table.clear();
            initSongsTable();
        };

        table = add(theme.table()).widget();

        initSongsTable();
    }

    private void initSongsTable() {
        AtomicBoolean noSongsFound = new AtomicBoolean(true);
        try (var songs = Files.list(Client33.FOLDER.toPath().resolve("notebot"))) {
            songs.filter(Files::isRegularFile).sorted().forEach(path -> {
                if (SongDecoders.hasDecoder(path)) {
                    String name = path.getFileName().toString();

                    if (Utils.searchTextDefault(name, filterText, false)) {
                        addPath(path);
                        noSongsFound.set(false);
                    }
                }
            });
        } catch (IOException _) {
            table.add(theme.label("Missing client33/notebot folder.")).expandCellX();
            table.row();
        }

        if (noSongsFound.get()) {
            table.add(theme.label("No songs found.")).expandCellX().center();
        }
    }

    private void addPath(Path path) {
        table.add(theme.horizontalSeparator()).expandX().minWidth(400);
        table.row();

        table.add(theme.label(FilenameUtils.getBaseName(path.getFileName().toString()))).expandCellX();
        WButton load = table.add(theme.button("Load")).right().widget();
        load.action = () -> notebot.loadSong(path.toFile());
        WButton preview = table.add(theme.button("Preview")).right().widget();
        preview.action = () -> notebot.previewSong(path.toFile());

        table.row();
    }
}
