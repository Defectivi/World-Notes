package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Arrays;
import java.util.List;

/** Samples only chunks already received by the client, with a fixed amount of work per tick. */
final class WorldNotesMinimap {
    static final int SIZE = 96;
    private static final int GRID = 48;
    private static final int UNKNOWN = 0xFF202630;
    private static final int[] COLORS = new int[GRID * GRID];
    private static MinimapSettings settings;
    private static ClientLevel level;
    private static int originX, originZ, cursor;
    private static boolean initialized;
    private static List<Bookmark> markers = List.of();
    private static String markerProfile;
    private static long markerRevision = -1;
    private static final BlockPos.MutableBlockPos POSITION = new BlockPos.MutableBlockPos();

    private WorldNotesMinimap() { }

    static void initialize() { settings = MinimapSettings.load(); }
    static boolean visible(Minecraft client) {
        return settings != null && settings.enabled && client.player != null && client.level != null
                && !client.gui.hud.isHidden() && client.getWindow().getGuiScaledWidth() >= 240
                && client.getWindow().getGuiScaledHeight() >= 180;
    }
    static void toggle(Minecraft client) {
        settings.enabled = !settings.enabled;
        settings.save();
        if (client.player != null) client.gui.hud.setOverlayMessage(
                Component.translatable(settings.enabled ? "worldnotes.minimap.enabled" : "worldnotes.minimap.disabled"), false);
    }
    static void zoom(Minecraft client) {
        settings.blocksPerPixel = settings.blocksPerPixel == 4 ? 1 : settings.blocksPerPixel * 2;
        initialized = false;
        settings.save();
        if (client.player != null) client.gui.hud.setOverlayMessage(
                Component.translatable("worldnotes.minimap.zoom", GRID * settings.blocksPerPixel), false);
    }

