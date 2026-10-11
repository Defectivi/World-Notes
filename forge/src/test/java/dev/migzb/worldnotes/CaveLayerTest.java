package dev.migzb.worldnotes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CaveLayerTest {
    private static final int FLOOR=0xFF738491;
    private CaveLayer.Terrain rooms=new CaveLayer.Terrain() {
        public int space(int x,int y,int z) {
            return Math.abs(z)<=2 && y>=4&&y<=7 && ((x>=-5&&x<=0)||(x>=2&&x<=5))?1:0;
        }
        public int color(int x,int y,int z){return FLOOR;}
    };
    private void finish(CaveLayer layer,CaveLayer.Terrain terrain) {layer.advance(terrain,100000,Long.MAX_VALUE);}
    private CaveLayer.Terrain corridor=new CaveLayer.Terrain() {
        public int space(int x,int y,int z){return z==0&&y>=4&&y<=7?1:0;}
        public int color(int x,int y,int z){return 0xFF000000|(x&255);}
    };
    @Test void walkingAheadOfTheScanLinksToDiscoveredTerrainWithoutRestarting() {
        var layer=new CaveLayer(0,4,0,2);layer.advance(corridor,1,Long.MAX_VALUE);
        assertTrue(layer.needsReseed(3,4,0));
        assertFalse(layer.followPlayer(corridor,3,4,0,Long.MAX_VALUE));
        assertTrue(layer.contains(0,4,0));assertTrue(layer.contains(3,4,0));
        finish(layer,corridor);assertTrue(layer.contains(-32,4,0));
    }
    @Test void aConfirmedDisconnectedCaveStillRequiresANewComponent() {
        var layer=new CaveLayer(-2,4,0,1);finish(layer,rooms);
        assertTrue(layer.followPlayer(rooms,3,4,0,Long.MAX_VALUE));
        assertFalse(layer.contains(3,4,0));
        assertFalse(layer.followPlayer(rooms,3,4,0,0));
    }
    @Test void recenteringPreservesPixelsAndContinuesIntoNewChunks() {
        var layer=new CaveLayer(0,4,0,2);finish(layer,corridor);
        var shifted=layer.recenter(17,0);
        assertTrue(shifted.contains(0,4,0));assertTrue(shifted.contains(32,7,0));
        assertFalse(shifted.contains(40,4,0));
        int[] pixel=new int[1];shifted.copyRegion(10,0,1,pixel);assertEquals(0xFF00000A,pixel[0]);
        finish(shifted,corridor);assertTrue(shifted.contains(49,4,0));
        var reverse=shifted.recenter(-1,0);finish(reverse,corridor);
        assertTrue(reverse.contains(-33,4,0));assertTrue(reverse.contains(10,4,0));
    }
    @Test void diagonalWindowShiftsDoNotLeakOtherCavesAcrossBitOrRowBoundaries() {
        var layer=new CaveLayer(-2,4,0,2);finish(layer,rooms);
        var shifted=layer.recenter(5,-7);finish(shifted,rooms);
        for(int x=-5;x<=0;x++)for(int y=4;y<=7;y++)for(int z=-2;z<=2;z++)assertTrue(shifted.contains(x,y,z));
        for(int x=2;x<=5;x++)for(int y=4;y<=7;y++)for(int z=-2;z<=2;z++)assertFalse(shifted.contains(x,y,z));
        assertTrue(shifted.changed(1,4,0,true,FLOOR));
    }
    @Test void recenteringRetainsPendingFrontiersWhileChunksArrive() {
        boolean[] loaded={false};
        CaveLayer.Terrain delayed=new CaveLayer.Terrain() {
            public int space(int x,int y,int z){return x>0&&!loaded[0]?-1:corridor.space(x,y,z);}
            public int color(int x,int y,int z){return FLOOR;}
        };
        var layer=new CaveLayer(0,4,0,2);finish(layer,delayed);
        var shifted=layer.recenter(17,0);assertTrue(shifted.contains(0,4,0));
        loaded[0]=true;shifted.retryUnknown();finish(shifted,delayed);
        assertTrue(shifted.contains(49,4,0));
    }
    @Test void recenteringKeepsQueuedWorkAndDoesNotTurnAnUnvisitedCenterIntoASeed() {
        var layer=new CaveLayer(0,4,0,2);layer.advance(corridor,2,Long.MAX_VALUE);
        var shifted=layer.recenter(17,0);
        assertTrue(shifted.needsReseed(17,4,0));
        finish(shifted,corridor);assertTrue(shifted.contains(49,4,0));
    }
    @Test void shiftingDenseMasksKeepsHeightAndWindowBoundariesExact() {
        CaveLayer.Terrain room=new CaveLayer.Terrain() {
            public int space(int x,int y,int z){return y>=4&&y<=7?1:0;}
            public int color(int x,int y,int z){return FLOOR;}
        };
        var layer=new CaveLayer(0,4,0,2);finish(layer,room);
        var shifted=layer.recenter(17,-7);
        for(int x=shifted.left;x<shifted.left+shifted.width;x++)
            for(int z=shifted.top;z<shifted.top+shifted.width;z++)
                for(int y=0;y<16;y++)assertEquals(x<=32&&z>=-32&&y>=4&&y<=7,shifted.contains(x,y,z));
    }
    @Test void seedSurvivesExpiredTickBudgetsAndDelayedChunkArrival() {
        var layer=new CaveLayer(-2,4,0,1);
        for(int tick=0;tick<20;tick++) {
            layer.advance(rooms,4096,0);
            assertFalse(layer.needsReseed(-2,4,0));
        }
        CaveLayer.Terrain missing=new CaveLayer.Terrain() {
            public int space(int x,int y,int z){return -1;}
            public int color(int x,int y,int z){return FLOOR;}
        };
        finish(layer,missing);assertFalse(layer.needsReseed(-2,4,0));
        layer.retryUnknown();finish(layer,rooms);assertTrue(layer.contains(-2,4,0));
    }
    @Test void queuedPlayerPositionsDoNotRestartAnIncompleteScan() {
        var layer=new CaveLayer(-2,4,0,1);
        layer.advance(rooms,1,Long.MAX_VALUE);
        assertFalse(layer.contains(-1,4,0));assertFalse(layer.needsReseed(-1,4,0));
        finish(layer,rooms);assertFalse(layer.needsReseed(-1,4,0));
        assertTrue(layer.needsReseed(3,4,0));
    }
    @Test void anInitiallyBlockedSeedStaysStableAndCanRecoverWhenItOpens() {
        var layer=new CaveLayer(1,4,0,1);finish(layer,rooms);
        assertFalse(layer.needsReseed(1,4,0));
        assertFalse(layer.changed(1,4,0,false,FLOOR));
        assertTrue(layer.changed(1,4,0,true,FLOOR));
    }
    @Test void repeatedNonBlockingNetherUpdatesKeepTheSameScanAndTopologyChangesInvalidateIt() {
        var layer=new CaveLayer(-2,4,0,1);finish(layer,rooms);
        long revision=layer.revision();
        for(int tick=0;tick<100;tick++) {
            assertFalse(layer.changed(-2,4,0,true,0xFFAA4422));
            assertFalse(layer.changed(-2,3,0,false,FLOOR));
        }
        assertEquals(revision,layer.revision());assertTrue(layer.contains(-5,7,2));
        assertTrue(layer.changed(-2,4,0,false,FLOOR));
        assertTrue(layer.changed(1,4,0,true,FLOOR));
        assertFalse(layer.changed(5,4,0,false,FLOOR));
    }
    @Test void floorColourUpdatesPatchTheMapWithoutDiscardingConnectivity() {
        var layer=new CaveLayer(-2,4,0,1);finish(layer,rooms);
        assertFalse(layer.changed(-2,3,0,false,0xFF884422));
        int[] pixel=new int[1];layer.copyRegion(-2,0,1,pixel);
        assertEquals(0xFF884422,pixel[0]);assertTrue(layer.contains(-5,7,2));
    }
    @Test void sealedNeighbouringCaveIsNotRevealed() {
        var layer=new CaveLayer(-2,4,0,1);finish(layer,rooms);
        assertTrue(layer.contains(-5,7,2));
        assertFalse(layer.contains(2,4,0));
        int[] pixels=new int[33*33];layer.copyRegion(-18,-16,33,pixels);
        assertEquals(FLOOR,pixels[16*33+16]);
        assertEquals(TerrainCache.UNKNOWN,pixels[16*33+20]);
    }
    @Test void changingTheSeedSelectsTheOtherCaveWithoutMergingHistory() {
        var layer=new CaveLayer(3,4,0,1);finish(layer,rooms);
        assertTrue(layer.contains(2,4,0));assertFalse(layer.contains(-2,4,0));
    }
    @Test void openingAndClosingAPassageRebuildsConnectivityFromThePlayer() {
        boolean[] open={false};
        CaveLayer.Terrain doorway=new CaveLayer.Terrain() {
            public int space(int x,int y,int z){return open[0]&&x==1&&y==4&&z==0?1:rooms.space(x,y,z);}
            public int color(int x,int y,int z){return FLOOR;}
        };
        var closed=new CaveLayer(-2,4,0,1);finish(closed,doorway);
        assertTrue(closed.touches(1,4,0));assertFalse(closed.touches(5,4,0));
        open[0]=true;
        var joined=new CaveLayer(-2,4,0,1);finish(joined,doorway);assertTrue(joined.contains(5,4,0));
        open[0]=false;
        var separated=new CaveLayer(-2,4,0,1);finish(separated,doorway);assertFalse(separated.contains(5,4,0));
    }
    @Test void negativeHeightAndStackedRoomsStayInTheirOwnLayer() {
        var layer=new CaveLayer(0,-17,0,1);
        assertEquals(-32,layer.minY);
        CaveLayer.Terrain vertical=new CaveLayer.Terrain() {
            public int space(int x,int y,int z){return x==0&&z==0&&y>=-30&&y<=3?1:0;}
            public int color(int x,int y,int z){return FLOOR;}
        };
        finish(layer,vertical);
        assertTrue(layer.contains(0,-30,0));assertFalse(layer.contains(0,-16,0));
        var next=new CaveLayer(0,-16,0,1);finish(next,vertical);
        assertTrue(next.contains(0,-16,0));assertFalse(next.contains(0,-17,0));
    }
    @Test void missingChunksResumeFromFrontierWithinIncrementalBudget() {
        boolean[] received={false};
        CaveLayer.Terrain delayed=new CaveLayer.Terrain() {
            public int space(int x,int y,int z){if(x>=0&&!received[0])return -1;return x>=-2&&x<=2&&z==0&&y==4?1:0;}
            public int color(int x,int y,int z){return FLOOR;}
        };
        var layer=new CaveLayer(-1,4,0,1);
        layer.advance(delayed,0,Long.MAX_VALUE);assertFalse(layer.contains(-1,4,0));
        layer.advance(delayed,1,Long.MAX_VALUE);assertTrue(layer.contains(-1,4,0));
        finish(layer,delayed);assertFalse(layer.contains(1,4,0));
        received[0]=true;layer.retryUnknown();finish(layer,delayed);
        assertTrue(layer.contains(2,4,0));
    }
}
