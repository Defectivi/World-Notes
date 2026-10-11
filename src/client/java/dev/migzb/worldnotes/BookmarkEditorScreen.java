package dev.migzb.worldnotes;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class BookmarkEditorScreen extends Screen {
    private final BookmarkScreen parent;
    private final Bookmark bookmark;
    private final boolean readOnly;
    private final boolean createdHere;
    private final List<String> dimensions = new ArrayList<>(List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"));
    private EditBox name, note, x, y, z;
    private Checkbox hidden;
    private Button tracking;
    private int dimensionIndex;
    private int left, panelWidth, top, gap;
    private Component error = Component.empty();
    private String draftX, draftY, draftZ;

    public BookmarkEditorScreen(BookmarkScreen parent, Bookmark bookmark) { this(parent, bookmark, false); }
    public BookmarkEditorScreen(BookmarkScreen parent, Bookmark bookmark, boolean readOnly) {
        super(Component.translatable(readOnly ? "worldnotes.details.title" : "worldnotes.editor.title"));
        this.parent = parent;
        this.bookmark = bookmark.copy();
        this.readOnly = readOnly;
        createdHere = !BookmarkStore.contains(bookmark);
        if (!dimensions.contains(bookmark.dimension)) dimensions.add(bookmark.dimension);
        dimensionIndex = dimensions.indexOf(bookmark.dimension);
        draftX = Double.toString(bookmark.x);
        draftY = Double.toString(bookmark.y);
        draftZ = Double.toString(bookmark.z);
    }

    @Override
    protected void init() {
        // Capture draft values before a window resize recreates widgets.
        if (name != null) captureText();
        panelWidth = Math.min(300, width - 24);
        left = (width - panelWidth) / 2;
        top = 34;
        gap = Math.min(30, Math.max(18, (height - 70) / 6));
        int fieldHeight = Math.min(20, gap - 2);
        name = field(left + 8, top, panelWidth - 16, bookmark.name, "worldnotes.name", 96, fieldHeight);
        note = field(left + 8, top + gap, panelWidth - 16, bookmark.note, "worldnotes.note", 2048, fieldHeight);
        Button dimension = addRenderableWidget(Button.builder(dimensionLabel(), button -> {
            dimensionIndex = (dimensionIndex + 1) % dimensions.size();
            bookmark.dimension = dimensions.get(dimensionIndex);
            button.setMessage(dimensionLabel());
        }).bounds(left + 8, top + gap * 2, panelWidth - 16, fieldHeight).build());
        dimension.active = !readOnly;
        dimension.setTooltip(Tooltip.create(Component.translatable("worldnotes.dimension.tooltip")));
        int coordinateWidth = (panelWidth - 24) / 3;
        int coordinateY = top + gap * 3 + (gap >= 26 ? 8 : 0);
        int coordinateHeight = gap >= 26 ? gap - 10 : fieldHeight;
        x = field(left + 8, coordinateY, coordinateWidth, draftX, "X", 32, coordinateHeight);
        y = field(left + 12 + coordinateWidth, coordinateY, coordinateWidth, draftY, "Y", 32, coordinateHeight);
        z = field(left + 16 + coordinateWidth * 2, coordinateY, coordinateWidth, draftZ, "Z", 32, coordinateHeight);
        int half = (panelWidth - 20) / 2;
        tracking = addRenderableWidget(Button.builder(trackingLabel(), button -> toggleTracking())
                .bounds(left + 8, top + gap * 4, half, fieldHeight).build());
        tracking.active = !createdHere && WorldNotesClient.canTrack(bookmark);
        tracking.setTooltip(Tooltip.create(Component.translatable(createdHere ? "worldnotes.track_save_first" : "worldnotes.track.tooltip")));
        Button color = addRenderableWidget(Button.builder(colorLabel(), button -> {
            bookmark.cycleGradient();
            button.setMessage(colorLabel());
        }).bounds(left + 12 + half, top + gap * 4, half, fieldHeight).build());
        color.active = !readOnly;
        hidden = addRenderableWidget(Checkbox.builder(Component.translatable("worldnotes.hide_from_main"), font)
                .pos(left + 8, top + gap * 5).selected(bookmark.hidden)
                .onValueChange((checkbox, selected) -> bookmark.hidden = selected).build());
        hidden.active = !readOnly && !parent.isHiddenView();
        int footer = height - 25;
        int buttonWidth = (panelWidth - 24) / 3;
        if (!readOnly) addRenderableWidget(Button.builder(Component.translatable("worldnotes.save"), button -> save())
                .bounds(left + 8, footer, buttonWidth, 20).build());
        if (!readOnly && !createdHere) addRenderableWidget(Button.builder(Component.translatable("worldnotes.delete"), button -> confirmDelete())
                .bounds(left + 12 + buttonWidth, footer, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(readOnly ? "gui.back" : "worldnotes.cancel"), button -> onClose())
                .bounds(left + panelWidth - 8 - buttonWidth, footer, buttonWidth, 20).build());
    }

    private EditBox field(int x, int y, int width, String value, String label, int maximum, int height) {
        Component description = label.length() == 1 ? Component.literal(label) : Component.translatable(label);
        EditBox field = new EditBox(font, x, y, width, height, description);
        field.setMaxLength(Math.max(maximum, value.length()));
        field.setValue(value);
        field.setHint(description);
        field.setTooltip(Tooltip.create(description));
        field.setEditable(!readOnly);
        addRenderableWidget(field);
        return field;
    }

    private void captureText() {
        bookmark.name = name.getValue();
        bookmark.note = note.getValue();
        bookmark.hidden = hidden.selected();
        draftX = x.getValue();
        draftY = y.getValue();
        draftZ = z.getValue();
    }

    private void save() {
        captureText();
        try {
            bookmark.x = Bookmark.roundToHundredth(Double.parseDouble(x.getValue().trim()));
            bookmark.y = Bookmark.roundToHundredth(Double.parseDouble(y.getValue().trim()));
            bookmark.z = Bookmark.roundToHundredth(Double.parseDouble(z.getValue().trim()));
            if (!bookmark.validCoordinates()) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            error = Component.translatable("worldnotes.invalid_coordinates");
            return;
        }
        bookmark.profile = WorldNotesClient.profile(minecraft);
        if (!BookmarkStore.save(bookmark)) {
            error = Component.translatable("worldnotes.save_failed");
            return;
        }
        parent.rebuild();
    }

    private void toggleTracking() {
        if (WorldNotesClient.isTracked(bookmark)) WorldNotesClient.stopTracking();
        else {
            Bookmark saved = BookmarkStore.find(bookmark.profile, bookmark.id);
            if (saved != null) WorldNotesClient.track(saved);
        }
        tracking.setMessage(trackingLabel());
    }

    private Component trackingLabel() { return Component.translatable(WorldNotesClient.isTracked(bookmark) ? "worldnotes.stop_tracking" : "worldnotes.track"); }
    private Component dimensionLabel() { return Component.translatable("worldnotes.filter", BookmarkScreen.dimensionComponent(dimensions.get(dimensionIndex))); }
    private Component colorLabel() { return Component.translatable("worldnotes.name_color", bookmark.gradientLabel()); }

    private void confirmDelete() {
        WorldNotesClient.setScreen(minecraft, new ConfirmScreen(confirmed -> {
            if (confirmed) {
                if (!BookmarkStore.delete(bookmark)) {
                    error = Component.translatable("worldnotes.save_failed");
                    WorldNotesClient.setScreen(minecraft, this);
                    return;
                }
                if (WorldNotesClient.isTracked(bookmark)) WorldNotesClient.stopTracking();
                parent.rebuild();
            } else WorldNotesClient.setScreen(minecraft, this);
        }, Component.translatable("worldnotes.delete_confirm.title"),
                Component.translatable("worldnotes.delete_confirm.message", bookmark.displayName()),
                Component.translatable("worldnotes.delete_confirm.delete"), Component.translatable("worldnotes.delete_confirm.cancel")));
    }

    @Override
    public void onClose() { WorldNotesClient.setScreen(minecraft, parent); }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        WorldNotesTheme.background(graphics, width, height);
        WorldNotesTheme.panel(graphics, left, 26, panelWidth, height - 59, WorldNotesTheme.dimensionAccent(bookmark.dimension));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, getTitle(), width / 2, 10, WorldNotesTheme.TEXT);
        if (gap >= 26) {
            graphics.text(font, "X", x.getX() + 2, top + gap * 3 - 1, WorldNotesTheme.MUTED, false);
            graphics.text(font, "Y", y.getX() + 2, top + gap * 3 - 1, WorldNotesTheme.MUTED, false);
            graphics.text(font, "Z", z.getX() + 2, top + gap * 3 - 1, WorldNotesTheme.MUTED, false);
        }
        graphics.centeredText(font, font.plainSubstrByWidth(error.getString(), width - 16), width / 2, height - 37, 0xFFFF8C83);
    }
}
