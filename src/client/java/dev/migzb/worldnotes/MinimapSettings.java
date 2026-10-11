package dev.migzb.worldnotes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class MinimapSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("worldnotes/minimap.json");
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
            Files.createDirectories(FILE.getParent());
            Path temporary = FILE.resolveSibling("minimap.json.tmp");
            Files.writeString(temporary, GSON.toJson(this));
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception exception) {
            LoggerFactory.getLogger("worldnotes").warn("Unable to save minimap settings", exception);
        }
    }
}
