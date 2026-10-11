package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BookmarkScreen extends Screen {
    private static final String[] FILTERS = {"all", "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"};
    private final boolean hiddenView;
    private int page, filter, pageCount, left, panelWidth, cardWidth, cardHeight;
    private String query;
    private EditBox search;
    private List<RowLayout> rows = List.of();
    private final List<Button> cards = new ArrayList<>();
    private Component error = Component.empty();

    public BookmarkScreen() { this(0, 0, false, ""); }
    private BookmarkScreen(int page, int filter, boolean hiddenView, String query) {
        super(Component.translatable(hiddenView ? "worldnotes.hidden.title" : "worldnotes.title"));
        this.page = page;
        this.filter = filter;
        this.hiddenView = hiddenView;
        this.query = query;
    }
    static BookmarkScreen hiddenScreen() { return new BookmarkScreen(0, 0, true, ""); }

    @Override
    protected void init() {
        panelWidth = Math.min(360, width - 16);
        left = (width - panelWidth) / 2;
        cardWidth = panelWidth - 52;
        int firstRow = hiddenView ? 73 : 98;
        int available = Math.max(33, height - 70 - firstRow);
        cardHeight = Math.min(54, available);
        int pageSize = Math.max(1, (available + 6) / (cardHeight + 6));
        List<Bookmark> bookmarks = filtered();
        pageCount = Math.max(1, (bookmarks.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, pageCount - 1));
        List<RowLayout> layout = new ArrayList<>();
        int first = page * pageSize;
        for (int index = first; index < Math.min(first + pageSize, bookmarks.size()); index++)
            layout.add(new RowLayout(bookmarks.get(index), firstRow + (index - first) * (cardHeight + 6)));
        rows = List.copyOf(layout);
        cards.clear();
        if (!hiddenView) {
            int half = (panelWidth - 4) / 2;
            addRenderableWidget(Button.builder(Component.translatable("worldnotes.add_current"), button -> addCurrent())
                    .bounds(left, 28, half, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("worldnotes.add_manual"), button ->
                    WorldNotesClient.setScreen(minecraft, new BookmarkEditorScreen(this, new Bookmark())))
                    .bounds(left + half + 4, 28, half, 18).build());
        }
        int filterY = hiddenView ? 27 : 52;
        addRenderableWidget(Button.builder(Component.translatable("worldnotes.filter", dimensionComponent(FILTERS[filter])), button -> {
            filter = (filter + 1) % FILTERS.length;
            page = 0;
            rebuild();
        }).bounds(left, filterY, panelWidth, 18).build());
        if (search == null) {
            search = new EditBox(font, left + 1, filterY + 23, panelWidth - 2, 18, Component.translatable("worldnotes.search"));
            search.setMaxLength(128);
            search.setHint(Component.translatable("worldnotes.search"));
            search.setValue(query);
            search.setResponder(value -> {
                query = value;
                page = 0;
                clearWidgets();
                init();
            });
        } else {
            search.setX(left + 1);
            search.setY(filterY + 23);
            search.setWidth(panelWidth - 2);
        }
        addRenderableWidget(search);
        setInitialFocus(search);
        for (RowLayout row : rows) {
            Bookmark bookmark = row.bookmark();
            cards.add(addRenderableWidget(Button.builder(bookmark.displayName(), button ->
                    WorldNotesClient.setScreen(minecraft, new BookmarkEditorScreen(this, bookmark, true)))
                    .tooltip(Tooltip.create(Component.translatable("worldnotes.view.tooltip")))
                    .bounds(left, row.y(), cardWidth, cardHeight).build()));
            int actionY = row.y() + (cardHeight - 20) / 2;
            addRenderableWidget(Button.builder(Component.empty(), button ->
                    WorldNotesClient.setScreen(minecraft, new BookmarkEditorScreen(this, bookmark)))
                    .createNarration(supplier -> Component.translatable("worldnotes.edit"))
                    .tooltip(Tooltip.create(Component.translatable("worldnotes.edit")))
                    .bounds(left + cardWidth + 4, actionY, 20, 20).build());
            if (hiddenView) addRenderableWidget(Button.builder(Component.empty(), button -> confirmUnhide(bookmark))
                    .createNarration(supplier -> Component.translatable("worldnotes.unhide.tooltip"))
                    .tooltip(Tooltip.create(Component.translatable("worldnotes.unhide.tooltip")))
                    .bounds(left + cardWidth + 28, actionY, 20, 20).build());
            else {
                Button teleport = addRenderableWidget(Button.builder(Component.empty(), button -> teleport(bookmark))
                        .createNarration(supplier -> Component.translatable("worldnotes.teleport"))
                        .tooltip(Tooltip.create(Component.translatable(canTeleport(bookmark) ? "worldnotes.teleport" : "worldnotes.teleport_no_permission")))
                        .bounds(left + cardWidth + 28, actionY, 20, 20).build());
                teleport.active = canTeleport(bookmark);
            }
        }
        int footer = height - 26;
        addRenderableWidget(Button.builder(Component.empty(), button -> WorldNotesClient.setScreen(minecraft, new MinimapSettingsScreen(this)))
                .bounds(left, footer - 24, 20, 20).tooltip(Tooltip.create(Component.literal("Map settings")))
                .createNarration(supplier -> Component.literal("Map settings")).build());
        if (!hiddenView) addRenderableWidget(Button.builder(Component.empty(), button ->
                WorldNotesClient.setScreen(minecraft, hiddenScreen())).bounds(left, footer, 20, 20)
                .createNarration(supplier -> Component.translatable("worldnotes.hidden.title"))
                .tooltip(Tooltip.create(Component.translatable("worldnotes.hidden.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> { page--; rebuild(); })
                .bounds(left + 24, footer, 24, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), button -> { page++; rebuild(); })
                .bounds(left + panelWidth - 24, footer, 24, 20).build()).active = page < pageCount - 1;
        addRenderableWidget(Button.builder(Component.translatable(hiddenView ? "gui.back" : "gui.done"), button -> onClose())
                .bounds(width / 2 - 40, footer, 80, 20).build());
    }

    private List<Bookmark> filtered() {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return BookmarkStore.forProfile(WorldNotesClient.profile(Minecraft.getInstance())).stream()
                .filter(bookmark -> bookmark.hidden == hiddenView)
                .filter(bookmark -> filter == 0 || FILTERS[filter].equals(bookmark.dimension))
                .filter(bookmark -> needle.isEmpty() || (bookmark.name + " " + bookmark.note + " " + friendly(bookmark.dimension)
                        + " " + coordinates(bookmark)).toLowerCase(Locale.ROOT).contains(needle)).toList();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        WorldNotesTheme.background(graphics, width, height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.nextStratum();
        graphics.centeredText(font, getTitle(), width / 2, 10, WorldNotesTheme.TEXT);
        for (int index = 0; index < rows.size(); index++) {
            RowLayout row = rows.get(index);
            Bookmark bookmark = row.bookmark();
            int accent = WorldNotesClient.isTracked(bookmark) ? WorldNotesTheme.ACCENT : WorldNotesTheme.dimensionAccent(bookmark.dimension);
            WorldNotesTheme.panel(graphics, left, row.y(), cardWidth, cardHeight, accent);
            if (cards.get(index).isFocused() || mouseX >= left && mouseX < left + cardWidth && mouseY >= row.y() && mouseY < row.y() + cardHeight)
                graphics.outline(left, row.y(), cardWidth, cardHeight, accent);
            graphics.fakeItem(new ItemStack(WorldNotesClient.isTracked(bookmark) ? Items.COMPASS : Items.FILLED_MAP), left + 5, row.y() + 5);
            int actionY = row.y() + (cardHeight - 20) / 2;
            graphics.fakeItem(new ItemStack(Items.BOOK), left + cardWidth + 6, actionY + 2);
            graphics.fakeItem(new ItemStack(hiddenView ? Items.ENDER_EYE : Items.ENDER_PEARL), left + cardWidth + 30, actionY + 2);
            int textX = left + 25, textWidth = cardWidth - 31, y = row.y() + 5;
            if ("black".equals(bookmark.gradient)) graphics.fill(textX - 1, y - 1, left + cardWidth - 5, y + 9, 0xFFCAD3DB);
            graphics.text(font, font.split(bookmark.displayName(), textWidth).getFirst(), textX, y, WorldNotesTheme.TEXT, false);
            y += 11;
            if (cardHeight >= 50 && !bookmark.note.isBlank()) {
                graphics.text(font, font.plainSubstrByWidth(bookmark.note, textWidth), textX, y, WorldNotesTheme.MUTED, false);
                y += 11;
            }
            graphics.text(font, font.plainSubstrByWidth(coordinates(bookmark), textWidth), textX, y, WorldNotesTheme.TEXT, false);
            graphics.text(font, font.split(dimensionComponent(bookmark.dimension), textWidth).getFirst(), textX, y + 11, accent, false);
        }
        if (!hiddenView) graphics.fakeItem(new ItemStack(Items.BARRIER), left + 2, height - 24);
        graphics.fakeItem(new ItemStack(Items.COMPARATOR), left + 2, height - 48);
        if (rows.isEmpty()) {
            var lines = font.split(Component.translatable(query.isBlank() ? "worldnotes.empty" : "worldnotes.no_results"), panelWidth - 8);
            int firstRow = hiddenView ? 73 : 98;
            int y = firstRow + Math.max(0, (height - 70 - firstRow - lines.size() * 11) / 2);
            for (var line : lines) {
                graphics.text(font, line, width / 2 - font.width(line) / 2, y, WorldNotesTheme.MUTED, false);
                y += 11;
            }
        }
        Component footer = error.getString().isEmpty() ? Component.translatable("worldnotes.page", page + 1, pageCount) : error;
        graphics.centeredText(font, footer, width / 2, height - 38, error.getString().isEmpty() ? WorldNotesTheme.MUTED : 0xFFFF8C83);
    }

    boolean isHiddenView() { return hiddenView; }
    void rebuild() { WorldNotesClient.setScreen(minecraft, new BookmarkScreen(page, filter, hiddenView, query)); }
    @Override
    public void onClose() { if (hiddenView) WorldNotesClient.setScreen(minecraft, new BookmarkScreen()); else super.onClose(); }

    private void addCurrent() {
        if (minecraft.player == null || minecraft.level == null) return;
        Bookmark bookmark = new Bookmark(WorldNotesClient.profile(minecraft), "New bookmark", "", WorldNotesClient.dimension(minecraft.level),
                Bookmark.roundToHundredth(minecraft.player.getX()), Bookmark.roundToHundredth(minecraft.player.getY()), Bookmark.roundToHundredth(minecraft.player.getZ()));
        WorldNotesClient.setScreen(minecraft, new BookmarkEditorScreen(this, bookmark));
    }

    private boolean canTeleport(Bookmark bookmark) {
        if (minecraft.player == null || minecraft.level == null || !bookmark.validCoordinates() || !Bookmark.validDimension(bookmark.dimension)) return false;
        var root = minecraft.player.connection.getCommands().getRoot();
        if (root.getChild("tp") == null) return false;
        return bookmark.dimension.equals(WorldNotesClient.dimension(minecraft.level))
                || root.getChild("execute") != null && root.getChild("execute").getChild("in") != null;
    }
    private void teleport(Bookmark bookmark) {
        if (!canTeleport(bookmark)) return;
        String position = String.format(Locale.ROOT, "%.2f %.2f %.2f", bookmark.x, bookmark.y, bookmark.z);
        String command = bookmark.dimension.equals(WorldNotesClient.dimension(minecraft.level)) ? "tp @s " + position
                : "execute in " + bookmark.dimension + " run tp @s " + position;
        minecraft.player.connection.sendCommand(command);
        WorldNotesGui.chat(minecraft, Component.translatable("worldnotes.teleport_requested", bookmark.displayName()));
        onClose();
    }
    private void confirmUnhide(Bookmark bookmark) {
        WorldNotesClient.setScreen(minecraft, new ConfirmScreen(confirmed -> {
            if (confirmed) {
                bookmark.hidden = false;
                if (!BookmarkStore.save(bookmark)) {
                    error = Component.translatable("worldnotes.save_failed");
                    WorldNotesClient.setScreen(minecraft, this);
                } else rebuild();
            } else WorldNotesClient.setScreen(minecraft, this);
        }, Component.translatable("worldnotes.unhide_confirm.title"), Component.translatable("worldnotes.unhide_confirm.message"),
                Component.translatable("worldnotes.unhide_confirm.confirm"), Component.translatable("worldnotes.unhide_confirm.cancel")));
    }
    private record RowLayout(Bookmark bookmark, int y) { }
    static String coordinates(Bookmark bookmark) { return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", bookmark.x, bookmark.y, bookmark.z); }
    static String friendly(String dimension) {
        return switch (dimension) { case "all" -> "All dimensions"; case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether"; case "minecraft:the_end" -> "The End"; default -> dimension; };
    }
    static Component dimensionComponent(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> Bookmark.gradientText(friendly(dimension), 0x56BC79, 0xB1F0C4);
            case "minecraft:the_nether" -> Bookmark.gradientText(friendly(dimension), 0xFFBB62, 0xF56877);
            case "minecraft:the_end" -> Bookmark.gradientText(friendly(dimension), 0xAD7FEA, 0xE4CDFF);
            default -> Bookmark.gradientText(friendly(dimension), 0x6DD7EC, 0xB7CEE8);
        };
    }
}
