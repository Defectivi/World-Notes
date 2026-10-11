package dev.migzb.worldnotes;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;

/** Incremental six-neighbour flood through one height layer, seeded only at the player. */
final class CaveLayer {
    interface Terrain {
        /** -1: not received; 0: blocked; 1: open space. Never loads a chunk. */
        int space(int x,int y,int z);
        int color(int x,int y,int z);
    }
    final int centerX,centerZ,minY,width,left,top;
    private final int seedY,plane;
    private final BitSet checked=new BitSet(),sampled=new BitSet(),connected=new BitSet(),waiting=new BitSet();
    private final ArrayDeque<Integer> queue=new ArrayDeque<>();
    private final int[] colors,floorY;
    private long revision;
    CaveLayer(int x,int y,int z,int radius) {
        centerX=x;centerZ=z;seedY=y;minY=Math.floorDiv(y,16)*16;
        width=radius*32+1;left=x-radius*16;top=z-radius*16;plane=width*width;
        colors=new int[plane];Arrays.fill(colors,TerrainCache.UNKNOWN);
        floorY=new int[plane];Arrays.fill(floorY,Integer.MIN_VALUE);
        enqueue(x,y,z);
    }
    private int index(int x,int y,int z) {
        if(x<left||x>=left+width||z<top||z>=top+width||y<minY||y>=minY+16)return -1;
        return (y-minY)*plane+(z-top)*width+x-left;
    }
    private void enqueue(int x,int y,int z) {
        int index=index(x,y,z);
        if(index>=0&&!checked.get(index)) {checked.set(index);queue.addLast(index);}
    }
    boolean contains(int x,int y,int z) {int index=index(x,y,z);return index>=0&&connected.get(index);}
    /** An unprocessed seed/frontier is not evidence that the player entered a different cave. */
    boolean needsReseed(int x,int y,int z) {
        if(x==centerX&&y==seedY&&z==centerZ)return false;
        int index=index(x,y,z);
        return index<0||!connected.get(index)&&(!checked.get(index)||sampled.get(index));
    }
    /** Only changes to known connectivity rebuild the scan; visual floor changes patch a pixel. */
    boolean changed(int x,int y,int z,boolean open,int color) {
        int index=index(x,y,z);
        if(index>=0&&sampled.get(index)&&connected.get(index)!=open
                &&(touches(x,y,z)||x==centerX&&y==seedY&&z==centerZ))return true;
        if(x>=left&&x<left+width&&z>=top&&z<top+width) {
            int column=(z-top)*width+x-left;
            if(floorY[column]==y+1&&colors[column]!=color){colors[column]=color;revision++;}
        }
        return false;
    }
    boolean touches(int x,int y,int z) {
        return contains(x,y,z)||contains(x-1,y,z)||contains(x+1,y,z)||contains(x,y-1,z)
                ||contains(x,y+1,z)||contains(x,y,z-1)||contains(x,y,z+1);
    }
    long revision(){return revision;}
    /** Retry only the unresolved frontier when more server chunks arrive. */
    void retryUnknown() {
        for(int index=waiting.nextSetBit(0);index>=0;index=waiting.nextSetBit(index+1))queue.addLast(index);
        waiting.clear();
    }
    void advance(Terrain terrain,int budget,long deadline) {
        for(int work=0;work<budget&&!queue.isEmpty()&&System.nanoTime()<deadline;work++) {
            int index=queue.removeFirst(),column=index%plane;
            int y=minY+index/plane,z=top+column/width,x=left+column%width;
            int space=terrain.space(x,y,z);
            if(space<0){waiting.set(index);continue;}
            sampled.set(index);
            if(space==0)continue;
            connected.set(index);revision++;
            if(colors[column]==TerrainCache.UNKNOWN)colors[column]=0xFF484F59;
            if(terrain.space(x,y-1,z)==0 && (floorY[column]==Integer.MIN_VALUE
                    ||Math.abs(y-seedY)<Math.abs(floorY[column]-seedY))) {
                colors[column]=terrain.color(x,y-1,z);floorY[column]=y;
            }
            enqueue(x-1,y,z);enqueue(x+1,y,z);enqueue(x,y,z-1);enqueue(x,y,z+1);
            enqueue(x,y-1,z);enqueue(x,y+1,z);
        }
    }
    void copyRegion(int x,int z,int size,int[] output) {
        Arrays.fill(output,TerrainCache.UNKNOWN);
        for(int row=0;row<size;row++) {
            int sourceZ=z+row-top,start=Math.max(x,left),end=Math.min(x+size,left+width);
            if(sourceZ>=0&&sourceZ<width&&end>start)
                System.arraycopy(colors,sourceZ*width+start-left,output,row*size+start-x,end-start);
        }
    }
}
