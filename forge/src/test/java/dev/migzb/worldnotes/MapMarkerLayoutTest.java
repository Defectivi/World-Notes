package dev.migzb.worldnotes;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class MapMarkerLayoutTest {
    @Test void crowdedMarkersPreserveKindsAndNamesInOneGroup() {
        List<MapMarkerLayout.Marker> markers = new ArrayList<>();
        for (int i = 0; i < 200; i++) markers.add(new MapMarkerLayout.Marker(51, 51,
                i % 2 == 0 ? MapMarkerLayout.Kind.HOSTILE : MapMarkerLayout.Kind.NEUTRAL, "mob " + i));
        var groups = MapMarkerLayout.group(markers, 22, 150);
        assertEquals(1, groups.size());
        assertEquals(200, groups.getFirst().members().size());
        assertEquals(100, groups.getFirst().count(MapMarkerLayout.Kind.HOSTILE));
        assertEquals(100, groups.getFirst().count(MapMarkerLayout.Kind.NEUTRAL));
        assertTrue(groups.getFirst().summary().contains("Hostile: 100"));
        assertEquals("mob 199", groups.getFirst().members().getLast().description());
    }
    @Test void solitaryMarkersKeepExactPositionsAndEdgesRemainInsideMap() {
        var groups = MapMarkerLayout.group(List.of(
                new MapMarkerLayout.Marker(9, 11, MapMarkerLayout.Kind.PLAYER, "Alex"),
                new MapMarkerLayout.Marker(95, 95, MapMarkerLayout.Kind.ITEM, "Diamond"),
                new MapMarkerLayout.Marker(94, 94, MapMarkerLayout.Kind.ITEM, "Coal"),
                new MapMarkerLayout.Marker(-1, 50, MapMarkerLayout.Kind.ITEM, "Outside")), 22, 100);
        assertEquals(2, groups.size());
        assertEquals(9, groups.getFirst().x()); assertEquals(11, groups.getFirst().z());
        assertEquals("Alex", groups.getFirst().summary());
        assertTrue(groups.getLast().x() <= 95); assertTrue(groups.getLast().z() <= 95);
    }
    @Test void mapAndControlsFitCompactAndWideWindowsWithoutOverlapping() {
        for (int[] window : List.of(new int[]{199, 210}, new int[]{320, 180}, new int[]{426, 240}, new int[]{800, 450})) {
            var layout = MapViewLayout.create(window[0], window[1]);
            assertTrue(layout.left() >= 0); assertTrue(layout.top() >= 0);
            assertTrue(layout.left() + layout.size() < window[0]);
            assertTrue(layout.top() + layout.size() < window[1] - 27);
            if (layout.sidebar()) assertTrue(layout.left() + layout.size() < layout.legendX());
            else assertTrue(layout.top() + layout.size() < layout.legendY());
            assertTrue(layout.legendX() + (layout.sidebar() ? layout.legendWidth() : 4 * layout.legendWidth() + 6) <= window[0]);
        }
    }
}