    static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            level = null;
            initialized = false;
            return;
        }
        if (!settings.enabled) return;
        String profile = WorldNotesClient.profile(client);
        if (!profile.equals(markerProfile) || markerRevision != BookmarkStore.revision()) {
            markers = BookmarkStore.forProfile(profile);
            markerProfile = profile;
            markerRevision = BookmarkStore.revision();
        }
        int scale = settings.blocksPerPixel;
        int x = Math.floorDiv(client.player.getBlockX(), scale) * scale - GRID / 2 * scale;
        int z = Math.floorDiv(client.player.getBlockZ(), scale) * scale - GRID / 2 * scale;
        if (!initialized || level != client.level) {
            Arrays.fill(COLORS, UNKNOWN);
            originX = x;
            originZ = z;
            level = client.level;
            cursor = 0;
            initialized = true;
        } else if (x != originX || z != originZ) {
            // Preserve overlapping samples so movement does not blank the whole map.
            int dx = (x - originX) / scale, dz = (z - originZ) / scale;
            int[] previous = COLORS.clone();
            for (int row = 0; row < GRID; row++) for (int col = 0; col < GRID; col++) {
                int oldX = col + dx, oldZ = row + dz;
                COLORS[row * GRID + col] = oldX >= 0 && oldX < GRID && oldZ >= 0 && oldZ < GRID
                        ? previous[oldZ * GRID + oldX] : UNKNOWN;
            }
            originX = x;
            originZ = z;
        }
        for (int count = 0; count < 192; count++) {
            int index = cursor++ % COLORS.length;
            int blockX = originX + index % GRID * scale;
            int blockZ = originZ + index / GRID * scale;
            LevelChunk chunk = level.getChunkSource().getChunk(blockX >> 4, blockZ >> 4, false);
            int color = UNKNOWN;
            if (chunk != null) {
                color = sampleColor(chunk, blockX, blockZ, client.player.getBlockY());
            }
            COLORS[index] = color;
        }
        cursor %= COLORS.length;
    }

    static void render(GuiGraphicsExtractor graphics, Minecraft client) {
        if (!visible(client) || !initialized || level != client.level) return;
        int left = client.getWindow().getGuiScaledWidth() - SIZE - 9, top = 18;
        WorldNotesTheme.panel(graphics, left - 3, top - 3, SIZE + 6, SIZE + 17,
                WorldNotesTheme.dimensionAccent(WorldNotesClient.dimension(level)));
        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID;) {
                int color = COLORS[row * GRID + col], end = col + 1;
                while (end < GRID && COLORS[row * GRID + end] == color) end++;
                graphics.fill(left + col * 2, top + row * 2, left + end * 2, top + row * 2 + 2, color);
                col = end;
            }
        }
        String dimension = WorldNotesClient.dimension(level);
        for (Bookmark bookmark : markers) {
            if (bookmark.hidden || !dimension.equals(bookmark.dimension) || WorldNotesClient.isTracked(bookmark)) continue;
            int x = mapCoordinate(bookmark.x, originX), z = mapCoordinate(bookmark.z, originZ);
            if (x < 2 || z < 2 || x >= SIZE - 2 || z >= SIZE - 2) continue;
            int color = 0xFF6DE5FF;
            graphics.fill(left + x - 2, top + z - 2, left + x + 3, top + z + 3, 0xFF10151D);
            graphics.fill(left + x - 1, top + z - 1, left + x + 2, top + z + 2, color);
        }
        Bookmark target = WorldNotesClient.trackedBookmark(client);
        if (target != null && !target.hidden && dimension.equals(target.dimension)) {
            int x = Math.clamp(mapCoordinate(target.x, originX), 2, SIZE - 3);
            int z = Math.clamp(mapCoordinate(target.z, originZ), 2, SIZE - 3);
            graphics.fill(left + x - 2, top + z - 2, left + x + 3, top + z + 3, 0xFF10151D);
            graphics.fill(left + x - 1, top + z - 1, left + x + 2, top + z + 2, WorldNotesTheme.ACCENT);
        }
        int px = mapCoordinate(client.player.getX(), originX), pz = mapCoordinate(client.player.getZ(), originZ);
        graphics.fill(left + px - 2, top + pz - 2, left + px + 3, top + pz + 3, 0xFF000000);
        graphics.fill(left + px - 1, top + pz - 1, left + px + 2, top + pz + 2, 0xFFFFFFFF);
        double yaw = Math.toRadians(client.player.getYRot());
        for (int step = 3; step <= 7; step++) {
            int hx = left + px - (int) Math.round(Math.sin(yaw) * step);
            int hz = top + pz + (int) Math.round(Math.cos(yaw) * step);
            graphics.fill(hx, hz, hx + 1, hz + 1, 0xFFFFFFFF);
        }
        graphics.text(client.font, "N", left + SIZE / 2 - 3, top - 13, 0xFFFFFFFF, true);
        graphics.text(client.font, Component.translatable("worldnotes.minimap.range", GRID * settings.blocksPerPixel),
                left + 2, top + SIZE + 3, 0xFFD4DEE9, true);
    }

    private static int mapCoordinate(double value, int origin) {
        return (int) Math.floor((value - origin) * 2 / settings.blocksPerPixel);
    }

    private static int sampleColor(LevelChunk chunk, int x, int z, int playerY) {
        int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
        if (level.dimensionType().hasCeiling() && playerY < level.dimensionType().logicalHeight()) {
            // Look for a cave floor near the player's elevation instead of painting the Nether roof.
            height = Math.min(height, playerY + 16);
            boolean foundAir = false;
            for (int depth = 0; depth < 64 && height >= level.getMinY(); depth++, height--) {
                POSITION.set(x, height, z);
                var state = chunk.getBlockState(POSITION);
                if (state.isAir()) foundAir = true;
                else if (foundAir || !state.getFluidState().isEmpty()) {
                    int rgb = state.getMapColor(level, POSITION).col;
                    if (rgb != 0) return 0xFF000000 | rgb;
                }
            }
            return UNKNOWN;
        }
        // Skip colorless surface blocks such as glass, without scanning entire columns.
        for (int depth = 0; depth < 16 && height >= level.getMinY(); depth++, height--) {
            POSITION.set(x, height, z);
            int rgb = chunk.getBlockState(POSITION).getMapColor(level, POSITION).col;
            if (rgb != 0) return 0xFF000000 | rgb;
        }
        return UNKNOWN;
    }
}
