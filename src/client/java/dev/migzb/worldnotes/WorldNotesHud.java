package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Draws the always-visible, client-only location and compass display. */
public final class WorldNotesHud {
    private static final int PANEL = 0xA0101010;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int NORTH = 0xFF62B6FF;
    private static final int EAST = 0xFFFFCC4D;
    private static final int SOUTH = 0xFFFF6B5E;
    private static final int WEST = 0xFFC895FF;

    private WorldNotesHud() { }

    public static void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                              net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        Component dimension = BookmarkScreen.dimensionComponent(WorldNotesClient.dimension(minecraft.level));
        String coordinates = "X: " + minecraft.player.getBlockX()
                + "  Y: " + minecraft.player.getBlockY()
                + "  Z: " + minecraft.player.getBlockZ();

        // Compact coordinate panel in the upper-left corner.
        graphics.fill(4, 4, 174, 34, PANEL);
        graphics.text(minecraft.font, dimension, 9, 8, WHITE, true);
        graphics.text(minecraft.font, coordinates, 9, 20, WHITE, true);

        // Day counter in the lower-left corner. A Minecraft day is 24,000 ticks.
        long day = Math.floorDiv(minecraft.level.getOverworldClockTime(), 24000L);
        String dayLabel = "Day: " + day;
        int dayWidth = minecraft.font.width(dayLabel);
        int bottom = minecraft.getWindow().getGuiScaledHeight();
        graphics.fill(4, bottom - 22, 13 + dayWidth, bottom - 4, PANEL);
        graphics.text(minecraft.font, dayLabel, 9, bottom - 18, WHITE, true);

        // One clear cardinal direction in the upper centre of the screen.
        int center = minecraft.getWindow().getGuiScaledWidth() / 2;
        int direction = directionIndex(minecraft.player.getYRot());
        String label = directionName(direction);
        int labelWidth = minecraft.font.width(label);
        int left = center - labelWidth / 2 - 8;
        graphics.fill(left, 4, left + labelWidth + 16, 22, PANEL);
        graphics.text(minecraft.font, label, center - labelWidth / 2, 8, directionColor(direction), true);

        renderTracking(graphics, minecraft);
    }

    private static void renderTracking(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                                       Minecraft minecraft) {
        Bookmark bookmark = WorldNotesClient.trackedBookmark(minecraft);
        if (bookmark == null) return;

        Component title = Component.literal("Tracking: ").append(bookmark.displayName());
        String targetDimension = BookmarkScreen.friendly(bookmark.dimension);
        boolean sameDimension = bookmark.dimension.equals(WorldNotesClient.dimension(minecraft.level));
        String destination;
        if (sameDimension) {
            double distance = Math.sqrt(minecraft.player.distanceToSqr(bookmark.x, bookmark.y, bookmark.z));
            destination = "Go " + trackingDirection(minecraft, bookmark) + "  |  "
                    + String.format(Locale.ROOT, "%,d blocks remaining", Math.round(distance));
        } else {
            destination = "Travel to " + targetDimension;
        }
        Component dimension = Component.literal("Dimension: ")
                .append(BookmarkScreen.dimensionComponent(bookmark.dimension));

        int width = Math.max(minecraft.font.width(title), Math.max(minecraft.font.width(destination),
                minecraft.font.width(dimension))) + 12;
        int right = minecraft.getWindow().getGuiScaledWidth() - 4;
        int left = right - width;
        graphics.fill(left, 26, right, 70, PANEL);
        graphics.text(minecraft.font, title, left + 6, 30, WHITE, true);
        graphics.text(minecraft.font, destination, left + 6, 43, WHITE, true);
        graphics.text(minecraft.font, dimension, left + 6, 56, WHITE, true);
    }

    private static String trackingDirection(Minecraft minecraft, Bookmark bookmark) {
        double dx = bookmark.x - minecraft.player.getX();
        double dz = bookmark.z - minecraft.player.getZ();
        double degrees = Math.toDegrees(Math.atan2(dx, dz));
        String[] directions = {"South", "Southeast", "East", "Northeast",
                "North", "Northwest", "West", "Southwest"};
        int index = Math.floorMod((int) Math.round(degrees / 45.0), directions.length);
        return directions[index];
    }

    private static int directionIndex(float yaw) {
        // Minecraft yaw: 0 = south, 90 = west, +/-180 = north, -90 = east.
        return switch (Math.floorMod(Math.round(yaw / 90.0f), 4)) {
            case 0 -> 3; // South
            case 1 -> 0; // West
            case 2 -> 1; // North
            default -> 2; // East
        };
    }

    private static String directionName(int direction) {
        return switch (direction) {
            case 0 -> "West";
            case 1 -> "North";
            case 2 -> "East";
            default -> "South";
        };
    }

    private static int directionColor(int direction) {
        return switch (direction) {
            case 0 -> WEST;
            case 1 -> NORTH;
            case 2 -> EAST;
            default -> SOUTH;
        };
    }
}
