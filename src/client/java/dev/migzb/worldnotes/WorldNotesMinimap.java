package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import java.util.*;

/** Never requests server chunks: terrain and entity information are client observations. */
final class WorldNotesMinimap {
    private static final int GRID = 128, UNKNOWN = TerrainCache.UNKNOWN;
    private static final BlockPos.MutableBlockPos POSITION = new BlockPos.MutableBlockPos();
    private static MinimapSettings settings;
    private static ClientLevel level;
    private static TerrainCache cache;
    private static String context;
    private static int originX, originZ, cursor, tickCount;
    private static final int[] pixels = new int[GRID*GRID];
    private static List<Bookmark> markers = List.of();
    private static String markerProfile;
    private static long markerRevision = -1;
    private static final List<int[]> OFFSETS = offsets();
    private static List<int[]> offsets() {
        List<int[]> result = new ArrayList<>();
        for (int z=-33; z<=33; z++) for (int x=-33; x<=33; x++) result.add(new int[]{x,z});
        result.sort(Comparator.comparingInt(a -> a[0]*a[0]+a[1]*a[1]));
        return List.copyOf(result);
    }
    static void initialize() { settings = MinimapSettings.load(); }
    static MinimapSettings settings() { return settings; }
    static boolean apply(MinimapSettings draft) {
        if (!draft.save()) return false;
        settings = draft.copy(); return true;
    }
    static boolean visible(Minecraft client) {
        return settings != null && settings.enabled && client.player != null && client.level != null
                && !WorldNotesGui.hidden(client) && client.getWindow().getGuiScaledWidth() >= 240
                && client.getWindow().getGuiScaledHeight() >= 180;
    }
    static void zoom(Minecraft client) {
        MinimapSettings draft = settings.copy();
        draft.radiusChunks = draft.radiusChunks == 32 ? 8 : draft.radiusChunks * 2;
        if (!apply(draft)) { WorldNotesGui.overlay(client, Component.translatable("worldnotes.save_failed")); return; }
        cursor = 0;
        if (client.player != null) WorldNotesGui.overlay(client, Component.literal("Map radius: " + settings.radiusChunks + " chunks"));
    }
    static int span() { return settings.radiusChunks * 32; }
    static void tick(Minecraft client) {
        if (client.level == null || client.player == null) { level=null; cache=null; context=null; return; }
        if (!settings.enabled && !(WorldNotesGui.screen(client) instanceof ExpandedMapScreen)) return;
        String profile = WorldNotesClient.profile(client);
        if (!profile.equals(markerProfile) || markerRevision != BookmarkStore.revision()) {
            markers = BookmarkStore.forProfile(profile); markerProfile=profile; markerRevision=BookmarkStore.revision();
        }
        String band = client.level.dimensionType().hasCeiling() && client.player.getBlockY()<client.level.dimensionType().logicalHeight()
                ? "cave:"+Math.floorDiv(client.player.getBlockY(),32) : "surface";
        String nextContext = profile+"\n"+WorldNotesClient.dimension(client.level)+"\n"+band;
        if (level != client.level || !nextContext.equals(context)) {
            level=client.level; context=nextContext; cursor=0;
            cache=new TerrainCache(WorldNotesClient.configDirectory().resolve("worldnotes/terrain"), context);
        }
        int cx=Math.floorDiv(client.player.getBlockX(),16), cz=Math.floorDiv(client.player.getBlockZ(),16);
        originX=client.player.getBlockX()-span()/2; originZ=client.player.getBlockZ()-span()/2;
        int samples=0;
        for (int work=0; work<64; work++) {
            int[] offset=OFFSETS.get(cursor++ % OFFSETS.size());
            if (Math.abs(offset[0])>settings.radiusChunks+1 || Math.abs(offset[1])>settings.radiusChunks+1) continue;
            int x=cx+offset[0], z=cz+offset[1];
            cache.request(x,z);
            if (samples>=8 || cache.has(x,z) || !cache.ready(x,z)) continue;
            LevelChunk chunk=level.getChunkSource().getChunk(x,z,false);
            if (chunk==null) continue;
            int[] colors=new int[16];
            for (int row=0; row<4; row++) for (int col=0; col<4; col++)
                colors[row*4+col]=sampleColor(chunk,x*16+col*4+2,z*16+row*4+2,client.player.getBlockY());
            cache.store(x,z,colors); samples++;
        }
        cursor %= OFFSETS.size();
        if (++tickCount % 100 == 0) cache.trim(cx,cz,settings.radiusChunks+3);
        for(int row=0;row<GRID;row++) for(int col=0;col<GRID;col++)
            pixels[row*GRID+col]=cache.color(originX+col*span()/GRID,originZ+row*span()/GRID);
    }
    static int[] bounds(Minecraft client) {
        int width=client.getWindow().getGuiScaledWidth(), height=client.getWindow().getGuiScaledHeight();
        int size=Math.min(settings.size,Math.min(width-24,height-80));
        int maxX=width-size-9, maxY=height-size-25;
        int x=9,y=56;
        switch (settings.position) {
            case TOP_RIGHT -> { x=maxX; y=width<360?56:18; }
            case TOP_LEFT -> { x=9; y=56; }
            case BOTTOM_RIGHT -> { x=maxX; y=maxY; }
            case BOTTOM_LEFT -> { x=9; y=maxY; }
            case CUSTOM -> { x=9+(maxX-9)*settings.customX/100; y=56+(maxY-56)*settings.customY/100; }
        }
        return new int[]{x,y,size};
    }
    static int trackingTop(Minecraft client) {
        if (!visible(client)) return 30;
        int[] b=bounds(client);
        return (settings.position == MinimapSettings.Position.TOP_RIGHT || settings.position == MinimapSettings.Position.CUSTOM)
                && b[1]+b[2]+65<=client.getWindow().getGuiScaledHeight() ? b[1]+b[2]+24 : 50;
    }
    static void render(GuiGraphicsExtractor graphics, Minecraft client) {
        if (!visible(client)) return;
        int[] b=bounds(client);
        WorldNotesTheme.panel(graphics,b[0]-3,b[1]-3,b[2]+6,b[2]+17,WorldNotesTheme.dimensionAccent(WorldNotesClient.dimension(client.level)));
        renderMap(graphics,client,b[0],b[1],b[2],-1,-1,false);
        graphics.text(client.font,"N",b[0]+b[2]/2-3,b[1]-13,0xFFFFFFFF,true);
        graphics.text(client.font,settings.radiusChunks+" chunks radius",b[0]+2,b[1]+b[2]+3,0xFFD4DEE9,true);
    }
    private static int coordinate(double value,int origin,int size) { return (int)Math.floor((value-origin)*size/span()); }
    private static void dot(GuiGraphicsExtractor g,int x,int y,int color,int radius) {
        g.fill(x-radius-1,y-radius-1,x+radius+2,y+radius+2,0xFF10151D);
        g.fill(x-radius,y-radius,x+radius+1,y+radius+1,color);
    }
    static void renderMap(GuiGraphicsExtractor g,Minecraft client,int left,int top,int size,int mouseX,int mouseY,boolean expanded) {
        if (cache==null || level!=client.level || client.player==null) return;
        int grid=Math.min(GRID,size);
        for (int row=0;row<grid;row++) {
            for (int col=0;col<grid;) {
                int color=pixels[row*GRID/grid*GRID+col*GRID/grid],end=col+1;
                while (end<grid && pixels[row*GRID/grid*GRID+end*GRID/grid]==color) end++;
                g.fill(left+col*size/grid,top+row*size/grid,left+end*size/grid,top+(row+1)*size/grid,color); col=end;
            }
        }
        String dimension=WorldNotesClient.dimension(level);
        Component hovered=null;
        for (Entity entity:level.entitiesForRendering()) {
            if (entity==client.player || entity.isRemoved()) continue;
            int color;
            if (entity instanceof ItemEntity && settings.items) color=0xFFFFD96B;
            else if (entity instanceof Player && settings.players) color=0xFF6DDFFF;
            else if (entity instanceof NeutralMob && settings.neutralMobs) color=0xFF86E8A1;
            else if (entity instanceof Enemy && !(entity instanceof NeutralMob) && settings.hostileMobs) color=0xFFFF6B75;
            else if (entity instanceof Mob && !(entity instanceof Enemy) && settings.neutralMobs) color=0xFF86E8A1;
            else continue;
            int x=coordinate(entity.getX(),originX,size),z=coordinate(entity.getZ(),originZ,size);
            if(x<3||z<3||x>=size-3||z>=size-3) continue;
            dot(g,left+x,top+z,color,expanded?2:1);
            if (expanded && Math.abs(mouseX-left-x)<=5 && Math.abs(mouseY-top-z)<=5) {
                hovered=entity instanceof ItemEntity item ? item.getItem().getHoverName().copy().append(" x"+item.getItem().getCount()) : entity.getDisplayName();
                hovered=hovered.copy().append(" ("+entity.getBlockX()+", "+entity.getBlockY()+", "+entity.getBlockZ()+")");
            }
        }
        for (Bookmark bookmark:markers) {
            if(bookmark.hidden || !dimension.equals(bookmark.dimension)) continue;
            int x=coordinate(bookmark.x,originX,size),z=coordinate(bookmark.z,originZ,size);
            boolean tracked=WorldNotesClient.isTracked(bookmark);
            if(tracked) { x=Math.clamp(x,3,size-4); z=Math.clamp(z,3,size-4); }
            else if(x<3||z<3||x>=size-3||z>=size-3) continue;
            dot(g,left+x,top+z,tracked?WorldNotesTheme.ACCENT:0xFFB5A0FF,2);
            if(expanded && Math.abs(mouseX-left-x)<=5 && Math.abs(mouseY-top-z)<=5) hovered=bookmark.displayName();
        }
        int px=coordinate(client.player.getX(),originX,size),pz=coordinate(client.player.getZ(),originZ,size);
        dot(g,left+px,top+pz,0xFFFFFFFF,2);
        double yaw=Math.toRadians(client.player.getYRot());
        for(int step=3;step<=8;step++) {
            int x=left+px-(int)Math.round(Math.sin(yaw)*step), z=top+pz+(int)Math.round(Math.cos(yaw)*step);
            g.fill(x,z,x+1,z+1,0xFFFFFFFF);
        }
        if(hovered!=null) g.text(client.font,hovered,left,top+size+5,0xFFFFFFFF,true);
    }
    private static int sampleColor(LevelChunk chunk, int x, int z, int playerY) {
        int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
        if (level.dimensionType().hasCeiling() && playerY < level.dimensionType().logicalHeight()) {
            // Look for a cave floor near the player's elevation instead of painting the Nether roof.
            height = Math.min(height, playerY + 16);
            boolean foundAir = false;
            for (int depth = 0; depth < 64 && height >= level.getMinY(); depth++, height--) {
                POSITION.set(x, height, z);
                var state = chunk.getBlockState(POSITION);
                if (state.isAir()) foundAir = true;
                else if (foundAir || !state.getFluidState().isEmpty()) {
                    int rgb = state.getMapColor(level, POSITION).col;
                    if (rgb != 0) return 0xFF000000 | rgb;
                }
            }
            return UNKNOWN;
        }
        // Skip colorless surface blocks such as glass, without scanning entire columns.
        for (int depth = 0; depth < 16 && height >= level.getMinY(); depth++, height--) {
            POSITION.set(x, height, z);
            int rgb = chunk.getBlockState(POSITION).getMapColor(level, POSITION).col;
            if (rgb != 0) return 0xFF000000 | rgb;
        }
        return UNKNOWN;
    }
}
