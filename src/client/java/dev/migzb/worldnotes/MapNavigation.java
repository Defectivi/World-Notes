package dev.migzb.worldnotes;

final class MapNavigation {
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
