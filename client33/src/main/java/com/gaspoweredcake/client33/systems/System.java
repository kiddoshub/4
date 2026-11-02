
package com.gaspoweredcake.client33.systems;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.utils.files.AtomicFile;
import com.gaspoweredcake.client33.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public abstract class System<T> implements ISerializable<T> {
    private final String name;
    private final File file;

    protected boolean isFirstInit;
    private final Set<Path> unrecoveredFiles = new HashSet<>();

    public System(String name) {
        this.name = name;

        file = name == null ? null : new File(Client33.FOLDER, name + ".nbt");
        isFirstInit = file != null && !file.exists();
    }

    public void init() {
    }

    public synchronized void save(File folder) {
        File target = resolveFile(folder);
        if (target == null) return;
        if (unrecoveredFiles.contains(target.toPath().toAbsolutePath().normalize())) {
            Client33.LOG.error("Skipping save of unreadable settings at '{}': no recovery copy could be made.", target);
            return;
        }
        CompoundTag tag = toTag();
        if (tag == null) return;
        try {
            AtomicFile.write(target.toPath(), staged -> NbtIo.write(tag, staged));
        } catch (IOException e) {
            Client33.LOG.error("Could not save system '{}' to '{}'.", name, target, e);
        }
    }

    private File resolveFile(File folder) {
        File source = getFile();
        return source == null || folder == null ? source : new File(folder, source.getName());
    }

    public void save() {
        save(null);
    }

    public synchronized void load(File folder) {
        File source = resolveFile(folder);
        if (source == null || !source.exists()) return;
        try {
            CompoundTag tag = NbtIo.read(source.toPath());
            if (tag == null) throw new IOException("Missing compound root");
            fromTag(tag);
            unrecoveredFiles.remove(source.toPath().toAbsolutePath().normalize());
        } catch (IOException | RuntimeException failure) {
            Client33.LOG.error("Could not load system '{}' from '{}'.", name, source, failure);
            // Preserve the exact input for recovery before a later save can replace it.
            try {
                var backup = Files.createTempFile(source.toPath().toAbsolutePath().getParent(), source.getName() + "-", ".backup.nbt");
                try {
                    Files.copy(source.toPath(), backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException backupFailure) {
                    try { Files.deleteIfExists(backup); } catch (IOException cleanup) { backupFailure.addSuppressed(cleanup); }
                    throw backupFailure;
                }
                unrecoveredFiles.remove(source.toPath().toAbsolutePath().normalize());
                Client33.LOG.info("Preserved unreadable settings in '{}'.", backup);
            } catch (IOException backupFailure) {
                unrecoveredFiles.add(source.toPath().toAbsolutePath().normalize());
                Client33.LOG.error("Could not back up system '{}'; original input remains at '{}'.", name, source, backupFailure);
            }
        }
    }

    public void load() {
        load(null);
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    @Override
    public CompoundTag toTag() {
        return null;
    }

    @Override
    public T fromTag(CompoundTag tag) {
        return null;
    }
}
