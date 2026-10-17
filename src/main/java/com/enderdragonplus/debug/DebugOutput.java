package com.enderdragonplus.debug;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DebugOutput {
    private DebugOutput() {}

    public static Path directory(String run) throws IOException {
        if (!run.matches("[a-zA-Z0-9_-]{1,80}")) {
            throw new IOException("Invalid debug run identifier");
        }
        String override = System.getProperty("edp.debug.output");
        Path root = override != null && !override.isBlank() ? Path.of(override)
                : Files.isDirectory(Path.of("D:/AI临时文件"))
                ? Path.of("D:/AI临时文件/EnderDragonPlus-debug") : Path.of("debug/enderdragonplus");
        Path directory = root.resolve(run).toAbsolutePath();
        Files.createDirectories(directory);
        return directory;
    }
}
