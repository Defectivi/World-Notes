package dev.migzb.worldnotes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapNavigationTest {
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
