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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Never requests server chunks: terrain and entity information are client observations. */
final class WorldNotesMinimap {
    private static final int UNKNOWN = TerrainCache.UNKNOWN;
    private static final Identifier TERRAIN_TEXTURE = Identifier.fromNamespaceAndPath("worldnotes", "terrain_map");
    private static WorldNotesMapTexture terrainTexture;
    private static int textureSize, radius = 1, imageOriginX, imageOriginZ;
    private static long imageRevision = -1;
    private static boolean textureDirty;
    private static final BlockPos.MutableBlockPos POSITION = new BlockPos.MutableBlockPos();
    private static MinimapSettings settings;
    private static ClientLevel level;
    private static TerrainCache cache;
    private static String context;
    private static int originX, originZ, cursor, tickCount;
    private static int scanX=Integer.MIN_VALUE,scanZ,scanZoom;
    private static int coverage;
    private static int[] pixels = new int[0];
    private static List<Bookmark> markers = List.of();
    private static String markerProfile;
    private static long markerRevision = -1;
    private static List<int[]> offsets = List.of();
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
        zoom(client,settings.zoom==16?-4:1);
    }
    static void zoom(Minecraft client,int steps) {
        MinimapSettings draft = settings.copy();
        draft.zoom = MapNavigation.zoom(draft.zoom,steps);
        if(draft.zoom==settings.zoom)return;
        if (!apply(draft)) { WorldNotesGui.overlay(client, Component.translatable("worldnotes.save_failed")); return; }
        cursor = 0;
        if (client.player != null) WorldNotesGui.overlay(client, Component.literal("Map zoom: " + settings.zoom + "x (render distance: " + radius + " chunks)"));
    }
    static int span() { return Math.max(16, radius * 32 / settings.zoom); }
    static int radius() { return radius; }
    static int coverage() { return coverage; }
    static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            level=null; cache=null; context=null; pixels=new int[0]; imageRevision=-1;
            if(terrainTexture!=null) { terrainTexture.close(); terrainTexture=null; textureSize=0; }
            return;
        }
        if (!settings.enabled && !(WorldNotesGui.screen(client) instanceof ExpandedMapScreen)) return;
        int renderRadius=Math.clamp(client.options.getEffectiveRenderDistance(),1,64);
        if(radius!=renderRadius || offsets.isEmpty() || scanZoom!=settings.zoom) {
            radius=renderRadius; offsets=MapNavigation.terrainOffsets(radius,settings.zoom); cursor=0; scanX=Integer.MIN_VALUE;
        }
        String profile = WorldNotesClient.profile(client);
        if (!profile.equals(markerProfile) || markerRevision != BookmarkStore.revision()) {
            markers = BookmarkStore.forProfile(profile); markerProfile=profile; markerRevision=BookmarkStore.revision();
        }
        String band = client.level.dimensionType().hasCeiling() && client.player.getBlockY()<client.level.dimensionType().logicalHeight()
                ? "cave:"+Math.floorDiv(client.player.getBlockY(),32) : "surface";
        // The versioned context preserves old coarse history without treating it as block-level data.
        String nextContext = "blocks-v2\n"+profile+"\n"+WorldNotesClient.dimension(client.level)+"\n"+band;
        if (level != client.level || !nextContext.equals(context)) {
            level=client.level; context=nextContext; cursor=0;
            cache=new TerrainCache(WorldNotesClient.configDirectory().resolve("worldnotes/terrain"), context);
            imageRevision=-1;
            scanX=Integer.MIN_VALUE;
        }
        int cx=Math.floorDiv(client.player.getBlockX(),16), cz=Math.floorDiv(client.player.getBlockZ(),16);
        originX=client.player.getBlockX()-span()/2; originZ=client.player.getBlockZ()-span()/2;
        if(cx!=scanX || cz!=scanZ || scanZoom!=settings.zoom || cursor>=offsets.size() && tickCount%4==0) {
            scanX=cx;scanZ=cz;scanZoom=settings.zoom;cursor=0;
        }
        int samples=0;
        long deadline=System.nanoTime()+6_000_000L;
        for (int work=0; work<512 && cursor<offsets.size() && samples<32 && System.nanoTime()<deadline; work++) {
            int[] offset=offsets.get(cursor++);
            int x=cx+offset[0], z=cz+offset[1];
            if(cache.has(x,z))continue;
            LevelChunk chunk=level.getChunkSource().getChunk(x,z,false);
            if (chunk==null) { cache.request(x,z);continue; }
            int[] colors=new int[TerrainTileFile.SAMPLES];
            for (int row=0; row<16; row++) for (int col=0; col<16; col++)
                colors[row*16+col]=sampleColor(chunk,x*16+col,z*16+row,client.player.getBlockY());
            cache.store(x,z,colors); samples++;
        }
        if (++tickCount % 100 == 0) cache.trim(cx,cz,radius+2);
        int width=span();
        if(tickCount%2==0 || pixels.length!=width*width || imageRevision<0
                || Math.abs((long)originX-imageOriginX)>width/4 || Math.abs((long)originZ-imageOriginZ)>width/4) {
            long revision=cache.revision();
            if(pixels.length!=width*width || imageOriginX!=originX || imageOriginZ!=originZ || imageRevision!=revision) {
                pixels=new int[width*width];
                cache.copyRegion(originX,originZ,width,pixels);
                int known=0;for(int color:pixels)if(color!=UNKNOWN)known++;coverage=known*100/pixels.length;
                imageOriginX=originX; imageOriginZ=originZ; imageRevision=revision; textureDirty=true;
            }
        }
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
        String dimension=WorldNotesClient.dimension(client.level);
        CompassColors.draw(graphics,client.font,"N",b[0]+b[2]/2-3,b[1]-13,dimension);
        CompassColors.draw(graphics,client.font,MapNavigation.facing(client.player.getYRot()),b[0]+2,b[1]+b[2]+3,dimension);
    }
    private static int coordinate(double value,int origin,int size) { return (int)Math.floor((value-origin)*size/Math.sqrt(pixels.length)); }
    private static void dot(GuiGraphicsExtractor g,int x,int y,int color,int radius) {
        g.fill(x-radius-1,y-radius-1,x+radius+2,y+radius+2,0xFF10151D);
        g.fill(x-radius,y-radius,x+radius+1,y+radius+1,color);
    }
    static List<Component> renderMap(GuiGraphicsExtractor g,Minecraft client,int left,int top,int size,int mouseX,int mouseY,boolean expanded) {
        if (cache==null || level!=client.level || client.player==null) return List.of();
        if(pixels.length==0) return List.of();
        int width=(int)Math.sqrt(pixels.length);
        if(terrainTexture==null || textureSize!=width || terrainTexture.image()==null || terrainTexture.image().isClosed()) {
            if(terrainTexture!=null) terrainTexture.close();
            terrainTexture=new WorldNotesMapTexture(client,TERRAIN_TEXTURE,width);
            textureSize=width; textureDirty=true;
        }
        if(textureDirty) {
            var image=terrainTexture.image();
            for(int z=0;z<width;z++) for(int x=0;x<width;x++) {
                int color=pixels[z*width+x];
                int muted=color==UNKNOWN?UNKNOWN:0xFF000000|((((color>>16)&255)*4/5+12)<<16)|((((color>>8)&255)*4/5+12)<<8)|((color&255)*4/5+12);
                image.setPixel(x,z,muted);
            }
            terrainTexture.upload(); textureDirty=false;
        }
        g.blit(RenderPipelines.GUI_TEXTURED,TERRAIN_TEXTURE,left,top,0,0,size,size,width,width,width,width);
        String dimension=WorldNotesClient.dimension(level);
        List<Component> hovered=List.of();
        List<MapMarkerLayout.Marker> observations=new ArrayList<>();
        for (Entity entity:level.entitiesForRendering()) {
            if (entity==client.player || entity.isRemoved()) continue;
            if(!MapNavigation.inRange(Math.floorDiv(entity.getBlockX(),16),Math.floorDiv(entity.getBlockZ(),16),
                    Math.floorDiv(client.player.getBlockX(),16),Math.floorDiv(client.player.getBlockZ(),16),radius)) continue;
            if(settings.nearbyHeights && !(entity instanceof Player) && Math.abs(entity.getY()-client.player.getY())>24) continue;
            MapMarkerLayout.Kind kind;
            if (entity instanceof ItemEntity && settings.items) kind=MapMarkerLayout.Kind.ITEM;
            else if (entity instanceof Player && settings.players) kind=MapMarkerLayout.Kind.PLAYER;
            else if (entity instanceof NeutralMob && settings.neutralMobs) kind=MapMarkerLayout.Kind.NEUTRAL;
            else if (entity instanceof Enemy && !(entity instanceof NeutralMob) && settings.hostileMobs) kind=MapMarkerLayout.Kind.HOSTILE;
            else if (entity instanceof Mob && !(entity instanceof Enemy) && settings.neutralMobs) kind=MapMarkerLayout.Kind.NEUTRAL;
            else continue;
            int x=coordinate(entity.getX(),imageOriginX,size),z=coordinate(entity.getZ(),imageOriginZ,size);
            String name=entity instanceof ItemEntity item ? item.getItem().getHoverName().getString()+" x"+item.getItem().getCount() : entity.getDisplayName().getString();
            observations.add(new MapMarkerLayout.Marker(x,z,kind,name+" ("+entity.getBlockX()+", "+entity.getBlockY()+", "+entity.getBlockZ()+")"));
        }
        for(MapMarkerLayout.Group group:MapMarkerLayout.group(observations,expanded?22:10,size)) {
            int x=left+group.x(),z=top+group.z();
            List<MapMarkerLayout.Kind> kinds=Arrays.stream(MapMarkerLayout.Kind.values()).filter(kind->group.count(kind)>0).toList();
            if(kinds.size()==1) {
                if(expanded) symbol(g,x,z,kinds.getFirst());
                else g.fill(x-1,z-1,x+2,z+2,kinds.getFirst().color);
            }
            else {
                g.fill(x-3,z-3,x+4,z+4,0xE010151D);
                for(int i=0;i<kinds.size();i++) g.fill(x-2+i%2*3,z-2+i/2*3,x+i%2*3,z+i/2*3,kinds.get(i).color);
            }
            if(expanded && group.members().size()>1) {
                String count=group.members().size()>99?"99+":String.valueOf(group.members().size());
                int countX=Math.clamp(x-client.font.width(count)/2,left+1,left+size-client.font.width(count)-1);
                int countY=group.z()+14>=size?z-12:z+5;
                g.text(client.font,count,countX,countY,0xFFFFFFFF,true);
            }
            if(expanded && Math.abs(mouseX-x)<=9 && Math.abs(mouseY-z)<=9) {
                List<Component> details=new ArrayList<>();
                details.add(Component.literal(group.summary()));
                if(group.members().size()>1) {
                    group.members().stream().limit(5).forEach(marker->details.add(Component.literal(marker.description()).withColor(marker.kind().color&0xFFFFFF)));
                    if(group.members().size()>5) details.add(Component.literal("+"+(group.members().size()-5)+" more nearby"));
                }
                hovered=List.copyOf(details);
            }
        }
        for (Bookmark bookmark:markers) {
            if(bookmark.hidden || !dimension.equals(bookmark.dimension)) continue;
            int x=coordinate(bookmark.x,imageOriginX,size),z=coordinate(bookmark.z,imageOriginZ,size);
            boolean tracked=WorldNotesClient.isTracked(bookmark);
            if(tracked) { x=Math.clamp(x,3,size-4); z=Math.clamp(z,3,size-4); }
            else if(x<3||z<3||x>=size-3||z>=size-3) continue;
            dot(g,left+x,top+z,tracked?WorldNotesTheme.ACCENT:0xFFB5A0FF,2);
            if(expanded && Math.abs(mouseX-left-x)<=5 && Math.abs(mouseY-top-z)<=5) hovered=List.of(bookmark.displayName(),Component.literal(BookmarkScreen.coordinates(bookmark)));
        }
        int px=coordinate(client.player.getX(),imageOriginX,size),pz=coordinate(client.player.getZ(),imageOriginZ,size);
        arrow(g,left+px,top+pz,client.player.getYRot(),expanded);
        if(expanded) {
            String[] directions={"N","E","S","W"};
            int[] dx={size/2-3,size-9,size/2-3,3},dy={3,size/2-4,size-12,size/2-4};
            for(int i=0;i<4;i++) {
                g.fill(left+dx[i]-1,top+dy[i]-1,left+dx[i]+7,top+dy[i]+9,0xD010151D);
                CompassColors.draw(g,client.font,directions[i],left+dx[i],top+dy[i],dimension);
            }
            g.fill(left+px-10,top+pz-25,left+px+10,top+pz-14,0xD010151D);
            g.text(client.font,"You",left+px-8,top+pz-24,0xFFFFFFFF,true);
            if(Math.abs(mouseX-left-px)<=5 && Math.abs(mouseY-top-pz)<=5)
                hovered=List.of(Component.literal("You: "+client.player.getBlockX()+", "+client.player.getBlockY()+", "+client.player.getBlockZ()),
                        Component.literal("Facing ").append(CompassColors.label(MapNavigation.facing(client.player.getYRot()),dimension)));
        }
        return hovered;
    }
    private static void arrow(GuiGraphicsExtractor g,int centerX,int centerZ,float yaw,boolean expanded) {
        int scale=expanded?1:2,extent=expanded?9:5;
        for(int z=-extent;z<=extent;z++) for(int x=-extent;x<=extent;x++) {
            if(MapNavigation.arrowPixel(x*scale,z*scale,yaw) || MapNavigation.arrowPixel((x-1)*scale,z*scale,yaw) || MapNavigation.arrowPixel((x+1)*scale,z*scale,yaw)
                    || MapNavigation.arrowPixel(x*scale,(z-1)*scale,yaw) || MapNavigation.arrowPixel(x*scale,(z+1)*scale,yaw))
                g.fill(centerX+x,centerZ+z,centerX+x+1,centerZ+z+1,0xFF10151D);
        }
        for(int z=-extent;z<=extent;z++) for(int x=-extent;x<=extent;x++) if(MapNavigation.arrowPixel(x*scale,z*scale,yaw))
            g.fill(centerX+x,centerZ+z,centerX+x+1,centerZ+z+1,0xFFFFFFFF);
    }
    private static void symbol(GuiGraphicsExtractor g,int x,int y,MapMarkerLayout.Kind kind) {
        g.fill(x-3,y-3,x+4,y+4,0xC010151D);
        switch(kind) {
            case ITEM -> { g.fill(x,y-2,x+1,y+3,kind.color);g.fill(x-2,y,x+3,y+1,kind.color); }
            case HOSTILE -> { for(int d=-2;d<=2;d++){g.fill(x+d,y+d,x+d+1,y+d+1,kind.color);g.fill(x+d,y-d,x+d+1,y-d+1,kind.color);} }
            case PLAYER -> g.outline(x-2,y-2,5,5,kind.color);
            case NEUTRAL -> {g.fill(x-1,y-2,x+2,y+3,kind.color);g.fill(x-2,y-1,x+3,y+2,kind.color);}
        }
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
