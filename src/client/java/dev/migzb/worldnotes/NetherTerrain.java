package dev.migzb.worldnotes;

/** Nether enclosure and floor sampling deliberately exclude the dimension's roof. */
final class NetherTerrain {
    interface Blocks {
        /** -1 unknown, 0 solid, 1 open, 2 fluid. */
        int space(int x,int y,int z);
        int color(int x,int y,int z);
    }
    static int supportY(Blocks blocks,int x,int y,int z) {
        for(int depth=1;depth<=4;depth++)if(blocks.space(x,y-depth,z)==0)return y-depth+1;
        return y;
    }
    static boolean cave(Blocks blocks,int x,int y,int z,int roofLimit,boolean previous) {
        int result=enclosure(blocks,x,y,z,roofLimit,previous);
        return result<0?previous:result==1;
    }
    /** Unknown neighbours must stay pending instead of becoming a cached cave boundary. */
    static int enclosure(Blocks blocks,int x,int y,int z,int roofLimit,boolean previous) {
        boolean ceiling=false,unknown=false;
        for(int above=2;above<=12&&y+above<roofLimit;above++) {
            int cell=blocks.space(x,y+above,z);
            if(cell<0)unknown=true;if(cell==0){ceiling=true;break;}
        }
        if(!ceiling)return unknown?-1:0;
        int walls=0,wallMask=0;
        int[][] directions={{1,0},{-1,0},{0,1},{0,-1}};
        for(int directionIndex=0;directionIndex<directions.length;directionIndex++)for(int distance=1;distance<=16;distance++) {
            int[] direction=directions[directionIndex];
            int cell=blocks.space(x+direction[0]*distance,y+1,z+direction[1]*distance);
            if(cell<0){unknown=true;break;}if(cell==0){walls++;wallMask|=1<<directionIndex;break;}
        }
        if((wallMask&3)==3||(wallMask&12)==12||previous&&walls>=2)return 1;
        return unknown?-1:0;
    }
    static int floor(Blocks blocks,int x,int z,int anchor,int minY,int roofLimit) {
        int start=Math.min(roofLimit-1,anchor+16),bottom=Math.max(minY,anchor-64);
        for(int y=start;y>=bottom;y--) {
            int space=blocks.space(x,y,z);
            if((space==0||space==2)&&blocks.space(x,y+1,z)==1)return blocks.color(x,y,z);
        }
        return TerrainCache.UNKNOWN;
    }
}
