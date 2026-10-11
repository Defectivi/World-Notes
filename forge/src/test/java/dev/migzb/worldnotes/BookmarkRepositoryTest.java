package dev.migzb.worldnotes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises persistence without launching the Minecraft client. */
class BookmarkRepositoryTest {
    @TempDir Path directory;
    private Bookmark entry(String profile) { return new Bookmark(profile, "Home", "original", "minecraft:overworld", -12.5, 70, 35); }

    @Test void draftsAndReturnedEntriesCannotMutateStoredBookmarks() throws Exception {
        Path file = directory.resolve("bookmarks.json");
        BookmarkRepository repository = new BookmarkRepository(file);
        Bookmark bookmark = entry("world:A");
        assertTrue(repository.save(bookmark));
        String persisted = Files.readString(file);
        bookmark.note = "unsaved";
        repository.forProfile("world:A").getFirst().name = "unsaved";
        assertEquals("original", repository.find("world:A", bookmark.id).note);
        assertEquals("Home", repository.find("world:A", bookmark.id).name);
        assertEquals(persisted, Files.readString(file));
    }

    @Test void identicalIdsInDifferentWorldsRemainIndependent() {
        BookmarkRepository repository = new BookmarkRepository(directory.resolve("bookmarks.json"));
        Bookmark first = entry("world:A"), second = first.copy();
        second.profile = "world:B";
        assertTrue(repository.save(first));
        assertTrue(repository.save(second));
        first.name = "Updated";
        assertTrue(repository.save(first));
        assertEquals("Home", repository.find("world:B", first.id).name);
        assertTrue(repository.delete(first));
        assertNotNull(repository.find("world:B", second.id));
    }

    @Test void damagedPrimaryRecoversBackupWithoutOverwritingIt() throws Exception {
        Path file = directory.resolve("bookmarks.json"), backup = directory.resolve("bookmarks.json.bak");
        BookmarkRepository repository = new BookmarkRepository(file);
        Bookmark bookmark = entry("world:A");
        assertTrue(repository.save(bookmark));
        bookmark.note = "second version";
        assertTrue(repository.save(bookmark));
        String goodBackup = Files.readString(backup);
        Files.writeString(file, "{broken");
        repository.load();
        assertEquals("original", repository.find("world:A", bookmark.id).note);
        assertTrue(repository.save(repository.find("world:A", bookmark.id)));
        assertEquals(goodBackup, Files.readString(backup));
        BookmarkRepository reloaded = new BookmarkRepository(file);
        reloaded.load();
        assertEquals("original", reloaded.find("world:A", bookmark.id).note);
    }

    @Test void unrepairableFileIsPreservedAndWritesAreBlocked() throws Exception {
        Path file = directory.resolve("bookmarks.json");
        Files.writeString(file, "not JSON");
        BookmarkRepository repository = new BookmarkRepository(file);
        repository.load();
        assertFalse(repository.save(entry("world:A")));
        assertEquals("not JSON", Files.readString(file));
    }

    @Test void missingPrimaryCanRecoverExistingBackup() throws Exception {
        Path file = directory.resolve("bookmarks.json");
        BookmarkRepository repository = new BookmarkRepository(file);
        Bookmark bookmark = entry("world:A");
        assertTrue(repository.save(bookmark));
        assertTrue(repository.save(bookmark));
        Files.delete(file);
        repository.load();
        assertNotNull(repository.find("world:A", bookmark.id));
    }

    @Test void failedUpdatePreservesExistingSnapshot() throws Exception {
        Path file = directory.resolve("bookmarks.json");
        BookmarkRepository repository = new BookmarkRepository(file);
        Bookmark bookmark = entry("world:A");
        assertTrue(repository.save(bookmark));
        Files.delete(file);
        Files.createDirectory(file);
        bookmark.note = "unsaved";
        assertFalse(repository.save(bookmark));
        assertEquals("original", repository.find("world:A", bookmark.id).note);
    }

    @Test void failedWriteDoesNotChangeMemory() throws Exception {
        Path parent = directory.resolve("blocked");
        Files.writeString(parent, "This is a file, not a directory");
        BookmarkRepository repository = new BookmarkRepository(parent.resolve("bookmarks.json"));
        assertFalse(repository.save(entry("world:A")));
        assertTrue(repository.forProfile("world:A").isEmpty());
        assertEquals(0, repository.revision());
    }

    @Test void invalidCoordinatesAndCommandLikeDimensionsAreRejected() {
        BookmarkRepository repository = new BookmarkRepository(directory.resolve("bookmarks.json"));
        Bookmark bookmark = entry("world:A");
        bookmark.x = Double.NaN;
        assertFalse(repository.save(bookmark));
        bookmark.x = 30_000_000;
        assertFalse(repository.save(bookmark));
        bookmark.x = 0;
        bookmark.dimension = "minecraft:overworld run op @s";
        assertFalse(repository.save(bookmark));
        bookmark.dimension = "custom:moon/base";
        assertTrue(repository.save(bookmark));
    }

    @Test void migratingWorldProfilesPreservesEntriesWithCollidingIds() {
        BookmarkRepository repository = new BookmarkRepository(directory.resolve("bookmarks.json"));
        Bookmark first = entry("world:Old"), second = first.copy();
        second.profile = "world:New";
        assertTrue(repository.save(first));
        assertTrue(repository.save(second));
        repository.migrateProfile("world:Old", "world:New");
        assertEquals(2, repository.forProfile("world:New").size());
        assertEquals(2, repository.forProfile("world:New").stream().map(bookmark -> bookmark.id).distinct().count());
    }

    @Test void roundingHandlesNegativeTiesAndDoesNotOverflowLargeValues() {
        assertEquals(-1.24, Bookmark.roundToHundredth(-1.235));
        assertEquals(1.24, Bookmark.roundToHundredth(1.235));
        assertEquals(1.0e100, Bookmark.roundToHundredth(1.0e100));
        assertThrows(IllegalArgumentException.class, () -> Bookmark.roundToHundredth(Double.POSITIVE_INFINITY));
    }
}
