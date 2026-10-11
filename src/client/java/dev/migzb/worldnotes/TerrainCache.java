package dev.migzb.worldnotes;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.LoggerFactory;

final class TerrainCache {
    static final int UNKNOWN = 0xFF202630;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "worldnotes-terrain-cache"); thread.setDaemon(true); return thread;
    });
    private final Path directory;
    private final Map<Long, int[]> tiles = new ConcurrentHashMap<>();
    private final Set<Long> checked = ConcurrentHashMap.newKeySet(), pending = ConcurrentHashMap.newKeySet();
    private final java.util.concurrent.atomic.AtomicLong revision = new java.util.concurrent.atomic.AtomicLong();
    long revision() { return revision.get(); }
    TerrainCache(Path root, String context) {
        try { directory = root.resolve(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(context.getBytes(StandardCharsets.UTF_8)))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private static long key(int x, int z) { return ((long)x << 32) | (z & 0xFFFFFFFFL); }
    private Path file(int x, int z) { return directory.resolve(x + "_" + z + ".wnt"); }
    boolean has(int x, int z) { return tiles.containsKey(key(x, z)); }
    boolean ready(int x, int z) { return checked.contains(key(x, z)) && !pending.contains(key(x, z)); }
    CompletableFuture<Void> request(int x, int z) {
        long key = key(x, z);
        if (checked.contains(key) || !pending.add(key)) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> {
            try { if (Files.exists(file(x, z)) && tiles.putIfAbsent(key, TerrainTileFile.read(file(x, z))) == null) revision.incrementAndGet(); }
            catch (Exception exception) { LoggerFactory.getLogger("worldnotes").warn("Discarding unreadable terrain tile {}, {}", x, z, exception); }
            finally { checked.add(key); pending.remove(key); }
        }, IO);
    }
    CompletableFuture<Void> store(int x, int z, int[] colors) {
        if (colors.length != TerrainTileFile.SAMPLES) throw new IllegalArgumentException("Expected 256 block colors");
        int[] snapshot = colors.clone(); tiles.put(key(x,z), snapshot); checked.add(key(x,z));
        revision.incrementAndGet();
        return CompletableFuture.runAsync(() -> {
            try { TerrainTileFile.write(file(x,z), snapshot); }
            catch (Exception exception) { LoggerFactory.getLogger("worldnotes").warn("Unable to save terrain tile", exception); }
        }, IO);
    }
    int color(int x, int z) {
        int[] colors = tiles.get(key(Math.floorDiv(x,16), Math.floorDiv(z,16)));
        return colors == null ? UNKNOWN : colors[Math.floorMod(z,16)*16 + Math.floorMod(x,16)];
    }
    /** Copies complete block-resolution rows without a hash lookup for every pixel. */
    void copyRegion(int originX, int originZ, int width, int[] output) {
        if (output.length != width * width) throw new IllegalArgumentException("Region size mismatch");
        Arrays.fill(output, UNKNOWN);
        for (int cz = Math.floorDiv(originZ,16); cz <= Math.floorDiv(originZ+width-1,16); cz++) {
            for (int cx = Math.floorDiv(originX,16); cx <= Math.floorDiv(originX+width-1,16); cx++) {
                int[] colors = tiles.get(key(cx,cz));
                if (colors == null) continue;
                int startX = Math.max(originX,cx*16), endX = Math.min(originX+width,cx*16+16);
                int startZ = Math.max(originZ,cz*16), endZ = Math.min(originZ+width,cz*16+16);
                for (int z = startZ; z < endZ; z++)
                    System.arraycopy(colors,(z-cz*16)*16+startX-cx*16,output,(z-originZ)*width+startX-originX,endX-startX);
            }
        }
    }
    void trim(int x, int z, int radius) {
        tiles.keySet().removeIf(k -> Math.abs((long)(int)(k >> 32)-x)>radius || Math.abs((long)(int)(long)k-z)>radius);
        checked.removeIf(k -> !pending.contains(k) && (Math.abs((long)(int)(k >> 32)-x)>radius || Math.abs((long)(int)(long)k-z)>radius));
    }
}
