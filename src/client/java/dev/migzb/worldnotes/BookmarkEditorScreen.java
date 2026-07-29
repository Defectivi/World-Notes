package dev.migzb.worldnotes;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
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
    private final boolean createdHere;
    private EditBox name;
    private EditBox note;
    private EditBox x;
    private EditBox y;
    private EditBox z;
    private Checkbox hidden;
    private int dimensionIndex;

    public BookmarkEditorScreen(BookmarkScreen parent, Bookmark bookmark) {
        this(parent, bookmark, false);
    }

    public BookmarkEditorScreen(BookmarkScreen parent, Bookmark bookmark, boolean readOnly) {
        this(parent, bookmark, readOnly, !BookmarkStore.contains(bookmark));
    }

    private BookmarkEditorScreen(BookmarkScreen parent, Bookmark bookmark, boolean readOnly, boolean createdHere) {
        super(Component.literal(readOnly ? "Bookmark details" : "Edit bookmark"));
        this.parent = parent;
        this.bookmark = bookmark;
        this.readOnly = readOnly;
        this.createdHere = createdHere;
        for (int i = 0; i < DIMENSIONS.length; i++) if (DIMENSIONS[i].equals(bookmark.dimension)) dimensionIndex = i;
    }

    @Override
    protected void init() {
        int left = width / 2 - 100;
        name = field(left, 40, 200, bookmark.name, "Name");
        note = field(left, 70, 200, bookmark.note, "Note (optional)");
        x = field(left, 135, 62, Double.toString(bookmark.x), "X");
        y = field(left + 69, 135, 62, Double.toString(bookmark.y), "Y");
        z = field(left + 138, 135, 62, Double.toString(bookmark.z), "Z");
        name.setEditable(!readOnly);
        note.setEditable(!readOnly);
        x.setEditable(!readOnly);
        y.setEditable(!readOnly);
        z.setEditable(!readOnly);
        Button dimension = addRenderableWidget(Button.builder(Component.literal("Dimension: " + BookmarkScreen.friendly(DIMENSIONS[dimensionIndex])), button -> {
            dimensionIndex = (dimensionIndex + 1) % DIMENSIONS.length;
            WorldNotesClient.setScreen(minecraft, new BookmarkEditorScreen(parent, copyFromFields(), readOnly, createdHere));
        }).bounds(left, 95, 200, 20).build());
        dimension.setTooltip(Tooltip.create(Component.translatable("worldnotes.dimension.tooltip")));
        dimension.active = !readOnly;
        hidden = addRenderableWidget(Checkbox.builder(Component.translatable("worldnotes.hide_from_main"), font)
                .pos(left, 115)
                .selected(bookmark.hidden)
                .tooltip(Tooltip.create(Component.translatable("worldnotes.hide_from_main.tooltip")))
                .onValueChange((checkbox, selected) -> { })
                .build());
        hidden.active = !readOnly && !parent.isHiddenView();
        Button color = addRenderableWidget(Button.builder(Component.literal("Name color: " + bookmark.gradientLabel()), button -> {
            copyFromFields();
            bookmark.cycleGradient();
            WorldNotesClient.setScreen(minecraft, new BookmarkEditorScreen(parent, bookmark, readOnly, createdHere));
        }).bounds(left, 185, 200, 20).build());
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
                .bounds(left, 160, 200, 20).build());
        tracking.active = canTrack;
        Button save = addRenderableWidget(Button.builder(Component.translatable("worldnotes.save"), button -> save())
                .tooltip(Tooltip.create(Component.translatable("worldnotes.save.tooltip")))
                .bounds(left, height - 25, 64, 20).build());
        Button delete = addRenderableWidget(Button.builder(Component.translatable("worldnotes.delete"), button -> confirmDelete())
                .tooltip(Tooltip.create(Component.translatable("worldnotes.delete.tooltip")))
                .bounds(left + 68, height - 25, 64, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("worldnotes.cancel"), button -> cancel())
                .tooltip(Tooltip.create(Component.translatable("worldnotes.cancel.tooltip")))
                .bounds(left + 136, height - 25, 64, 20).build());
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
        bookmark.hidden = hidden.selected();
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
            if (!createdHere) BookmarkStore.save(saved);
            WorldNotesClient.track(saved);
        }
        WorldNotesClient.setScreen(minecraft, new BookmarkEditorScreen(parent, bookmark, readOnly, createdHere));
    }

    private void confirmDelete() {
        WorldNotesClient.setScreen(minecraft, new ConfirmScreen(confirmed -> {
            if (confirmed) {
                if (WorldNotesClient.isTracked(bookmark)) WorldNotesClient.stopTracking();
                BookmarkStore.delete(bookmark);
                parent.rebuild();
            } else {
                WorldNotesClient.setScreen(minecraft, this);
            }
        }, Component.translatable("worldnotes.delete_confirm.title"),
                Component.translatable("worldnotes.delete_confirm.message", bookmark.displayName()),
                Component.translatable("worldnotes.delete_confirm.delete"),
                Component.translatable("worldnotes.delete_confirm.cancel")));
    }

    private void cancel() {
        if (createdHere) {
            if (WorldNotesClient.isTracked(bookmark)) WorldNotesClient.stopTracking();
            if (BookmarkStore.contains(bookmark)) BookmarkStore.delete(bookmark);
        }
        WorldNotesClient.setScreen(minecraft, parent);
    }

    private static double number(String value, double fallback) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
