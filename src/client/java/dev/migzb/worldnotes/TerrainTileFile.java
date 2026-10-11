package dev.migzb.worldnotes;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

/** Versioned, bounded terrain records. Coordinates are supplied by the filename. */
final class TerrainTileFile {
    static final int SAMPLES = 16;
    static int[] read(Path file) throws IOException {
        if (Files.size(file) > 1024) throw new IOException("Oversized terrain tile");
        try (var input = new DataInputStream(new GZIPInputStream(Files.newInputStream(file)))) {
            if (input.readInt() != 0x574E5431) throw new IOException("Unknown terrain format");
            int[] colors = new int[SAMPLES];
            for (int i = 0; i < colors.length; i++) colors[i] = input.readInt();
            if (input.read() != -1) throw new IOException("Trailing terrain data");
            return colors;
        }
    }
    static void write(Path file, int[] colors) throws IOException {
        if (colors.length != SAMPLES) throw new IllegalArgumentException("Expected 16 colors");
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "terrain-", ".tmp");
        try {
            try (var output = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(temporary)))) {
                output.writeInt(0x574E5431);
                for (int color : colors) output.writeInt(color);
            }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
