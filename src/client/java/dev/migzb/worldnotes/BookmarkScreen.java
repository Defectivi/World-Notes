package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BookmarkScreen extends Screen {
    private static final Identifier NIGHT_VISION_EFFECT = Identifier.parse("minecraft:textures/mob_effect/night_vision.png");
    private static final int PAGE_SIZE = 2;
    private static final int CARD_WIDTH = 235;
    private static final int MIN_CARD_HEIGHT = 54;
    private static final int ROW_GAP = 4;
    private static final int CONTENT_WIDTH = 205;
    private static final int ACTION_SIZE = 20;
    private static final int MAIN_FIRST_ROW_Y = 85;
    private static final int HIDDEN_FIRST_ROW_Y = 60;
    private final boolean hiddenView;
    private int page;
    private int filter;

    public BookmarkScreen() {
        this(0, 0, false);
    }

    private BookmarkScreen(int page, int filter, boolean hiddenView) {
        super(Component.translatable(hiddenView ? "worldnotes.hidden.title" : "worldnotes.title"));
        this.page = page;
        this.filter = filter;
        this.hiddenView = hiddenView;
    }

    static BookmarkScreen hiddenScreen() {
        return new BookmarkScreen(0, 0, true);
    }

    @Override
    protected void init() {
        int center = width / 2;
        int filterY = hiddenView ? 30 : 55;
        int pageCount = Math.max(1, (filtered().size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pageCount - 1);
        if (!hiddenView) {
            addRenderableWidget(Button.builder(Component.literal("   ").append(Component.translatable("worldnotes.add_current")), button -> addCurrent())
                    .tooltip(Tooltip.create(Component.translatable("worldnotes.add_current.tooltip")))
                    .bounds(center - 155, 30, 150, 20).build());
            addRenderableWidget(Button.builder(Component.literal("   ").append(Component.translatable("worldnotes.add_manual")), button ->
                    WorldNotesClient.setScreen(minecraft, new BookmarkEditorScreen(this, new Bookmark())))
                    .tooltip(Tooltip.create(Component.translatable("worldnotes.add_manual.tooltip")))
                    .bounds(center + 5, 30, 150, 20).build());
            Button hidden = addRenderableWidget(Button.builder(Component.empty(), button -> WorldNotesClient.setScreen(minecraft, hiddenScreen()))
                    .tooltip(Tooltip.create(Component.translatable("worldnotes.hidden.tooltip")))
                    .bounds(4, height - 45, ACTION_SIZE, ACTION_SIZE).build());
            hidden.active = BookmarkStore.forProfile(WorldNotesClient.profile(minecraft)).stream().anyMatch(bookmark -> bookmark.hidden);
        }
        addRenderableWidget(Button.builder(filterComponent(), button -> {
            filter = (filter + 1) % DimensionStyle.VALUES.length;
            rebuild();
        }).tooltip(Tooltip.create(Component.translatable("worldnotes.filter.tooltip")))
                .bounds(center - 155, filterY, 310, 20).build());
        buildRows();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> { page--; rebuild(); })
                .tooltip(Tooltip.create(Component.translatable("worldnotes.previous_page.tooltip")))
                .bounds(center - 155, height - 45, 35, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), button -> { page++; rebuild(); })
                .tooltip(Tooltip.create(Component.translatable("worldnotes.next_page.tooltip")))
                .bounds(center + 120, height - 45, 35, 20).build()).active = page < pageCount - 1;
        addRenderableWidget(Button.builder(Component.translatable(hiddenView ? "gui.back" : "gui.done"), button -> {
            if (hiddenView) WorldNotesClient.setScreen(minecraft, new BookmarkScreen());
            else onClose();
        }).tooltip(Tooltip.create(Component.translatable(hiddenView ? "worldnotes.back.tooltip" : "worldnotes.done.tooltip")))
                .bounds(center - 50, height - 45, 100, 20).build());
    }

    private void buildRows() {
        int center = width / 2;
        for (RowLayout row : currentPageLayout()) {
            addRenderableWidget(Button.builder(Component.empty(), button ->
                    WorldNotesClient.setScreen(minecraft, new BookmarkEditorScreen(this, row.bookmark(), true)))
                    .tooltip(Tooltip.create(Component.translatable("worldnotes.view.tooltip")))
                    .bounds(center - 155, row.y(), CARD_WIDTH, row.height()).build());
            addRenderableWidget(Button.builder(Component.empty(), button ->
                            WorldNotesClient.setScreen(minecraft, new BookmarkEditorScreen(this, row.bookmark())))
                    .tooltip(Tooltip.create(Component.translatable("worldnotes.edit")))
                    .bounds(center + 85, row.actionY(), ACTION_SIZE, ACTION_SIZE).build());
            if (hiddenView) {
                addRenderableWidget(Button.builder(Component.empty(), button -> confirmUnhide(row.bookmark()))
                        .tooltip(Tooltip.create(Component.translatable("worldnotes.unhide.tooltip")))
                        .bounds(center + 109, row.actionY(), ACTION_SIZE, ACTION_SIZE).build());
            } else {
                Component teleportTooltip = hasTeleportCommand()
                        ? Component.translatable("worldnotes.teleport")
                        : Component.translatable("worldnotes.teleport_no_permission");
                Button teleport = addRenderableWidget(Button.builder(Component.empty(), button -> teleport(row.bookmark()))
                        .tooltip(Tooltip.create(teleportTooltip))
                        .bounds(center + 109, row.actionY(), ACTION_SIZE, ACTION_SIZE).build());
                teleport.active = hasTeleportCommand();
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int center = width / 2;
        graphics.centeredText(font, getTitle(), center, 10, 0xFFFFFFFF);
        if (!hiddenView) {
            graphics.fakeItem(new ItemStack(Items.WRITABLE_BOOK), center - 151, 32);
            graphics.fakeItem(new ItemStack(Items.WRITABLE_BOOK), center + 9, 32);
            graphics.fakeItem(new ItemStack(Items.BARRIER), 6, height - 43);
        }

        for (RowLayout row : currentPageLayout()) {
            Bookmark bookmark = row.bookmark();
            int textX = center - 133;
            int textY = row.y() + 5;
            graphics.fill(center - 155, row.y(), center - 155 + CARD_WIDTH, row.y() + row.height(),
                    DimensionStyle.tint(bookmark.dimension));
            graphics.fakeItem(new ItemStack(Items.NETHER_STAR), center - 151, row.y() + 3);
            graphics.fakeItem(new ItemStack(Items.BOOK), center + 87, row.actionY() + 2);
            if (hiddenView) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, NIGHT_VISION_EFFECT,
                        center + 111, row.actionY() + 2, 0, 0, 18, 18, 18, 18);
            } else {
                graphics.fakeItem(new ItemStack(Items.ENDER_PEARL), center + 111, row.actionY() + 2);
            }

            textY = drawWrapped(graphics, bookmark.displayName(), textX, textY, 0xFFFFFFFF);
            if (!bookmark.note.isBlank()) textY = drawWrapped(graphics, Component.literal(bookmark.note), textX, textY, 0xFFCCCCCC);
            graphics.text(font, coordinates(bookmark), textX, textY, 0xFFFFFFFF, true);
            textY += 12;
            graphics.text(font, dimensionComponent(bookmark.dimension), textX, textY, 0xFFFFFFFF, true);
        }
    }

    boolean isHiddenView() {
        return hiddenView;
    }

    private int drawWrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, CONTENT_WIDTH);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            graphics.text(font, line, x, y, color, true);
            y += 12;
        }
        return y;
    }

    private record RowLayout(Bookmark bookmark, int y, int height, int actionY) { }

    private List<RowLayout> currentPageLayout() {
        List<Bookmark> bookmarks = filtered();
        int first = page * PAGE_SIZE;
        List<RowLayout> rows = new ArrayList<>(PAGE_SIZE);
        int y = hiddenView ? HIDDEN_FIRST_ROW_Y : MAIN_FIRST_ROW_Y;
        for (int row = 0; row < PAGE_SIZE && first + row < bookmarks.size(); row++) {
            Bookmark bookmark = bookmarks.get(first + row);
            int height = cardHeight(bookmark);
            rows.add(new RowLayout(bookmark, y, height, y + (height - ACTION_SIZE) / 2));
            y += height + ROW_GAP;
        }
        return rows;
    }

    private int cardHeight(Bookmark bookmark) {
        int lines = font.split(bookmark.displayName(), CONTENT_WIDTH).size() + 2;
        if (!bookmark.note.isBlank()) lines += font.split(Component.literal(bookmark.note), CONTENT_WIDTH).size();
        return Math.max(MIN_CARD_HEIGHT, lines * 12 + 8);
    }

    private List<Bookmark> filtered() {
        String profile = WorldNotesClient.profile(Minecraft.getInstance());
        DimensionStyle style = DimensionStyle.VALUES[filter];
        return BookmarkStore.forProfile(profile).stream()
                .filter(bookmark -> bookmark.hidden == hiddenView)
                .filter(bookmark -> style == DimensionStyle.ALL || style.key.equals(bookmark.dimension))
                .toList();
    }

    private void addCurrent() {
        if (minecraft.player == null || minecraft.level == null) return;
        Bookmark bookmark = new Bookmark(WorldNotesClient.profile(minecraft), "New bookmark", "",
                WorldNotesClient.dimension(minecraft.level),
                Bookmark.roundToHundredth(minecraft.player.getX()),
                Bookmark.roundToHundredth(minecraft.player.getY()),
                Bookmark.roundToHundredth(minecraft.player.getZ()));
        WorldNotesClient.setScreen(minecraft, new BookmarkEditorScreen(this, bookmark));
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
                Component.literal(DimensionStyle.friendly(bookmark.dimension))), false);
        onClose();
    }

    private void confirmUnhide(Bookmark bookmark) {
        WorldNotesClient.setScreen(minecraft, new ConfirmScreen(confirmed -> {
            if (confirmed) {
                bookmark.hidden = false;
                BookmarkStore.save(bookmark);
                rebuild();
            } else {
                WorldNotesClient.setScreen(minecraft, this);
            }
        }, Component.translatable("worldnotes.unhide_confirm.title"),
                Component.translatable("worldnotes.unhide_confirm.message"),
                Component.translatable("worldnotes.unhide_confirm.confirm"),
                Component.translatable("worldnotes.unhide_confirm.cancel")));
    }

    void rebuild() {
        page = Math.max(0, page);
        WorldNotesClient.setScreen(minecraft, new BookmarkScreen(page, filter, hiddenView));
    }

    static String friendly(String dimension) {
        return DimensionStyle.friendly(dimension);
    }

    static Component dimensionComponent(String dimension) {
        return DimensionStyle.component(dimension);
    }

    private Component filterComponent() {
        return Component.literal("Show: ").append(dimensionComponent(DimensionStyle.VALUES[filter].key));
    }

    private static String coordinates(Bookmark bookmark) {
        return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", bookmark.x, bookmark.y, bookmark.z);
    }

    private enum DimensionStyle {
        ALL("All dimensions", "All dimensions", 0x55402D1D, 0x55FFFF, 0x00AAAA),
        OVERWORLD("minecraft:overworld", "Overworld", 0x55402D1D, 0x55FF55, 0x00AA00),
        NETHER("minecraft:the_nether", "Nether", 0x556A211B, 0xFF5555, 0xAA0000),
        END("minecraft:the_end", "The End", 0x555F5B3F, 0xFF55FF, 0xAA00AA);

        static final DimensionStyle[] VALUES = values();
        final String key;
        final String label;
        final int tint;
        final int gradientStart;
        final int gradientEnd;

        DimensionStyle(String key, String label, int tint, int gradientStart, int gradientEnd) {
            this.key = key;
            this.label = label;
            this.tint = tint;
            this.gradientStart = gradientStart;
            this.gradientEnd = gradientEnd;
        }

        private static DimensionStyle fromKey(String key) {
            for (DimensionStyle style : VALUES) if (style.key.equals(key)) return style;
            return null;
        }

        static String friendly(String dimension) {
            DimensionStyle style = fromKey(dimension);
            return style != null ? style.label : dimension;
        }

        static Component component(String dimension) {
            DimensionStyle style = fromKey(dimension);
            return style != null ? Bookmark.gradientText(style.label, style.gradientStart, style.gradientEnd)
                    : Component.literal(dimension);
        }

        static int tint(String dimension) {
            DimensionStyle style = fromKey(dimension);
            return style != null ? style.tint : OVERWORLD.tint;
        }
    }
}
