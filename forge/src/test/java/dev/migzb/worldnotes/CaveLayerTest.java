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
