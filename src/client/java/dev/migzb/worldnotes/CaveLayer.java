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
    private int seedX,seedZ;
    private final BitSet checked=new BitSet(),sampled=new BitSet(),connected=new BitSet(),waiting=new BitSet();
    private final ArrayDeque<Integer> queue=new ArrayDeque<>();
    private final int[] colors,floorY;
    private long revision;
    CaveLayer(int x,int y,int z,int radius) {
        centerX=x;centerZ=z;seedX=x;seedZ=z;seedY=y;minY=Math.floorDiv(y,16)*16;
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
        int index=index(x,y,z);
        if(index<0)return true;
        if(x==seedX&&y==seedY&&z==seedZ)return false;
        return index<0||!connected.get(index)&&(!checked.get(index)||sampled.get(index));
    }
    /** Verify a short path to the existing component before deciding this is a different cave. */
    boolean followPlayer(Terrain terrain,int x,int y,int z,long deadline) {
        if(!needsReseed(x,y,z))return false;
        if(index(x,y,z)<0)return true;
        int side=9,total=side*side*side,center=total/2;
        int[] parent=new int[total],work=new int[total];Arrays.fill(parent,-2);
        parent[center]=-1;work[0]=center;int head=0,tail=1;boolean unknown=false;
        while(head<tail) {
            if(System.nanoTime()>=deadline)return false;
            int point=work[head++],dx=point%side-4,dz=point/side%side-4,dy=point/(side*side)-4;
            int px=x+dx,py=y+dy,pz=z+dz;
            if(index(px,py,pz)<0)continue;
            if(contains(px,py,pz)) {
                for(int path=parent[point];path>=0;path=parent[path]) {
                    int qx=x+path%side-4,qz=z+path/side%side-4,qy=y+path/(side*side)-4;
                    int cell=index(qx,qy,qz);
                    if(!connected.get(cell)) {
                        checked.set(cell);sampled.set(cell);connected.set(cell);revision++;
                        int column=cell%plane;if(colors[column]==TerrainCache.UNKNOWN)colors[column]=0xFF484F59;
                        queue.addFirst(cell);
                    }
                }
                return false;
            }
            int space=terrain.space(px,py,pz);
            if(space<0){unknown=true;continue;}if(space==0)continue;
            int[] neighbours={dx>-4?point-1:-1,dx<4?point+1:-1,dz>-4?point-side:-1,dz<4?point+side:-1,
                    dy>-4?point-side*side:-1,dy<4?point+side*side:-1};
            for(int next:neighbours)if(next>=0&&parent[next]==-2){parent[next]=point;work[tail++]=next;}
        }
        return !unknown;
    }
    /** Shift the window while preserving overlap, queued work, and unresolved chunk frontiers. */
    CaveLayer recenter(int x,int z) {
        int dx=x-centerX,dz=z-centerZ;
        CaveLayer next=new CaveLayer(x,seedY,z,(width-1)/32);next.queue.clear();next.checked.clear();
        next.seedX=seedX;next.seedZ=seedZ;
        next.checked.or(shift(checked,dx,dz));next.sampled.or(shift(sampled,dx,dz));
        next.connected.or(shift(connected,dx,dz));next.waiting.or(shift(waiting,dx,dz));
        int startX=Math.max(0,dx),endX=Math.min(width,width+dx);
        for(int row=Math.max(0,dz);row<Math.min(width,width+dz);row++)if(endX>startX) {
            int source=row*width+startX,destination=(row-dz)*width+startX-dx;
            System.arraycopy(colors,source,next.colors,destination,endX-startX);
            System.arraycopy(floorY,source,next.floorY,destination,endX-startX);
        }
        for(int cell:queue) {
            int column=cell%plane,target=next.index(left+column%width,minY+cell/plane,top+column/width);
            if(target>=0)next.queue.addLast(target);
        }
        for(int y=minY;y<minY+16;y++)for(int offset=0;offset<width;offset++) {
            if(dx>0&&contains(left+width-1,y,top+offset))next.enqueue(left+width,y,top+offset);
            if(dx<0&&contains(left,y,top+offset))next.enqueue(left-1,y,top+offset);
            if(dz>0&&contains(left+offset,y,top+width-1))next.enqueue(left+offset,y,top+width);
            if(dz<0&&contains(left+offset,y,top))next.enqueue(left+offset,y,top-1);
        }
        next.revision=revision+1;return next;
    }
    private BitSet shift(BitSet source,int dx,int dz) {
        long[] input=source.toLongArray(),output=new long[(plane*16+63)/64];
        int startX=Math.max(0,dx),count=width-Math.abs(dx);
        if(count<=0)return new BitSet();
        for(int y=0;y<16;y++)for(int row=Math.max(0,dz);row<Math.min(width,width+dz);row++) {
            int from=y*plane+row*width+startX,to=y*plane+(row-dz)*width+startX-dx;
            for(int copied=0;copied<count;copied+=64) {
                int s=from+copied,d=to+copied,remaining=Math.min(64,count-copied),word=s>>>6,offset=s&63;
                long bits=word<input.length?input[word]>>>offset:0;
                if(offset!=0&&word+1<input.length)bits|=input[word+1]<<(64-offset);
                if(remaining<64)bits&=(1L<<remaining)-1;
                output[d>>>6]|=bits<<(d&63);
                if((d&63)!=0&&(d>>>6)+1<output.length)output[(d>>>6)+1]|=bits>>>(64-(d&63));
            }
        }
        return BitSet.valueOf(output);
    }
    /** Only changes to known connectivity rebuild the scan; visual floor changes patch a pixel. */
    boolean changed(int x,int y,int z,boolean open,int color) {
        int index=index(x,y,z);
        if(index>=0&&sampled.get(index)&&connected.get(index)!=open
                &&(touches(x,y,z)||x==seedX&&y==seedY&&z==seedZ))return true;
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
    int referenceY(){return seedY;}
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
