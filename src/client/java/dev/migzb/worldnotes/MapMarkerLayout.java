package dev.migzb.worldnotes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Screen-space grouping preserves every observation without painting overlapping squares. */
final class MapMarkerLayout {
    enum Kind {
        ITEM("Items", 0xFFFFD96B), NEUTRAL("Neutral", 0xFF86E8A1),
        HOSTILE("Hostile", 0xFFFF6B75), PLAYER("Players", 0xFF6DDFFF);
        final String label;
        final int color;
        Kind(String label, int color) { this.label = label; this.color = color; }
    }
    record Marker(int x, int z, Kind kind, String description) { }
    record Group(int x, int z, List<Marker> members) {
        int count(Kind kind) { return (int) members.stream().filter(marker -> marker.kind == kind).count(); }
        String summary() {
            if (members.size() == 1) return members.getFirst().description;
            List<String> counts = new ArrayList<>();
            for (Kind kind : Kind.values()) if (count(kind) > 0) counts.add(kind.label + ": " + count(kind));
            return String.join(" | ", counts);
        }
    }
    static List<Group> group(List<Marker> markers, int cellSize, int mapSize) {
        Map<Long, List<Marker>> cells = new LinkedHashMap<>();
        for (Marker marker : markers) {
            if (marker.x < 4 || marker.z < 4 || marker.x >= mapSize - 4 || marker.z >= mapSize - 4) continue;
            long key = ((long) (marker.x / cellSize) << 32) | (marker.z / cellSize & 0xFFFFFFFFL);
            cells.computeIfAbsent(key, ignored -> new ArrayList<>()).add(marker);
        }
        List<Group> result = new ArrayList<>();
        cells.forEach((key, members) -> {
            // Single entities retain their exact location. Crowded cells get one shared marker.
            int x = members.size() == 1 ? members.getFirst().x : (int) (key >> 32) * cellSize + cellSize / 2;
            int z = members.size() == 1 ? members.getFirst().z : (int) (long) key * cellSize + cellSize / 2;
            result.add(new Group(Math.clamp(x, 4, mapSize - 5), Math.clamp(z, 4, mapSize - 5), List.copyOf(members)));
        });
        return List.copyOf(result);
    }
}
