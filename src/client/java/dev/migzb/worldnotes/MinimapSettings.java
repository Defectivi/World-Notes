package dev.migzb.worldnotes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

final class MinimapSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    enum Position { TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT, CUSTOM }
    private static Path file() { return WorldNotesClient.configDirectory().resolve("worldnotes/minimap.json"); }
    boolean enabled = true;
    boolean items = true, neutralMobs = true, hostileMobs = true, players = true, rainbowHighlight = true;
    boolean nearbyHeights = true;
    int radiusChunks = 32, size = 96, customX = 100, customY = 0;
    Position position = Position.TOP_RIGHT;
    void normalize() {
        if (radiusChunks != 8 && radiusChunks != 16 && radiusChunks != 32) radiusChunks = 32;
        if (size != 96 && size != 128 && size != 160) size = 96;
        if (position == null) position = Position.TOP_RIGHT;
        customX = Math.clamp(customX, 0, 100); customY = Math.clamp(customY, 0, 100);
    }
    MinimapSettings copy() { return GSON.fromJson(GSON.toJson(this), MinimapSettings.class); }

    static MinimapSettings load() {
        Path FILE = file();
        if (Files.exists(FILE)) {
            try {
                MinimapSettings settings = GSON.fromJson(Files.readString(FILE), MinimapSettings.class);
                if (settings != null) {
                    settings.normalize();
                    return settings;
                }
            } catch (Exception exception) {
                LoggerFactory.getLogger("worldnotes").warn("Unable to read minimap settings; using defaults", exception);
            }
        }
        return new MinimapSettings();
    }

    boolean save() {
        try {
            normalize();
            AtomicFiles.write(file(), GSON.toJson(this));
            return true;
        } catch (Exception exception) {
            LoggerFactory.getLogger("worldnotes").warn("Unable to save minimap settings", exception);
            return false;
        }
    }
}
