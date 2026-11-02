package com.gaspoweredcake.client33.utils.files;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Writes a complete sibling file before replacing the destination. */
public final class AtomicFile {
    private AtomicFile() {}

    @FunctionalInterface
    public interface Writer {
        void write(Path stagingFile) throws IOException;
    }

    public static void write(Path destination, Writer writer) throws IOException {
        Path target = destination.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        try (Staging staging = new Staging(Files.createTempFile(target.getParent(), "." + target.getFileName() + "-", ".tmp"))) {
            writer.write(staging.path);
            try (FileChannel channel = FileChannel.open(staging.path, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            try {
                Files.move(staging.path, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                // A complete sibling file still avoids streaming partial data into the live file.
                // Filesystems without atomic rename cannot guarantee crash-safe replacement.
                Files.move(staging.path, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private record Staging(Path path) implements AutoCloseable {
        @Override public void close() throws IOException { Files.deleteIfExists(path); }
    }
}
