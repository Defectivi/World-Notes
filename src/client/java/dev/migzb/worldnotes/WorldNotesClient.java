package dev.migzb.worldnotes;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Path;
import java.util.List;

public final class WorldNotesClient {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("worldnotes", "general"));
    private static KeyMapping openManager;
    private static KeyMapping toggleMinimap;
    private static KeyMapping zoomMinimap;
    private static String trackedId;
    private static String trackedProfile;
    private static Path configDirectory;
    private static String migratedProfile;

    public static void initialize(Path configDir) {
        if (configDirectory != null) return;
        configDirectory = configDir;
        BookmarkStore.load();
        WorldNotesMinimap.initialize();
        toggleMinimap = new KeyMapping("key.worldnotes.toggle_minimap", InputConstants.KEY_M, CATEGORY);
        zoomMinimap = new KeyMapping("key.worldnotes.zoom_minimap", InputConstants.KEY_N, CATEGORY);
        openManager = new KeyMapping("key.worldnotes.open_manager", InputConstants.KEY_B, CATEGORY);
    }

    public static Path configDirectory() {
        if (configDirectory == null) throw new IllegalStateException("World Notes has not initialized");
        return configDirectory;
    }

    public static List<KeyMapping> keyMappings() {
        return List.of(openManager, toggleMinimap, zoomMinimap);
    }

    public static void tick(Minecraft client) {
        if (configDirectory == null) return;
        while (openManager.consumeClick()) setScreen(client, new BookmarkScreen());
        while (toggleMinimap.consumeClick()) if (client.player != null) setScreen(client, new ExpandedMapScreen());
        while (zoomMinimap.consumeClick()) WorldNotesMinimap.zoom(client);
        WorldNotesMinimap.tick(client);
        StructureCheats.tick(client);
        WorldNotesBlockHighlight.update(client);
    }
    static boolean mapKey(net.minecraft.client.input.KeyEvent event) { return toggleMinimap.matches(event); }
    static boolean zoomKey(net.minecraft.client.input.KeyEvent event) { return zoomMinimap.matches(event); }

    public static void setScreen(Minecraft client, Screen screen) {
        try {
            client.getClass().getMethod("setScreen", Screen.class).invoke(client, screen);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            client.gui.getClass().getMethod("setScreen", Screen.class).invoke(client.gui, screen);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to open a World Notes screen", exception);
        }
    }

    public static void track(Bookmark bookmark) {
        trackedId = bookmark.id;
        trackedProfile = bookmark.profile;
    }

    public static void stopTracking() {
        trackedId = null;
        trackedProfile = null;
    }

    public static boolean isTracked(Bookmark bookmark) {
        return trackedId != null && trackedId.equals(bookmark.id)
                && trackedProfile != null && trackedProfile.equals(bookmark.profile);
    }

    public static boolean canTrack(Bookmark bookmark) {
        return trackedId == null || trackedProfile == null
                || !trackedProfile.equals(bookmark.profile) || isTracked(bookmark);
    }

    public static Bookmark trackedBookmark(Minecraft client) {
        if (trackedId == null || !profile(client).equals(trackedProfile)) return null;
        Bookmark bookmark = BookmarkStore.find(trackedProfile, trackedId);
        if (bookmark != null) return bookmark;
        stopTracking();
        return null;
    }

    public static String profile(Minecraft client) {
        if (client.getSingleplayerServer() != null) {
            String profile = "world:" + client.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath().normalize();
            if (!profile.equals(migratedProfile)) {
                BookmarkStore.migrateProfile("world:" + client.getSingleplayerServer().getWorldData().getLevelName(), profile);
                migratedProfile = profile;
            }
            return profile;
        }
        if (client.getCurrentServer() != null) return "server:" + client.getCurrentServer().ip;
        return "unknown";
    }

    public static String dimension(Level level) {
        ResourceKey<Level> key = level.dimension();
        return key.identifier().toString();
    }
}
