package dev.migzb.worldnotes;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Reuses Minecraft's pixel texture, with quiet cartography lines and high-contrast panels. */
final class WorldNotesTheme {
    static final int TEXT = 0xFFF1EEE4;
    static final int MUTED = 0xFFB6C2CE;
    static final int ACCENT = 0xFFE6BE75;
    private static final Identifier STONE = Identifier.parse("minecraft:textures/block/deepslate_tiles.png");
    private WorldNotesTheme() { }

    static void background(GuiGraphicsExtractor graphics, int width, int height) {
        for (int y = 0; y < height; y += 32) for (int x = 0; x < width; x += 32)
            graphics.blit(RenderPipelines.GUI_TEXTURED, STONE, x, y, 0, 0, 32, 32, 16, 16, 16, 16);
        graphics.fillGradient(0, 0, width, height, 0xE5101823, 0xED080F17);
        for (int x = 16; x < width; x += 32) graphics.fill(x, 0, x + 1, height, 0x082FBCDC);
        for (int y = 16; y < height; y += 32) graphics.fill(0, y, width, y + 1, 0x082FBCDC);
    }

    static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x55000000);
        graphics.fillGradient(x, y, x + width, y + height, 0xF0202B38, 0xF0141D29);
        graphics.outline(x, y, width, height, 0xFF435263);
        graphics.fill(x, y, x + 2, y + height, accent);
    }

    static int dimensionAccent(String dimension) {
        return switch (dimension) {
            case "minecraft:the_nether" -> 0xFFE47969;
            case "minecraft:the_end" -> 0xFFC8A0EC;
            default -> 0xFF7DC6A3;
        };
    }
}
