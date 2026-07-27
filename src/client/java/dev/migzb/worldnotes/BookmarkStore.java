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
            if (loaded != null) BOOKMARKS.addAll(loaded);
        } catch (IOException | RuntimeException ignored) {
        }
    }

    public static List<Bookmark> forProfile(String profile) {
        return BOOKMARKS.stream().filter(bookmark -> profile.equals(bookmark.profile))
                .sorted(Comparator.comparing(bookmark -> bookmark.name.toLowerCase())).toList();
    }

    public static void save(Bookmark bookmark) {
        BOOKMARKS.removeIf(existing -> existing.id.equals(bookmark.id));
        BOOKMARKS.add(bookmark);
        persist();
    }

    public static void delete(Bookmark bookmark) {
        BOOKMARKS.removeIf(existing -> existing.id.equals(bookmark.id));
        persist();
    }

    public static boolean contains(Bookmark bookmark) {
        return BOOKMARKS.stream().anyMatch(existing -> existing.id.equals(bookmark.id));
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
}
