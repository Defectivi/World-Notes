package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

public final class BookmarkScreen extends Screen {
    private static final int PAGE_SIZE = 2;
    private static final int CARD_WIDTH = 235;
    private static final int MIN_CARD_HEIGHT = 54;
    private static final int ROW_GAP = 4;
    private static final int CONTENT_WIDTH = 205;
    private static final int ACTION_SIZE = 20;
    private static final String[] FILTERS = {"All dimensions", "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"};
    private int page;
    private int filter;

    public BookmarkScreen() {
        this(0, 0);
    }

    private BookmarkScreen(int page, int filter) {
        super(Component.translatable("worldnotes.title"));
        this.page = page;
        this.filter = filter;
    }

    @Override
    protected void init() {
        int center = width / 2;
        addRenderableWidget(Button.builder(Component.literal("   ").append(Component.translatable("worldnotes.add_current")), button -> addCurrent())
                .bounds(center - 155, 30, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("   ").append(Component.translatable("worldnotes.add_manual")), button ->
                minecraft.gui.setScreen(new BookmarkEditorScreen(this, new Bookmark())))
                .bounds(center + 5, 30, 150, 20).build());
        addRenderableWidget(Button.builder(filterComponent(), button -> {
            filter = (filter + 1) % FILTERS.length;
            rebuild();
        }).bounds(center - 155, 55, 310, 20).build());
        buildRows();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> { page--; rebuild(); })
                .bounds(center - 155, height - 45, 35, 20).build()).active = page > 0;
        int pageCount = Math.max(1, (filtered().size() + PAGE_SIZE - 1) / PAGE_SIZE);
        addRenderableWidget(Button.builder(Component.literal(">"), button -> { page++; rebuild(); })
                .bounds(center + 120, height - 45, 35, 20).build()).active = page < pageCount - 1;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(center - 50, height - 45, 100, 20).build());
    }

    private void buildRows() {
        List<Bookmark> bookmarks = filtered();
        int first = page * PAGE_SIZE;
        for (int row = 0; row < PAGE_SIZE && first + row < bookmarks.size(); row++) {
            Bookmark bookmark = bookmarks.get(first + row);
            int y = rowY(bookmarks, first, row);
            int cardHeight = cardHeight(bookmark);
            int actionY = y + (cardHeight - ACTION_SIZE) / 2;
            int center = width / 2;
            addRenderableWidget(Button.builder(Component.empty(), button ->
                    minecraft.gui.setScreen(new BookmarkEditorScreen(this, bookmark, true)))
                    .bounds(center - 155, y, CARD_WIDTH, cardHeight).build());
            addRenderableWidget(Button.builder(Component.empty(), button ->
                            minecraft.gui.setScreen(new BookmarkEditorScreen(this, bookmark)))
                    .tooltip(Tooltip.create(Component.translatable("worldnotes.edit")))
                    .bounds(center + 85, actionY, ACTION_SIZE, ACTION_SIZE).build());
            Component teleportTooltip = hasTeleportCommand()
                    ? Component.translatable("worldnotes.teleport")
                    : Component.translatable("worldnotes.teleport_no_permission");
            Button teleport = addRenderableWidget(Button.builder(Component.empty(), button -> teleport(bookmark))
                    .tooltip(Tooltip.create(teleportTooltip))
                    .bounds(center + 109, actionY, ACTION_SIZE, ACTION_SIZE).build());
            teleport.active = hasTeleportCommand();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int center = width / 2;
        graphics.fakeItem(new ItemStack(Items.WRITABLE_BOOK), center - 151, 32);
        graphics.fakeItem(new ItemStack(Items.WRITABLE_BOOK), center + 9, 32);

        List<Bookmark> bookmarks = filtered();
        int first = page * PAGE_SIZE;
        for (int row = 0; row < PAGE_SIZE && first + row < bookmarks.size(); row++) {
            Bookmark bookmark = bookmarks.get(first + row);
            int y = rowY(bookmarks, first, row);
            int textX = width / 2 - 133;
            int textY = y + 5;
            int cardHeight = cardHeight(bookmark);
            int actionY = y + (cardHeight - ACTION_SIZE) / 2;
            graphics.fill(width / 2 - 155, y, width / 2 - 155 + CARD_WIDTH, y + cardHeight,
                    dimensionTint(bookmark.dimension));
            graphics.fakeItem(new ItemStack(Items.NETHER_STAR), width / 2 - 151, y + 3);
            graphics.fakeItem(new ItemStack(Items.BOOK), width / 2 + 87, actionY + 2);
            graphics.fakeItem(new ItemStack(Items.ENDER_PEARL), width / 2 + 111, actionY + 2);

            textY = drawWrapped(graphics, bookmark.displayName(), textX, textY, 0xFFFFFFFF);
            if (!bookmark.note.isBlank()) {
                textY = drawWrapped(graphics, Component.literal(bookmark.note), textX, textY, 0xFFCCCCCC);
            }
            graphics.text(font, coordinates(bookmark), textX, textY, 0xFFFFFFFF, true);
            textY += 12;
            graphics.text(font, dimensionComponent(bookmark.dimension), textX, textY, 0xFFFFFFFF, true);
        }
    }

    private static int dimensionTint(String dimension) {
        return switch (dimension) {
            case "minecraft:the_nether" -> 0x556A211B;
            case "minecraft:the_end" -> 0x555F5B3F;
            default -> 0x55402D1D;
        };
    }

    private int drawWrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, CONTENT_WIDTH);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            graphics.text(font, line, x, y, color, true);
            y += 12;
        }
        return y;
    }

    private int rowY(List<Bookmark> bookmarks, int first, int row) {
        int y = 85;
        for (int index = 0; index < row; index++) {
            y += cardHeight(bookmarks.get(first + index)) + ROW_GAP;
        }
        return y;
    }

    private int cardHeight(Bookmark bookmark) {
        int lines = font.split(bookmark.displayName(), CONTENT_WIDTH).size() + 2;
        if (!bookmark.note.isBlank()) lines += font.split(Component.literal(bookmark.note), CONTENT_WIDTH).size();
        return Math.max(MIN_CARD_HEIGHT, lines * 12 + 8);
    }

    private List<Bookmark> filtered() {
        String profile = WorldNotesClient.profile(Minecraft.getInstance());
        return BookmarkStore.forProfile(profile).stream()
                .filter(bookmark -> filter == 0 || FILTERS[filter].equals(bookmark.dimension)).toList();
    }

    private void addCurrent() {
        if (minecraft.player == null || minecraft.level == null) return;
        Bookmark bookmark = new Bookmark(WorldNotesClient.profile(minecraft), "New bookmark", "",
                WorldNotesClient.dimension(minecraft.level), minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ());
        minecraft.gui.setScreen(new BookmarkEditorScreen(this, bookmark));
    }

    private boolean hasTeleportCommand() {
        return minecraft.player != null && minecraft.player.connection.getCommands().getRoot().getChild("tp") != null;
    }

    private void teleport(Bookmark bookmark) {
        if (!hasTeleportCommand()) return;
        String position = String.format(Locale.ROOT, "%.2f %.2f %.2f", bookmark.x, bookmark.y, bookmark.z);
        String command = minecraft.level != null && bookmark.dimension.equals(WorldNotesClient.dimension(minecraft.level))
                ? "tp @s " + position
                : "execute in " + bookmark.dimension + " run tp @s " + position;
        minecraft.player.connection.sendCommand(command);
        minecraft.gui.chatListener().handleSystemMessage(Component.translatable("worldnotes.teleport_success",
                bookmark.displayName(), Component.literal(coordinates(bookmark)),
                Component.literal(friendly(bookmark.dimension))), false);
        onClose();
    }

    void rebuild() {
        page = Math.max(0, page);
        minecraft.gui.setScreen(new BookmarkScreen(page, filter));
    }

    static String friendly(String dimension) {
        return switch (dimension) {
            case "All dimensions" -> "All dimensions";
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "The End";
            default -> dimension;
        };
    }

    static Component dimensionComponent(String dimension) {
        String label = friendly(dimension);
        return switch (dimension) {
            case "All dimensions" -> Bookmark.gradientText(label, 0x55FFFF, 0x00AAAA);
            case "minecraft:overworld" -> Bookmark.gradientText(label, 0x55FF55, 0x00AA00);
            case "minecraft:the_nether" -> Bookmark.gradientText(label, 0xFF5555, 0xAA0000);
            case "minecraft:the_end" -> Bookmark.gradientText(label, 0xFF55FF, 0xAA00AA);
            default -> Component.literal(label);
        };
    }

    private Component filterComponent() {
        return Component.literal("Show: ").append(dimensionComponent(FILTERS[filter]));
    }

    private static String coordinates(Bookmark bookmark) {
        return String.format(Locale.ROOT, "%.0f, %.0f, %.0f", bookmark.x, bookmark.y, bookmark.z);
    }
}
