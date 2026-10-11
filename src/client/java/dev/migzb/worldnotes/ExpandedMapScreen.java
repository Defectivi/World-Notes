package dev.migzb.worldnotes;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.List;

final class ExpandedMapScreen extends Screen {
    private MapViewLayout layout;
    private boolean saveFailed;
    private double wheelRemainder;
    ExpandedMapScreen() { super(Component.literal("World Notes map")); }

    private boolean enabled(MapMarkerLayout.Kind kind) {
        MinimapSettings s = WorldNotesMinimap.settings();
        return switch (kind) {
            case ITEM -> s.items; case NEUTRAL -> s.neutralMobs;
            case HOSTILE -> s.hostileMobs; case PLAYER -> s.players;
        };
    }
    private Component legend(MapMarkerLayout.Kind kind) {
        return Component.literal(kind.label).withStyle(style -> style.withColor(enabled(kind) ? kind.color & 0xFFFFFF : 0x858D99)
                .withStrikethrough(!enabled(kind)));
    }
    @Override protected void init() {
        layout = MapViewLayout.create(width, height);
        for (MapMarkerLayout.Kind kind : MapMarkerLayout.Kind.values()) {
            int index = kind.ordinal();
            addRenderableWidget(Button.builder(legend(kind), button -> {
                MinimapSettings draft = WorldNotesMinimap.settings().copy();
                switch (kind) {
                    case ITEM -> draft.items = !draft.items;
                    case NEUTRAL -> draft.neutralMobs = !draft.neutralMobs;
                    case HOSTILE -> draft.hostileMobs = !draft.hostileMobs;
                    case PLAYER -> draft.players = !draft.players;
                }
                saveFailed = !WorldNotesMinimap.apply(draft);
                button.setMessage(legend(kind));
            }).bounds(layout.legendX() + (layout.sidebar() ? 0 : index * (layout.legendWidth() + 2)),
                    layout.legendY() + (layout.sidebar() ? index * 22 : 0), layout.legendWidth(), 18)
                    .tooltip(Tooltip.create(Component.literal("Click to show/hide " + kind.label.toLowerCase(java.util.Locale.ROOT)))).build());
        }
        int buttonWidth = Math.min(100, (width - 28) / 3);
        addRenderableWidget(Button.builder(Component.literal("Settings"), b -> WorldNotesClient.setScreen(minecraft, new MinimapSettingsScreen(this)))
                .bounds(8, height - 27, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width - buttonWidth - 8, height - 27, buttonWidth, 20).build());
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (WorldNotesClient.mapKey(event)) { onClose(); return true; }
        if (WorldNotesClient.zoomKey(event)) { WorldNotesMinimap.zoom(minecraft); return true; }
        return super.keyPressed(event);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float partial) {
        WorldNotesTheme.background(g, width, height);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(x>=layout.left() && x<layout.left()+layout.size() && y>=layout.top() && y<layout.top()+layout.size()) {
            wheelRemainder+=vertical;
            int steps=(int)wheelRemainder;
            if(steps!=0){WorldNotesMinimap.zoom(minecraft,Math.clamp(steps,-4,4));wheelRemainder-=steps;}
            return true;
        }
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float partial) {
        super.extractRenderState(g, x, y, partial);
        if (minecraft.level == null || minecraft.player == null) { onClose(); return; }
        int left = layout.left(), top = layout.top(), size = layout.size();
        int center = left + size / 2;
        g.centeredText(font, BookmarkScreen.dimensionComponent(WorldNotesClient.dimension(minecraft.level)), center, 6, 0xFFFFFFFF);
        g.centeredText(font, Component.literal("Facing ").append(CompassColors.label(MapNavigation.facing(minecraft.player.getYRot()),WorldNotesClient.dimension(minecraft.level)))
                .append(" | " + WorldNotesMinimap.radius() + "ch"), center, 18, WorldNotesTheme.MUTED);
        g.centeredText(font,Component.literal(WorldNotesMinimap.settings().zoom+"x"),width/2,height-21,WorldNotesTheme.MUTED);
        g.fill(left - 2, top - 2, left + size + 2, top + size + 2, 0xFF778A9B);
        List<Component> hovered = WorldNotesMinimap.renderMap(g, minecraft, left, top, size, x, y, true);
        String hint = hovered.isEmpty() ? "Scroll to zoom | Terrain " + WorldNotesMinimap.coverage() + "%" : hovered.getFirst().getString();
        if (layout.sidebar()) {
            int textX = layout.legendX(), textY = layout.legendY() + 94;
            List<Component> help = List.of(Component.literal("Scroll to zoom"), Component.literal("Terrain: " + WorldNotesMinimap.coverage() + "%"),
                    Component.literal("Numbers: entity groups"), Component.literal(WorldNotesMinimap.settings().nearbyHeights ? "Nearby Y: +/-24 blocks" : "All heights shown"));
            for (Component line : help) {
                for (var wrapped : font.split(line, layout.legendWidth())) {
                    g.text(font, wrapped, textX, textY, WorldNotesTheme.MUTED, false); textY += 11;
                }
                textY += 3;
            }
        } else {
            g.centeredText(font, Component.literal(font.plainSubstrByWidth(hint, width - 20)), width / 2, height - 69, WorldNotesTheme.TEXT);
        }
        if (!hovered.isEmpty()) {
            java.util.List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>();
            for (Component line : hovered) lines.addAll(font.split(line, Math.min(240, width - 24)));
            g.setTooltipForNextFrame(font, lines, x, y);
        }
        if (saveFailed) g.centeredText(font, Component.translatable("worldnotes.save_failed"), width / 2, height - 38, 0xFFFF8C83);
    }
    @Override public boolean isPauseScreen() { return false; }
}
