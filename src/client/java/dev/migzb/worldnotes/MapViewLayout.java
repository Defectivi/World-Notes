package dev.migzb.worldnotes;

/** Keeps controls and hover details outside the terrain, including at large GUI scales. */
record MapViewLayout(int left, int top, int size, int legendX, int legendY, int legendWidth, boolean sidebar) {
    static MapViewLayout create(int width, int height) {
        boolean sidebar = width >= 400 && height >= 220;
        int size = Math.max(16, Math.min(width - (sidebar ? 174 : 28), height - (sidebar ? 66 : 108)));
        int left = sidebar ? Math.max(14, (width - 158 - size) / 2) : (width - size) / 2;
        return new MapViewLayout(left, 30, size, sidebar ? width - 142 : 10,
                sidebar ? 34 : height - 53, sidebar ? 132 : (width - 26) / 4, sidebar);
    }
}
