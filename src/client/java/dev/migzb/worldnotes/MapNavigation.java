package dev.migzb.worldnotes;

final class MapNavigation {
    static int zoom(int current,int steps) {
        return 1 << (int)Math.clamp((long)Integer.numberOfTrailingZeros(current)+steps,0,4);
    }
    /** Finish the visible square before surveying corners and off-screen history. */
    static java.util.List<int[]> terrainOffsets(int radius,int zoom) {
        int visible=(radius+zoom-1)/zoom;
        java.util.List<int[]> result=new java.util.ArrayList<>();
        for(int z=-radius;z<=radius;z++)for(int x=-radius;x<=radius;x++)result.add(new int[]{x,z});
        result.sort(java.util.Comparator.<int[]>comparingInt(a->Math.max(Math.abs(a[0]),Math.abs(a[1]))<=visible?0:1)
                .thenComparingInt(a->a[0]*a[0]+a[1]*a[1]));
        return java.util.List.copyOf(result);
    }
    private static final String[] DIRECTIONS = {"South", "Southwest", "West", "Northwest", "North", "Northeast", "East", "Southeast"};
    static String facing(float yaw) { return DIRECTIONS[Math.floorMod((int)Math.round(yaw / 45.0), 8)]; }
    static boolean inRange(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ, int radius) {
        return Math.abs((long)chunkX-playerChunkX)<=radius && Math.abs((long)chunkZ-playerChunkZ)<=radius;
    }
    /** Arrow silhouette in a forward/right basis; yaw zero points south (+Z). */
    static boolean arrowPixel(int x, int z, float yaw) {
        double radians = Math.toRadians(yaw), forward=-Math.sin(radians)*x+Math.cos(radians)*z;
        double sideways=Math.cos(radians)*x+Math.sin(radians)*z;
        return forward>=-4 && forward<=7 && Math.abs(sideways)<=
                (forward>=0 ? (7-forward)*0.65+0.4 : 1.25);
    }
}
