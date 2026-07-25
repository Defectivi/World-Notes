package dev.migzb.worldnotes;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

public final class BookmarkEditorScreen extends Screen {
    private static final String[] DIMENSIONS = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"};
    private final BookmarkScreen parent;
    private final Bookmark bookmark;
    private final boolean readOnly;
    private EditBox name;
    private EditBox note;
    private EditBox x;
    private EditBox y;
    private EditBox z;
    private int dimensionIndex;

    public BookmarkEditorScreen(BookmarkScreen parent, Bookmark bookmark) {
        this(parent, bookmark, false);
    }

    public BookmarkEditorScreen(BookmarkScreen parent, Bookmark bookmark, boolean readOnly) {
        super(Component.literal(readOnly ? "Bookmark details" : "Edit bookmark"));
        this.parent = parent;
        this.bookmark = bookmark;
        this.readOnly = readOnly;
        for (int i = 0; i < DIMENSIONS.length; i++) if (DIMENSIONS[i].equals(bookmark.dimension)) dimensionIndex = i;
    }

    @Override
    protected void init() {
        int left = width / 2 - 100;
        name = field(left, 40, 200, bookmark.name, "Name");
        note = field(left, 70, 200, bookmark.note, "Note (optional)");
        x = field(left, 125, 62, Double.toString(bookmark.x), "X");
        y = field(left + 69, 125, 62, Double.toString(bookmark.y), "Y");
        z = field(left + 138, 125, 62, Double.toString(bookmark.z), "Z");
        name.setEditable(!readOnly);
        note.setEditable(!readOnly);
        x.setEditable(!readOnly);
        y.setEditable(!readOnly);
        z.setEditable(!readOnly);
        Button dimension = addRenderableWidget(Button.builder(Component.literal("Dimension: " + BookmarkScreen.friendly(DIMENSIONS[dimensionIndex])), button -> {
            dimensionIndex = (dimensionIndex + 1) % DIMENSIONS.length;
            minecraft.gui.setScreen(new BookmarkEditorScreen(parent, copyFromFields()));
        }).bounds(left, 95, 200, 20).build());
        dimension.setTooltip(Tooltip.create(Component.translatable("worldnotes.dimension.tooltip")));
        dimension.active = !readOnly;
        Button color = addRenderableWidget(Button.builder(Component.literal("Name color: " + bookmark.gradientLabel()), button -> {
            copyFromFields();
            bookmark.cycleGradient();
            minecraft.gui.setScreen(new BookmarkEditorScreen(parent, bookmark));
        }).bounds(left, 175, 200, 20).build());
        color.setTooltip(Tooltip.create(Component.translatable("worldnotes.color.tooltip")));
        color.active = !readOnly;
        boolean tracked = WorldNotesClient.isTracked(bookmark);
        boolean canTrack = WorldNotesClient.canTrack(bookmark);
        Component trackingTooltip = tracked
                ? Component.literal("Stop tracking this bookmark")
                : canTrack
                ? Component.literal("Track this bookmark")
                : Component.translatable("worldnotes.track_blocked");
        Button tracking = addRenderableWidget(Button.builder(
                        Component.literal(tracked ? "Stop tracking" : "Track"), button -> toggleTracking())
                .tooltip(Tooltip.create(trackingTooltip))
                .bounds(left, 150, 200, 20).build());
        tracking.active = canTrack;
        Button save = addRenderableWidget(Button.builder(Component.translatable("worldnotes.save"), button -> save())
                .tooltip(Tooltip.create(Component.translatable("worldnotes.save.tooltip")))
                .bounds(left, height - 45, 64, 20).build());
        Button delete = addRenderableWidget(Button.builder(Component.translatable("worldnotes.delete"), button -> confirmDelete())
                .tooltip(Tooltip.create(Component.translatable("worldnotes.delete.tooltip")))
                .bounds(left + 68, height - 45, 64, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("worldnotes.cancel"), button -> minecraft.gui.setScreen(parent))
                .tooltip(Tooltip.create(Component.translatable("worldnotes.cancel.tooltip")))
                .bounds(left + 136, height - 45, 64, 20).build());
        save.active = !readOnly;
        delete.active = !readOnly;
    }

    private EditBox field(int x, int y, int width, String value, String hint) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.literal(hint));
        field.setValue(value);
        field.setHint(Component.literal(hint));
        addRenderableWidget(field);
        return field;
    }

    private Bookmark copyFromFields() {
        bookmark.name = name.getValue();
        bookmark.note = note.getValue();
        bookmark.dimension = DIMENSIONS[dimensionIndex];
        bookmark.x = Bookmark.roundToHundredth(number(x.getValue(), bookmark.x));
        bookmark.y = Bookmark.roundToHundredth(number(y.getValue(), bookmark.y));
        bookmark.z = Bookmark.roundToHundredth(number(z.getValue(), bookmark.z));
        return bookmark;
    }

    private void save() {
        Bookmark saved = copyFromFields();
        if (saved.name.isBlank()) saved.name = "Untitled bookmark";
        saved.profile = WorldNotesClient.profile(minecraft);
        BookmarkStore.save(saved);
        parent.rebuild();
    }

    private void toggleTracking() {
        if (WorldNotesClient.isTracked(bookmark)) {
            WorldNotesClient.stopTracking();
        } else {
            Bookmark saved = copyFromFields();
            if (saved.name.isBlank()) saved.name = "Untitled bookmark";
            saved.profile = WorldNotesClient.profile(minecraft);
            BookmarkStore.save(saved);
            WorldNotesClient.track(saved);
        }
        minecraft.gui.setScreen(new BookmarkEditorScreen(parent, bookmark, readOnly));
    }

    private void confirmDelete() {
        minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                if (WorldNotesClient.isTracked(bookmark)) WorldNotesClient.stopTracking();
                BookmarkStore.delete(bookmark);
                parent.rebuild();
            } else {
                minecraft.gui.setScreen(this);
            }
        }, Component.translatable("worldnotes.delete_confirm.title"),
                Component.translatable("worldnotes.delete_confirm.message", bookmark.displayName()),
                Component.translatable("worldnotes.delete_confirm.delete"),
                Component.translatable("worldnotes.delete_confirm.cancel")));
    }

    private static double number(String value, double fallback) {
        try { return Double.parseDouble(value); } catch (NumberFormatException ignored) { return fallback; }
    }
}
