package dev.migzb.worldnotes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

final class MinimapSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = WorldNotesClient.configDirectory().resolve("worldnotes/minimap.json");
    boolean enabled = true;
    int blocksPerPixel = 2;

    static MinimapSettings load() {
        if (Files.exists(FILE)) {
            try {
                MinimapSettings settings = GSON.fromJson(Files.readString(FILE), MinimapSettings.class);
                if (settings != null) {
                    if (settings.blocksPerPixel != 1 && settings.blocksPerPixel != 2 && settings.blocksPerPixel != 4)
                        settings.blocksPerPixel = 2;
                    return settings;
                }
            } catch (Exception exception) {
                LoggerFactory.getLogger("worldnotes").warn("Unable to read minimap settings; using defaults", exception);
            }
        }
        return new MinimapSettings();
    }

    void save() {
        try {
            AtomicFiles.write(FILE, GSON.toJson(this));
        } catch (Exception exception) {
            LoggerFactory.getLogger("worldnotes").warn("Unable to save minimap settings", exception);
        }
    }
}
