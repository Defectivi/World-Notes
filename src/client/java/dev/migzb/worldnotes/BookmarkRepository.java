package dev.migzb.worldnotes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Detached snapshots and transactional writes keep unsaved UI changes out of the store. */
final class BookmarkRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Bookmark>>() { }.getType();
    private final Path file;
    private final Path backup;
    private List<Bookmark> entries = new ArrayList<>();
    private boolean writable = true;
    private boolean primaryHealthy = true;
    private long revision;

    BookmarkRepository(Path file) {
        this.file = file;
        backup = file.resolveSibling(file.getFileName() + ".bak");
    }

    void load() {
        entries = new ArrayList<>();
        writable = true;
        primaryHealthy = true;
        revision++;
        if (!Files.exists(file) && !Files.exists(backup)) return;
        try {
            entries = read(file);
        } catch (IOException | RuntimeException exception) {
            primaryHealthy = false;
            LoggerFactory.getLogger("worldnotes").warn("Unable to read bookmarks; trying backup", exception);
            try {
                entries = read(backup);
            } catch (IOException | RuntimeException backupException) {
                writable = false;
                LoggerFactory.getLogger("worldnotes").error("No readable bookmark backup. Writes are disabled to preserve the original file", backupException);
            }
        }
    }

    private List<Bookmark> read(Path path) throws IOException {
        List<Bookmark> loaded = GSON.fromJson(Files.readString(path), LIST_TYPE);
        if (loaded == null) throw new IOException("Bookmark document must be an array");
        List<Bookmark> result = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        for (Bookmark bookmark : loaded) {
            if (bookmark == null) continue;
            normalize(bookmark);
            if (!bookmark.validCoordinates() || !Bookmark.validDimension(bookmark.dimension)) {
                LoggerFactory.getLogger("worldnotes").warn("Skipping invalid bookmark: {}", bookmark.id);
                continue;
            }
            if (!ids.add(bookmark.profile + "\u0000" + bookmark.id)) bookmark.id = UUID.randomUUID().toString();
            result.add(bookmark);
        }
        return result;
    }

    List<Bookmark> forProfile(String profile) {
        return entries.stream().filter(bookmark -> profile.equals(bookmark.profile))
                .sorted(Comparator.comparing(bookmark -> bookmark.name.toLowerCase(Locale.ROOT)))
                .map(Bookmark::copy).toList();
    }

    Bookmark find(String profile, String id) {
        return entries.stream().filter(bookmark -> profile.equals(bookmark.profile) && id.equals(bookmark.id))
                .findFirst().map(Bookmark::copy).orElse(null);
    }

    boolean save(Bookmark bookmark) {
        Bookmark value = bookmark.copy();
        normalize(value);
        if (!value.validCoordinates() || !Bookmark.validDimension(value.dimension)) return false;
        List<Bookmark> proposed = new ArrayList<>(entries);
        proposed.removeIf(existing -> sameIdentity(existing, value));
        proposed.add(value);
        return persist(proposed);
    }

    boolean delete(Bookmark bookmark) {
        List<Bookmark> proposed = new ArrayList<>(entries);
        proposed.removeIf(existing -> sameIdentity(existing, bookmark));
        return persist(proposed);
    }

    void migrateProfile(String oldProfile, String newProfile) {
        if (entries.stream().noneMatch(bookmark -> oldProfile.equals(bookmark.profile))) return;
        List<Bookmark> proposed = entries.stream().map(Bookmark::copy).toList();
        proposed.forEach(bookmark -> { if (oldProfile.equals(bookmark.profile)) bookmark.profile = newProfile; });
        HashSet<String> identities = new HashSet<>();
        proposed.forEach(bookmark -> {
            if (!identities.add(bookmark.profile + "\u0000" + bookmark.id)) bookmark.id = UUID.randomUUID().toString();
        });
        persist(proposed);
    }

    long revision() { return revision; }

    private boolean persist(List<Bookmark> proposed) {
        if (!writable) return false;
        try {
            if (primaryHealthy && Files.exists(file)) AtomicFiles.write(backup, Files.readString(file));
            AtomicFiles.write(file, GSON.toJson(proposed, LIST_TYPE));
            entries = proposed;
            primaryHealthy = true;
            revision++;
            return true;
        } catch (IOException exception) {
            LoggerFactory.getLogger("worldnotes").error("Unable to save bookmarks", exception);
            return false;
        }
    }

    private static boolean sameIdentity(Bookmark first, Bookmark second) {
        return first.id.equals(second.id) && first.profile.equals(second.profile);
    }

    private static void normalize(Bookmark bookmark) {
        if (bookmark.id == null || bookmark.id.isBlank()) bookmark.id = UUID.randomUUID().toString();
        if (bookmark.name == null || bookmark.name.isBlank()) bookmark.name = "Untitled bookmark";
        if (bookmark.note == null) bookmark.note = "";
        if (bookmark.profile == null) bookmark.profile = "";
        if (bookmark.dimension == null || bookmark.dimension.isBlank()) bookmark.dimension = "minecraft:overworld";
        if (bookmark.gradient == null || bookmark.gradient.isBlank()) bookmark.gradient = "none";
    }
}
