package dev.migzb.worldnotes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;
class TerrainTileFileTest {
    @TempDir Path directory;
    @Test void preservesAllColorsAndReplacesPreviousTile() throws Exception {
        Path tile=directory.resolve("nested/-2_3.wnt");
        int[] colors=new int[256];
        for(int i=0;i<256;i++)colors[i]=0xFF000000|i*0x111111;
        TerrainTileFile.write(tile,colors);assertArrayEquals(colors,TerrainTileFile.read(tile));
        colors[0]=0xFFABCDEF;TerrainTileFile.write(tile,colors);assertArrayEquals(colors,TerrainTileFile.read(tile));
        try(var files=Files.list(tile.getParent())){assertEquals(1,files.count());}
    }
    @Test void rejectsCorruptTruncatedUnknownAndTrailingData() throws Exception {
        Path file=directory.resolve("bad.wnt");
        Files.writeString(file,"broken");assertThrows(IOException.class,()->TerrainTileFile.read(file));
        for(int kind=0;kind<3;kind++){
            try(var out=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(file)))){
                out.writeInt(kind==0?0:0x574E5432);
                for(int i=0;i<(kind==1?255:256);i++)out.writeInt(i);
                if(kind==2)out.writeInt(1);
            }
            assertThrows(IOException.class,()->TerrainTileFile.read(file));
        }
        Files.write(file,new byte[4097]);assertThrows(IOException.class,()->TerrainTileFile.read(file));
    }
    @Test void negativeCoordinatesAndContextsNeverShareTerrain() throws Exception {
        TerrainCache first=new TerrainCache(directory,"server:A\noverworld"),second=new TerrainCache(directory,"server:A\nnether");
        int[] colors=new int[256];for(int i=0;i<256;i++)colors[i]=i+1;
        first.store(-1,-1,colors).get(5,java.util.concurrent.TimeUnit.SECONDS);colors[255]=99;
        assertEquals(256,first.color(-1,-1));assertEquals(1,first.color(-16,-16));
        assertEquals(TerrainCache.UNKNOWN,first.color(0,0));assertEquals(TerrainCache.UNKNOWN,second.color(-1,-1));
        TerrainCache reopened=new TerrainCache(directory,"server:A\noverworld");
        reopened.request(-1,-1).get(5,java.util.concurrent.TimeUnit.SECONDS);assertEquals(256,reopened.color(-1,-1));
        first.trim(100,100,35);assertEquals(TerrainCache.UNKNOWN,first.color(-1,-1));
    }
    @Test void settingsClampMalformedValuesWithoutDiscardingToggles() {
        MinimapSettings s=new MinimapSettings();s.zoom=-1;s.size=999;s.position=null;s.customX=-90;s.customY=900;s.enabled=false;
        s.normalize();assertEquals(1,s.zoom);assertEquals(96,s.size);assertEquals(0,s.customX);assertEquals(100,s.customY);assertFalse(s.enabled);
        MinimapSettings copy=s.copy();copy.enabled=true;assertFalse(s.enabled);assertEquals(MinimapSettings.Position.TOP_RIGHT,copy.position);
    }
    @Test void coordinateHighlightUsesContainingBlockIncludingNegativeFractions() {
        assertEquals(new net.minecraft.core.BlockPos(-1,63,2),net.minecraft.core.BlockPos.containing(-0.01,63.99,2.5));
        assertEquals(new net.minecraft.core.BlockPos(0,64,-16),net.minecraft.core.BlockPos.containing(0,64,-16));
    }
    @Test void adjacentBlocksRemainDistinctAndRegionCopiesCrossChunkBoundaries() throws Exception {
        TerrainCache cache=new TerrainCache(directory,"blocks-v2");
        int[] west=new int[256],east=new int[256];
        for(int i=0;i<256;i++){west[i]=0xFF000000|i;east[i]=0xFF100000|i;}
        cache.store(-1,0,west).get(5,java.util.concurrent.TimeUnit.SECONDS);
        cache.store(0,0,east).get(5,java.util.concurrent.TimeUnit.SECONDS);
        assertNotEquals(cache.color(-16,0),cache.color(-15,0));
        assertNotEquals(cache.color(0,0),cache.color(1,0));
        int[] image=new int[4*4];cache.copyRegion(-2,0,4,image);
        assertEquals(west[14],image[0]);assertEquals(west[15],image[1]);assertEquals(east[0],image[2]);assertEquals(east[1],image[3]);
        assertEquals(east[16],image[6]);
        cache.copyRegion(15,15,4,image);
        assertEquals(east[255],image[0]);assertEquals(TerrainCache.UNKNOWN,image[1]);assertEquals(TerrainCache.UNKNOWN,image[4]);
    }
    @Test void coarseCacheCannotBeMistakenForBlockResolution() throws Exception {
        Path tile=directory.resolve("coarse.wnt");
        try(var output=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(tile)))){
            output.writeInt(0x574E5431);for(int i=0;i<16;i++)output.writeInt(i);
        }
        assertThrows(IOException.class,()->TerrainTileFile.read(tile));
    }
}
