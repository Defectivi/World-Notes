package dev.migzb.worldnotes;

/** Shared geometry for the HUD and drag preview; anchors preserve placement across resizing. */
final class MinimapPlacement {
    record Bounds(int x,int y,int size) {
        boolean contains(double mouseX,double mouseY){return mouseX>=x&&mouseX<x+size&&mouseY>=y&&mouseY<y+size;}
    }
    private static int size(MinimapSettings settings,int width,int height) {
        return Math.max(16,Math.min(settings.size,Math.min(width-24,height-80)));
    }
    static Bounds bounds(MinimapSettings settings,int width,int height) {
        int size=size(settings,width,height),maxX=Math.max(3,width-size-3),maxY=Math.max(16,height-size-17);
        int x=9,y=56;
        switch(settings.position) {
            case TOP_RIGHT -> {x=width-size-9;y=width<360?56:18;}
            case TOP_LEFT -> {x=9;y=56;}
            case BOTTOM_RIGHT -> {x=width-size-9;y=height-size-25;}
            case BOTTOM_LEFT -> {x=9;y=height-size-25;}
            case CUSTOM -> {
                // Existing percentage settings retain their original on-screen coordinates.
                x=settings.customAnchorX<0?9+(width-size-18)*settings.customX/100:
                        3+(int)Math.round((maxX-3)*settings.customAnchorX);
                y=settings.customAnchorY<0?56+(height-size-81)*settings.customY/100:
                        16+(int)Math.round((maxY-16)*settings.customAnchorY);
            }
        }
        return new Bounds(Math.clamp(x,3,maxX),Math.clamp(y,16,maxY),size);
    }
    static void move(MinimapSettings draft,int width,int height,double x,double y) {
        int size=size(draft,width,height),rangeX=Math.max(0,width-size-6),rangeY=Math.max(0,height-size-33);
        draft.position=MinimapSettings.Position.CUSTOM;
        draft.customAnchorX=rangeX==0?0:Math.clamp((x-3)/rangeX,0,1);
        draft.customAnchorY=rangeY==0?0:Math.clamp((y-16)/rangeY,0,1);
        draft.customX=(int)Math.round(draft.customAnchorX*100);
        draft.customY=(int)Math.round(draft.customAnchorY*100);
    }
}
