package dev.migzb.worldnotes;

import java.util.List;

public final class BookmarkStore {
    private static final BookmarkRepository REPOSITORY = new BookmarkRepository(
            WorldNotesClient.configDirectory().resolve("worldnotes/bookmarks.json"));

    private BookmarkStore() { }
    public static void load() { REPOSITORY.load(); }
    public static List<Bookmark> forProfile(String profile) { return REPOSITORY.forProfile(profile); }
    public static Bookmark find(String profile, String id) { return REPOSITORY.find(profile, id); }
    public static boolean save(Bookmark bookmark) { return REPOSITORY.save(bookmark); }
    public static boolean delete(Bookmark bookmark) { return REPOSITORY.delete(bookmark); }
    public static boolean contains(Bookmark bookmark) { return find(bookmark.profile, bookmark.id) != null; }
    public static void migrateProfile(String oldProfile, String newProfile) { REPOSITORY.migrateProfile(oldProfile, newProfile); }
    public static long revision() { return REPOSITORY.revision(); }
}
