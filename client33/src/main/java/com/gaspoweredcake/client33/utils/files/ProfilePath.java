package com.gaspoweredcake.client33.utils.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves one profile directory without allowing the profiles root or a symlink target. */
public final class ProfilePath {
    private ProfilePath() {}

    public static Path resolve(Path root, String name) throws IOException {
        if (name == null || name.isEmpty() || !name.matches("[A-Za-z0-9_ -]+"))
            throw new IOException("Invalid profile name");
        Path base = root.toFile().getCanonicalFile().toPath();
        Path candidate = base.resolve(name);
        if (Files.isSymbolicLink(candidate) || !candidate.toFile().getCanonicalFile().toPath().getParent().equals(base))
            throw new IOException("Profile must be a direct directory under the profiles folder");
        return candidate;
    }
}
