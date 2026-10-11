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
    private static TerrainCache rememberedSurface;
    private static String rememberedSurfaceContext;
    private static CaveLayer cave;
    private static boolean caveDirty;
    private static boolean netherOpen;
    private static int netherAnchor=Integer.MIN_VALUE;
    private static final Map<Long,Boolean> netherCaveColumns=new HashMap<>();
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
                && !(WorldNotesGui.screen(client) instanceof MinimapPositionScreen)
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
    static String layerLabel(){return cave==null?(netherOpen?"Nether terrain":"Surface"):"Cave Y "+cave.minY+".."+(cave.minY+15);}
    static void terrainChanged(BlockPos position) {
        if(cave==null||level==null)return;
        LevelChunk chunk=level.getChunkSource().getChunk(Math.floorDiv(position.getX(),16),Math.floorDiv(position.getZ(),16),false);
        if(chunk==null)return;
        var state=chunk.getBlockState(position);
        boolean open=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,position.getX()&15,position.getZ()&15)>position.getY()
                &&!state.isCollisionShapeFullBlock(level,position);
        long column=((long)position.getX()<<32)|(position.getZ()&0xFFFFFFFFL);
        if(netherOpen&&Boolean.FALSE.equals(netherCaveColumns.get(column)))open=false;
        int rgb=state.getMapColor(level,position).col;
        if(cave.changed(position.getX(),position.getY(),position.getZ(),open,rgb==0?0xFF484F59:0xFF000000|rgb))caveDirty=true;
    }
    private static boolean underground(Minecraft client) {
        int x=client.player.getBlockX(),z=client.player.getBlockZ();
        if(client.level.dimensionType().hasCeiling())return NetherTerrain.cave(netherBlocks(client.level),x,client.player.getBlockY(),z,
                client.level.dimensionType().logicalHeight()-8,cave!=null&&level==client.level);
        LevelChunk chunk=client.level.getChunkSource().getChunk(Math.floorDiv(x,16),Math.floorDiv(z,16),false);
        if(chunk==null)return cave!=null;
        int roof=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x&15,z&15);
        POSITION.set(x,roof,z);
        return roof>client.player.getEyeY() && chunk.getBlockState(POSITION).getFluidState().isEmpty();
    }
    private static NetherTerrain.Blocks netherBlocks(ClientLevel target) {
        return new NetherTerrain.Blocks() {
            public int space(int x,int y,int z) {
                if(y<target.getMinY()||y>=target.getMinY()+target.getHeight())return 0;
                LevelChunk chunk=target.getChunkSource().getChunk(Math.floorDiv(x,16),Math.floorDiv(z,16),false);
                if(chunk==null)return -1;
                POSITION.set(x,y,z);var state=chunk.getBlockState(POSITION);
                return state.isCollisionShapeFullBlock(target,POSITION)?0:state.getFluidState().isEmpty()?1:2;
            }
            public int color(int x,int y,int z) {
                LevelChunk chunk=target.getChunkSource().getChunk(Math.floorDiv(x,16),Math.floorDiv(z,16),false);
                if(chunk==null)return UNKNOWN;
                POSITION.set(x,y,z);int rgb=chunk.getBlockState(POSITION).getMapColor(target,POSITION).col;
                return rgb==0?0xFF484F59:0xFF000000|rgb;
            }
        };
    }
    static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            level=null; cache=null; rememberedSurface=null;rememberedSurfaceContext=null;cave=null; context=null; pixels=new int[0]; imageRevision=-1;netherAnchor=Integer.MIN_VALUE;netherCaveColumns.clear();
            if(terrainTexture!=null) { terrainTexture.close(); terrainTexture=null; textureSize=0; }
            return;
        }
        if (!settings.enabled && !(WorldNotesGui.screen(client) instanceof ExpandedMapScreen)
                && !(WorldNotesGui.screen(client) instanceof MinimapPositionScreen)) return;
        int renderRadius=Math.clamp(client.options.getEffectiveRenderDistance(),1,64);
        if(radius!=renderRadius || offsets.isEmpty() || scanZoom!=settings.zoom) {
            radius=renderRadius; offsets=MapNavigation.terrainOffsets(radius,settings.zoom); cursor=0; scanX=Integer.MIN_VALUE;scanZoom=settings.zoom;
        }
        String profile = WorldNotesClient.profile(client);
        if (!profile.equals(markerProfile) || markerRevision != BookmarkStore.revision()) {
            markers = BookmarkStore.forProfile(profile); markerProfile=profile; markerRevision=BookmarkStore.revision();
        }
        boolean below=underground(client);
        int layerY=NetherTerrain.supportY(netherBlocks(client.level),client.player.getBlockX(),client.player.getBlockY(),client.player.getBlockZ());
        netherOpen=client.level.dimensionType().hasCeiling()&&client.player.getBlockY()<client.level.dimensionType().logicalHeight();
        if(netherOpen&&(level!=client.level||netherAnchor==Integer.MIN_VALUE||Math.abs((long)client.player.getBlockY()-netherAnchor)>24))
            netherAnchor=Math.floorDiv(client.player.getBlockY(),32)*32+16;
        String band = below?"connected-cave:"+Math.floorDiv(layerY,16):netherOpen?"nether-open-v1:"+netherAnchor:"surface";
        // The versioned context preserves old coarse history without treating it as block-level data.
        String nextContext = "blocks-v2\n"+profile+"\n"+WorldNotesClient.dimension(client.level)+"\n"+band;
        if (level != client.level || !nextContext.equals(context)) {
            if(level!=client.level){rememberedSurface=null;rememberedSurfaceContext=null;}
            level=client.level; context=nextContext; cursor=0;
            cache=!below&&context.equals(rememberedSurfaceContext)?rememberedSurface:
                    new TerrainCache(WorldNotesClient.configDirectory().resolve("worldnotes/terrain"), context);
            if(!below){rememberedSurface=cache;rememberedSurfaceContext=context;}
            imageRevision=-1;
            cave=null;
            netherCaveColumns.clear();
            scanX=Integer.MIN_VALUE;
        }
        int cx=Math.floorDiv(client.player.getBlockX(),16), cz=Math.floorDiv(client.player.getBlockZ(),16);
        originX=client.player.getBlockX()-span()/2; originZ=client.player.getBlockZ()-span()/2;
        if(below) {
            int x=client.player.getBlockX(),y=layerY,z=client.player.getBlockZ();
            NetherTerrain.Blocks blocks=netherBlocks(level);
            CaveLayer.Terrain terrain=new CaveLayer.Terrain() {
                public int space(int x,int y,int z) {
                    if(y<level.getMinY()||y>=level.getMinY()+level.getHeight())return 0;
                    if(!MapNavigation.inRange(Math.floorDiv(x,16),Math.floorDiv(z,16),cx,cz,radius))return -1;
                    LevelChunk chunk=level.getChunkSource().getChunk(Math.floorDiv(x,16),Math.floorDiv(z,16),false);
                    if(chunk==null)return -1;
                    if(netherOpen) {
                        long column=((long)x<<32)|(z&0xFFFFFFFFL);
                        Boolean enclosed=netherCaveColumns.get(column);
                        if(enclosed==null) {
                            int result=NetherTerrain.enclosure(blocks,x,cave.referenceY(),z,level.dimensionType().logicalHeight()-8,true);
                            if(result<0)return -1;
                            enclosed=result==1;netherCaveColumns.put(column,enclosed);
                        }
                        if(!enclosed)return 0;
                    }
                    POSITION.set(x,y,z);
                    if(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x&15,z&15)<=y)return 0;
                    return chunk.getBlockState(POSITION).isCollisionShapeFullBlock(level,POSITION)?0:1;
                }
                public int color(int x,int y,int z) {
                    POSITION.set(x,y,z);
                    LevelChunk chunk=level.getChunkSource().getChunk(Math.floorDiv(x,16),Math.floorDiv(z,16),false);
                    if(chunk==null)return UNKNOWN;
                    int rgb=chunk.getBlockState(POSITION).getMapColor(level,POSITION).col;
                    return rgb==0?0xFF484F59:0xFF000000|rgb;
                }
            };
            long deadline=System.nanoTime()+6_000_000L;
            if(cave==null || caveDirty || cave.width!=radius*32+1 || cave.followPlayer(terrain,x,y,z,deadline)) {
                cave=new CaveLayer(x,y,z,radius);imageRevision=-1;caveDirty=false;netherCaveColumns.clear();
            } else if(Math.abs((long)x-cave.centerX)>=Math.min(16,radius*8)||Math.abs((long)z-cave.centerZ)>=Math.min(16,radius*8)) {
                cave=cave.recenter(x,z);imageRevision=-1;
                netherCaveColumns.keySet().removeIf(key->Math.abs((long)(int)(key>>32)-x)>radius*16||Math.abs((long)(int)(long)key-z)>radius*16);
            }
            if(tickCount%20==0)cave.retryUnknown();
            cave.advance(terrain,4096,deadline);
        } else {
        if(cx!=scanX || cz!=scanZ || scanZoom!=settings.zoom || cursor>=offsets.size() && tickCount%4==0) {
            scanX=cx;scanZ=cz;scanZoom=settings.zoom;cursor=0;
        }
        int samples=0;
        NetherTerrain.Blocks floorBlocks=netherOpen?netherBlocks(level):null;
        long deadline=System.nanoTime()+6_000_000L;
        for (int work=0; work<512 && cursor<offsets.size() && samples<32 && System.nanoTime()<deadline; work++) {
            int[] offset=offsets.get(cursor++);
            int x=cx+offset[0], z=cz+offset[1];
            if(cache.has(x,z))continue;
            LevelChunk chunk=level.getChunkSource().getChunk(x,z,false);
            if (chunk==null) { cache.request(x,z);continue; }
            int[] colors=new int[TerrainTileFile.SAMPLES];
            for (int row=0; row<16; row++) for (int col=0; col<16; col++)
                colors[row*16+col]=floorBlocks==null?sampleColor(chunk,x*16+col,z*16+row):
                        NetherTerrain.floor(floorBlocks,x*16+col,z*16+row,netherAnchor,level.getMinY(),level.dimensionType().logicalHeight()-8);
            cache.store(x,z,colors); samples++;
        }
        if (tickCount % 100 == 99) cache.trim(cx,cz,radius+2);
        }
        tickCount++;
        int width=span();
        if(tickCount%2==0 || pixels.length!=width*width || imageRevision<0
                || Math.abs((long)originX-imageOriginX)>width/4 || Math.abs((long)originZ-imageOriginZ)>width/4) {
            long revision=cave==null?cache.revision():cave.revision();
            if(pixels.length!=width*width || imageOriginX!=originX || imageOriginZ!=originZ || imageRevision!=revision) {
                pixels=new int[width*width];
                if(cave==null)cache.copyRegion(originX,originZ,width,pixels);
                else cave.copyRegion(originX,originZ,width,pixels);
                int known=0;for(int color:pixels)if(color!=UNKNOWN)known++;coverage=known*100/pixels.length;
                imageOriginX=originX; imageOriginZ=originZ; imageRevision=revision; textureDirty=true;
            }
        }
    }
    static int[] bounds(Minecraft client) {
        var bounds=MinimapPlacement.bounds(settings,client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());
        return new int[]{bounds.x(),bounds.y(),bounds.size()};
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
        renderPreview(graphics,client,b[0],b[1],b[2]);
    }
    static void renderPreview(GuiGraphicsExtractor graphics,Minecraft client,int x,int y,int size) {
        if(client.player==null||client.level==null)return;
        int[] b={x,y,size};
        WorldNotesTheme.panel(graphics,b[0]-3,b[1]-3,b[2]+6,b[2]+17,WorldNotesTheme.dimensionAccent(WorldNotesClient.dimension(client.level)));
        renderMap(graphics,client,b[0],b[1],b[2],-1,-1,false);
        String dimension=WorldNotesClient.dimension(client.level);
        CompassColors.draw(graphics,client.font,"N",b[0]+b[2]/2-3,b[1]-13,dimension);
        graphics.text(client.font,CompassColors.label(MapNavigation.facing(client.player.getYRot()),dimension)
                .copy().append(cave==null?"":" | Cave "+cave.minY),b[0]+2,b[1]+b[2]+3,0xFFFFFFFF,true);
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
            if(cave!=null&&!cave.contains(entity.getBlockX(),entity.getBlockY(),entity.getBlockZ()))continue;
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
            if(cave!=null&&!cave.contains((int)Math.floor(bookmark.x),(int)Math.floor(bookmark.y),(int)Math.floor(bookmark.z))
                    &&!cave.contains((int)Math.floor(bookmark.x),(int)Math.floor(bookmark.y)+1,(int)Math.floor(bookmark.z)))continue;
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
    private static int sampleColor(LevelChunk chunk, int x, int z) {
        int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
        // Skip colorless surface blocks such as glass, without scanning entire columns.
        for (int depth = 0; depth < 16 && height >= level.getMinY(); depth++, height--) {
            POSITION.set(x, height, z);
            int rgb = chunk.getBlockState(POSITION).getMapColor(level, POSITION).col;
            if (rgb != 0) return 0xFF000000 | rgb;
        }
        return UNKNOWN;
    }
}
