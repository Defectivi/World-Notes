package dev.migzb.worldnotes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class BookmarkStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Bookmark>>() { }.getType();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("worldnotes/bookmarks.json");
    private static final List<Bookmark> BOOKMARKS = new ArrayList<>();

    private BookmarkStore() { }

    public static void load() {
        BOOKMARKS.clear();
        if (!Files.exists(FILE)) return;
        try {
            List<Bookmark> loaded = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), LIST_TYPE);
            if (loaded != null) {
                for (Bookmark bookmark : loaded) {
                    if (bookmark != null) {
                        normalize(bookmark);
                        BOOKMARKS.add(bookmark);
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }

    public static List<Bookmark> forProfile(String profile) {
        return BOOKMARKS.stream().filter(bookmark -> bookmark != null && profile.equals(bookmark.profile))
                .sorted(Comparator.comparing(bookmark -> bookmark.name == null
                        ? "" : bookmark.name.toLowerCase(java.util.Locale.ROOT))).toList();
    }

    public static void save(Bookmark bookmark) {
        if (bookmark == null) return;
        normalize(bookmark);
        BOOKMARKS.removeIf(existing -> sameId(existing, bookmark));
        BOOKMARKS.add(bookmark);
        persist();
    }

    public static void delete(Bookmark bookmark) {
        if (bookmark == null) return;
        BOOKMARKS.removeIf(existing -> sameId(existing, bookmark));
        persist();
    }

    public static boolean contains(Bookmark bookmark) {
        return BOOKMARKS.stream().anyMatch(existing -> sameId(existing, bookmark));
    }

    public static void migrateProfile(String oldProfile, String newProfile) {
        boolean changed = false;
        for (Bookmark bookmark : BOOKMARKS) {
            if (oldProfile.equals(bookmark.profile)) {
                bookmark.profile = newProfile;
                changed = true;
            }
        }
        if (changed) persist();
    }

    private static void persist() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(BOOKMARKS, LIST_TYPE), StandardCharsets.UTF_8);
        } catch (IOException ignored) { }
    }

    private static boolean sameId(Bookmark first, Bookmark second) {
        return first != null && second != null && first.id != null && first.id.equals(second.id);
    }

    private static void normalize(Bookmark bookmark) {
        if (bookmark.id == null || bookmark.id.isBlank()) bookmark.id = UUID.randomUUID().toString();
        if (bookmark.name == null) bookmark.name = "Untitled bookmark";
        if (bookmark.note == null) bookmark.note = "";
        if (bookmark.profile == null) bookmark.profile = "";
        if (bookmark.dimension == null || bookmark.dimension.isBlank()) bookmark.dimension = "minecraft:overworld";
        if (bookmark.gradient == null || bookmark.gradient.isBlank()) bookmark.gradient = "none";
        if (!Double.isFinite(bookmark.x)) bookmark.x = 0.0;
        if (!Double.isFinite(bookmark.y)) bookmark.y = 0.0;
        if (!Double.isFinite(bookmark.z)) bookmark.z = 0.0;
    }
}
