
package com.gaspoweredcake.client33.gui.tabs.builtin;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.WindowScreen;
import com.gaspoweredcake.client33.gui.renderer.GuiRenderer;
import com.gaspoweredcake.client33.gui.tabs.Tab;
import com.gaspoweredcake.client33.gui.tabs.TabScreen;
import com.gaspoweredcake.client33.gui.tabs.WindowTabScreen;
import com.gaspoweredcake.client33.gui.widgets.containers.WContainer;
import com.gaspoweredcake.client33.gui.widgets.containers.WHorizontalList;
import com.gaspoweredcake.client33.gui.widgets.containers.WTable;
import com.gaspoweredcake.client33.gui.widgets.pressable.WButton;
import com.gaspoweredcake.client33.gui.widgets.pressable.WCheckbox;
import com.gaspoweredcake.client33.gui.widgets.pressable.WConfirmedButton;
import com.gaspoweredcake.client33.gui.widgets.pressable.WConfirmedMinus;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.systems.profiles.Profile;
import com.gaspoweredcake.client33.systems.profiles.Profiles;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.misc.NbtUtils;
import com.gaspoweredcake.client33.utils.render.prompts.OkPrompt;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import com.gaspoweredcake.client33.utils.files.AtomicFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.gaspoweredcake.client33.Client33.mc;

public class ProfilesTab extends Tab {
    private static final PointerBuffer filters;

    static {
        filters = BufferUtils.createPointerBuffer(1);

        ByteBuffer pngFilter = MemoryUtil.memASCII("*.nbt");

        filters.put(pngFilter);
        filters.rewind();
    }

    public ProfilesTab() {
        super("Profiles");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new ProfilesScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof ProfilesScreen;
    }

    private static class ProfilesScreen extends WindowTabScreen {
        public ProfilesScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            WTable table = add(theme.table()).expandX().minWidth(400).widget();
            initTable(table);

            add(theme.horizontalSeparator()).expandX();

            WHorizontalList l = add(theme.horizontalList()).expandX().widget();

            // Create
            WButton create = l.add(theme.button("Create")).expandX().widget();
            create.tooltip = "Create new profile";
            create.action = () -> mc.gui.setScreen(new EditProfileScreen(theme, null, this::reload));

            // Import
            WButton importBtn = l.add(theme.button("Import")).expandX().widget();
            importBtn.tooltip = "Import profile";
            importBtn.action = () -> {
                try {
                    Profile imported = importProfile();
                    if (imported != null)
                        Client33.LOG.info("Successfully imported profile '{}'.", imported.name.get());
                    reload();
                } catch (Exception e) {
                    Client33.LOG.error("Error importing profile", e);
                    OkPrompt.create()
                        .title("Failure importing profile")
                        .message("There was an error importing the profile.")
                        .message("Error: %s", e.getMessage())
                        .dontShowAgainCheckboxVisible(false)
                        .show();
                }
            };
        }

        private void initTable(WTable table) {
            table.clear();
            if (Profiles.get().isEmpty()) return;

            for (Profile profile : Profiles.get()) {
                table.add(theme.label(profile.name.get())).expandCellX();

                WConfirmedButton save = theme.confirmedButton("Save", "Confirm");
                save.action = profile::save;
                table.add(save).right();

                WButton load = table.add(theme.button("Load")).widget();
                load.action = profile::load;

                WButton export = table.add(theme.button("Export")).widget();
                export.action = () -> mc.gui.setScreen(new ExportProfileScreen(theme, profile));

                WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
                edit.action = () -> mc.gui.setScreen(new EditProfileScreen(theme, profile, this::reload));

                WConfirmedMinus remove = table.add(theme.confirmedMinus()).widget();
                remove.action = () -> {
                    Profiles.get().remove(profile);
                    reload();
                };

                table.row();
            }
        }

        private Profile importProfile() throws IOException {
            String file = TinyFileDialogs.tinyfd_openFileDialog("Select profile to import", null, filters, null, false);
            if (file == null) return null;
            File profileFile = new File(file);

            CompoundTag nbt = NbtIo.read(profileFile.toPath());
            if (nbt == null) return null;

            Profile p = new Profile();
            Optional<String> parsedName = nbt.getString("name").filter(n -> !n.isEmpty());

            if (parsedName.filter(p.name::set).isEmpty() && !p.name.set(FilenameUtils.removeExtension(profileFile.getName()))) {
                throw new IllegalStateException("Imported profile does not have a valid name.");
            }

            File profileFolder = p.getFile().getCanonicalFile();
            if (!profileFolder.getParentFile().equals(Profiles.FOLDER.getCanonicalFile())) {
                throw new IllegalStateException("Imported profile does not have a valid location.");
            }

            //noinspection ResultOfMethodCallIgnored
            profileFolder.mkdirs();
            nbt.remove("name");

            boolean valid = false;
            for (var entry : nbt.entrySet()) {
                String filename = entry.getKey();
                if (!filename.endsWith(".nbt")) continue;
                if (filename.contains("/") || filename.contains("\\") || new File(filename).isAbsolute()) continue;

                switch (filename) {
                    case "hud.nbt" -> p.hud.set(true);
                    case "macros.nbt" -> p.macros.set(true);
                    case "modules.nbt" -> p.modules.set(true);
                    default -> {
                        if (filename.endsWith(".nbt")) p.waypoints.set(true);
                    }
                }

                File f = new File(profileFolder, filename).getCanonicalFile();
                if (!f.toPath().startsWith(profileFolder.toPath())) continue;

                valid = true;
                AtomicFile.write(f.toPath(), staged -> {
                    try (DataOutputStream output = new DataOutputStream(new FileOutputStream(staged.toFile()))) {
                        NbtIo.writeUnnamedTagWithFallback(entry.getValue(), output);
                    }
                });
            }

            if (!valid) {
                throw new IllegalStateException("Imported file is not a profile.");
            }

            Profiles.get().getAll().add(p);
            Profiles.get().save();

            return p;
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(Profiles.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(Profiles.get());
        }
    }

