package dev.migzb.worldnotes;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.lwjgl.glfw.GLFW;

public final class WorldNotesClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("worldnotes", "general"));
    private static KeyMapping openManager;
    private static String trackedId;
    private static String trackedProfile;

    @Override
    public void onInitializeClient() {
        BookmarkStore.load();
        openManager = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.worldnotes.open_manager", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
        HudElementRegistry.addLast(net.minecraft.resources.Identifier.fromNamespaceAndPath("worldnotes", "coordinates"),
                WorldNotesHud::render);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openManager.consumeClick()) client.gui.setScreen(new BookmarkScreen());
        });
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
        for (Bookmark bookmark : BookmarkStore.forProfile(trackedProfile)) {
            if (trackedId.equals(bookmark.id)) return bookmark;
        }
        stopTracking();
        return null;
    }

    public static String profile(Minecraft client) {
        if (client.getSingleplayerServer() != null) {
            String profile = "world:" + client.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath().normalize();
            BookmarkStore.migrateProfile(
                    "world:" + client.getSingleplayerServer().getWorldData().getLevelName(), profile);
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
