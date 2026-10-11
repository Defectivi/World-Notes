package dev.migzb.worldnotes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NetherTerrainTest {
    private NetherTerrain.Blocks cavern=new NetherTerrain.Blocks() {
        public int space(int x,int y,int z){return y<=31||y>=120?0:1;}
        public int color(int x,int y,int z){return y>=120?0xFF111111:0xFF884422;}
    };
    @Test void globalNetherRoofDoesNotMakeOpenTerrainACave() {
        assertFalse(NetherTerrain.cave(cavern,0,64,0,120,false));
        assertFalse(NetherTerrain.cave(cavern,0,115,0,120,false));
        assertFalse(NetherTerrain.cave(cavern,0,128,0,120,false));
    }
    @Test void localRoofAndEnclosingWallsStillIdentifyNetherTunnels() {
        NetherTerrain.Blocks tunnel=new NetherTerrain.Blocks() {
            public int space(int x,int y,int z){return y<=63||y>=68||Math.abs(z)>=2||x<=-6?0:1;}
            public int color(int x,int y,int z){return 1;}
        };
        assertTrue(NetherTerrain.cave(tunnel,0,64,0,120,false));
        NetherTerrain.Blocks throughTunnel=new NetherTerrain.Blocks() {
            public int space(int x,int y,int z){return y<=63||y>=68||Math.abs(z)>=2?0:1;}
            public int color(int x,int y,int z){return 1;}
        };
        assertTrue(NetherTerrain.cave(throughTunnel,0,64,0,120,false));
        NetherTerrain.Blocks lowRoof=new NetherTerrain.Blocks() {
            public int space(int x,int y,int z){return y>=68?0:1;}
            public int color(int x,int y,int z){return 1;}
        };
        assertFalse(NetherTerrain.cave(lowRoof,0,64,0,120,false));
    }
    @Test void missingNeighbourChunksRetainThePreviousClassification() {
        NetherTerrain.Blocks missing=new NetherTerrain.Blocks() {
            public int space(int x,int y,int z){return -1;}
            public int color(int x,int y,int z){return 1;}
        };
        assertTrue(NetherTerrain.cave(missing,0,64,0,120,true));
        assertFalse(NetherTerrain.cave(missing,0,64,0,120,false));
        assertEquals(-1,NetherTerrain.enclosure(missing,0,64,0,120,true));
    }
    @Test void floorSamplingExcludesBedrockRoofAndIncludesLavaSurfaces() {
        assertEquals(0xFF884422,NetherTerrain.floor(cavern,0,0,80,0,120));
        NetherTerrain.Blocks lava=new NetherTerrain.Blocks() {
            public int space(int x,int y,int z){return y<=30?0:y==31?2:y>=120?0:1;}
            public int color(int x,int y,int z){return y==31?0xFFFF6600:0xFF111111;}
        };
        assertEquals(0xFFFF6600,NetherTerrain.floor(lava,0,0,80,0,120));
    }
    @Test void jumpingOverABandBoundaryUsesTheStandingFloor() {
        NetherTerrain.Blocks floor=new NetherTerrain.Blocks() {
            public int space(int x,int y,int z){return y<=14?0:1;}
            public int color(int x,int y,int z){return 1;}
        };
        assertEquals(15,NetherTerrain.supportY(floor,0,15,0));
        assertEquals(15,NetherTerrain.supportY(floor,0,16,0));
        assertEquals(15,NetherTerrain.supportY(floor,0,17,0));
        assertEquals(20,NetherTerrain.supportY(floor,0,20,0));
    }
}
