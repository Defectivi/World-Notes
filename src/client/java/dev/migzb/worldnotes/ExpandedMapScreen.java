package dev.migzb.worldnotes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
final class ExpandedMapScreen extends Screen {
    ExpandedMapScreen() { super(Component.literal("World Notes map")); }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Settings"),b->WorldNotesClient.setScreen(minecraft,new MinimapSettingsScreen(this))).bounds(8,height-27,80,20).build());
        addRenderableWidget(Button.builder(Component.literal("Zoom"),b->WorldNotesMinimap.zoom(minecraft)).bounds(width/2-40,height-27,80,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(width-88,height-27,80,20).build());
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(WorldNotesClient.mapKey(event)){onClose();return true;}
        if(WorldNotesClient.zoomKey(event)){WorldNotesMinimap.zoom(minecraft);return true;}
        return super.keyPressed(event);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float p){WorldNotesTheme.background(g,width,height);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float p){
        super.extractRenderState(g,x,y,p);
        if(minecraft.level==null||minecraft.player==null){onClose();return;}
        int size=Math.min(width-20,height-90),left=(width-size)/2;
        g.centeredText(font,BookmarkScreen.dimensionComponent(WorldNotesClient.dimension(minecraft.level)),width/2,7,0xFFFFFFFF);
        WorldNotesMinimap.renderMap(g,minecraft,left,27,size,x,y,true);
        g.centeredText(font,Component.literal(WorldNotesMinimap.settings().radiusChunks+" chunks radius | North ^"),width/2,18,WorldNotesTheme.MUTED);
        g.centeredText(font,Component.literal("Items").withColor(0xFFD96B).append(Component.literal("  Neutral").withColor(0x86E8A1)).append(Component.literal("  Hostile").withColor(0xFF6B75)).append(Component.literal("  Players").withColor(0x6DDFFF)),width/2,height-42,0xFFFFFFFF);
    }
    @Override public boolean isPauseScreen(){return false;}
}
