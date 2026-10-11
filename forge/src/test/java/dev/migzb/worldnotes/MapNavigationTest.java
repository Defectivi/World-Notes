package dev.migzb.worldnotes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapNavigationTest {
    @Test void scrollZoomClampsWithoutWrappingOrOverflow() {
        assertEquals(4,MapNavigation.zoom(2,1));
        assertEquals(2,MapNavigation.zoom(8,-2));
        assertEquals(16,MapNavigation.zoom(16,1));
        assertEquals(1,MapNavigation.zoom(1,-1));
        assertEquals(16,MapNavigation.zoom(8,Integer.MAX_VALUE));
        assertEquals(1,MapNavigation.zoom(8,Integer.MIN_VALUE));
    }
    @Test void scanPrioritizesVisibleCornersBeforeOffScreenChunksAndVisitsEveryChunkOnce() {
        var offsets=MapNavigation.terrainOffsets(16,4);
        assertEquals(1089,offsets.size());
        assertEquals(1089,offsets.stream().map(a->a[0]+","+a[1]).distinct().count());
        assertArrayEquals(new int[]{0,0},offsets.getFirst());
        assertTrue(offsets.subList(0,81).stream().allMatch(a->Math.abs(a[0])<=4&&Math.abs(a[1])<=4));
        assertTrue(offsets.subList(81,offsets.size()).stream().noneMatch(a->Math.abs(a[0])<=4&&Math.abs(a[1])<=4));
    }
    @Test void directionMatchesMinecraftYawAndWrapsCorrectly() {
        assertEquals("South",MapNavigation.facing(0));
        assertEquals("West",MapNavigation.facing(90));
        assertEquals("North",MapNavigation.facing(180));
        assertEquals("East",MapNavigation.facing(-90));
        assertEquals("Southwest",MapNavigation.facing(45));
        assertEquals("Northeast",MapNavigation.facing(-135));
        assertEquals("South",MapNavigation.facing(720));
    }
    @Test void arrowTipTurnsWithYawAndHasAVisibleShaft() {
        assertTrue(MapNavigation.arrowPixel(0,7,0));
        assertFalse(MapNavigation.arrowPixel(0,-7,0));
        assertTrue(MapNavigation.arrowPixel(-7,0,90));
        assertTrue(MapNavigation.arrowPixel(0,-7,180));
        assertTrue(MapNavigation.arrowPixel(7,0,-90));
        assertTrue(MapNavigation.arrowPixel(0,-3,0));
        assertFalse(MapNavigation.arrowPixel(5,-3,0));
    }
    @Test void terrainAndRadarShareTheRenderDistanceBoundaryIncludingNegativeChunks() {
        assertTrue(MapNavigation.inRange(-20,3,-12,-5,8));
        assertFalse(MapNavigation.inRange(-21,3,-12,-5,8));
        assertFalse(MapNavigation.inRange(-20,4,-12,-5,8));
        assertFalse(MapNavigation.inRange(Integer.MIN_VALUE,0,Integer.MAX_VALUE,0,32));
    }
}