    private static class EditProfileScreen extends WindowScreen {
        private WContainer settingsContainer;
        private final Profile profile;
        private final boolean isNew;
        private final Runnable action;

        public EditProfileScreen(GuiTheme theme, Profile profile, Runnable action) {
            super(theme, profile == null ? "New Profile" : "Edit Profile");

            this.isNew = profile == null;
            this.profile = isNew ? new Profile() : profile;
            this.action = action;
        }

        @Override
        public void initWidgets() {
            settingsContainer = add(theme.verticalList()).expandX().minWidth(400).widget();
            settingsContainer.add(theme.settings(profile.settings)).expandX();

            add(theme.horizontalSeparator()).expandX();

            WButton save = add(theme.button(isNew ? "Create" : "Save")).expandX().widget();
            save.action = () -> {
                if (profile.name.get().isEmpty()) return;

                if (isNew) {
                    for (Profile p : Profiles.get()) {
                        if (profile.equals(p)) return;
                    }
                }

                List<String> valid = new ArrayList<>();
                for (String address : profile.loadOnJoin.get()) {
                    if (Utils.resolveAddress(address)) valid.add(address);
                }

                profile.loadOnJoin.set(valid);

                if (isNew) Profiles.get().add(profile);
                else Profiles.get().save();

                onClose();
            };

            enterAction = save.action;
        }

        @Override
        public void tick() {
            super.tick();

            profile.settings.tick(settingsContainer, theme);
        }

        @Override
        protected void onClosed() {
            if (action != null) action.run();
        }
    }

    private static class ExportProfileScreen extends WindowScreen {
        private final Profile profile;

        public ExportProfileScreen(GuiTheme theme, Profile profile) {
            super(theme, "Export Profile");
            this.profile = profile;
        }

        @Override
        public void initWidgets() {
            add(theme.label("Select which profile settings to export."));

            WContainer settingsContainer = add(theme.verticalList()).expandX().minWidth(400).widget();

            settingsContainer.add(theme.horizontalSeparator()).expandX().widget();

            WCheckbox hud = addBool(settingsContainer, profile.settings.get("hud", Boolean.class));
            WCheckbox macros = addBool(settingsContainer, profile.settings.get("macros", Boolean.class));
            WCheckbox modules = addBool(settingsContainer, profile.settings.get("modules", Boolean.class));
            WCheckbox waypoints = addBool(settingsContainer, profile.settings.get("waypoints", Boolean.class));

            add(theme.horizontalSeparator()).expandX().widget();

            WButton export = add(theme.button("Export profile")).expandX().widget();
            export.action = () -> {
                exportProfile(profile, hud.checked, macros.checked, modules.checked, waypoints.checked);
                onClose();
            };
        }

        private WCheckbox addBool(WContainer container, Setting<Boolean> setting) {
            WHorizontalList boolList = container.add(theme.horizontalList()).expandX().widget();
            boolList.add(theme.label(setting.title)).widget().tooltip = setting.description;

            WCheckbox c = theme.checkbox(setting.get());
            boolList.add(c).expandCellX().right();

            return c;
        }

        private void exportProfile(Profile profile, boolean hud, boolean macros, boolean modules, boolean waypoints) {
            String path = TinyFileDialogs.tinyfd_saveFileDialog("Save profile", profile.name.get(), filters, null);
            if (path == null) return;
            Path p = Path.of(path.endsWith(".nbt") ? path : path + ".nbt");

            CompoundTag nbt = new CompoundTag();
            nbt.putString("name", profile.name.get());

            try {
                File[] files = profile.getFile().listFiles();
                if (files == null) throw new IOException("Cannot read profile directory.");
                for (File f : files) {
                    if (f.getName().equals("hud.nbt") && hud ||
                        f.getName().equals("macros.nbt") && macros ||
                        f.getName().equals("modules.nbt") && modules
                    ) {
                        nbt.put(f.getName(), NbtIo.read(f.toPath()));
                    } else if (f.getName().endsWith(".nbt") && waypoints)
                        nbt.put(f.getName(), NbtIo.read(f.toPath()));
                }

                AtomicFile.write(p, staged -> NbtIo.write(nbt, staged));
            } catch (IOException | IllegalStateException e) {
                Client33.LOG.error("Error serialising profile {} to a file", profile.name.get(), e);
                OkPrompt.create()
                    .title("Failure exporting profile")
                    .message("There was an error serialising or exporting the profile %s.", profile.name.get())
                    .message("Error: %s", e.getMessage())
                    .dontShowAgainCheckboxVisible(false)
                    .show();
            }
        }
    }
}
