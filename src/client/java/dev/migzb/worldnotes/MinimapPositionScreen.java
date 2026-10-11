package dev.migzb.worldnotes;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A separate draft makes dragging, Escape, and window resizing independent of saved settings. */
final class MinimapPositionScreen extends Screen {
    private final Screen parent;
    private final MinimapSettings draft;
    private boolean dragging,failed;
    private double grabX,grabY;
    MinimapPositionScreen(Screen parent,MinimapSettings settings) {
        super(Component.literal("Move minimap"));this.parent=parent;draft=settings.copy();
    }
    @Override protected void init() {
        dragging=false;
        var initial=MinimapPlacement.bounds(draft,width,height);
        if(draft.position!=MinimapSettings.Position.CUSTOM||draft.customAnchorX<0||draft.customAnchorY<0)
            MinimapPlacement.move(draft,width,height,initial.x(),initial.y());
        int buttonWidth=Math.min(110,(width-28)/2);
        addRenderableWidget(Button.builder(Component.literal("Save & play"),button->{
            if(WorldNotesMinimap.apply(draft))WorldNotesClient.setScreen(minecraft,null);else failed=true;
        }).bounds(width/2-buttonWidth-3,height-27,buttonWidth,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),button->onClose())
                .bounds(width/2+3,height-27,buttonWidth,20).build());
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if(super.mouseClicked(event,doubleClick))return true;
        var bounds=MinimapPlacement.bounds(draft,width,height);
        if(event.button()==0&&bounds.contains(event.x(),event.y())) {
            dragging=true;grabX=event.x()-bounds.x();grabY=event.y()-bounds.y();return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double deltaX,double deltaY) {
        if(dragging&&event.button()==0) {
            MinimapPlacement.move(draft,width,height,event.x()-grabX,event.y()-grabY);return true;
        }
        return super.mouseDragged(event,deltaX,deltaY);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if(dragging&&event.button()==0){dragging=false;return true;}
        return super.mouseReleased(event);
    }
    @Override public void tick(){if(minecraft.player==null||minecraft.level==null)WorldNotesClient.setScreen(minecraft,null);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float partial){ }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial) {
        var bounds=MinimapPlacement.bounds(draft,width,height);
        WorldNotesMinimap.renderPreview(g,minecraft,bounds.x(),bounds.y(),bounds.size());
        g.outline(bounds.x()-1,bounds.y()-1,bounds.size()+2,bounds.size()+2,dragging?WorldNotesTheme.ACCENT:0xFFFFFFFF);
        String hint=failed?Component.translatable("worldnotes.save_failed").getString():"Drag the map, then Save & play";
        hint=font.plainSubstrByWidth(hint,width-20);
        int hintWidth=font.width(hint);
        g.fill(width/2-hintWidth/2-4,5,width/2+hintWidth/2+4,19,0xD010151D);
        g.centeredText(font,Component.literal(hint),width/2,8,failed?0xFFFF8C83:WorldNotesTheme.TEXT);
        super.extractRenderState(g,x,y,partial);
    }
    @Override public void onClose(){WorldNotesClient.setScreen(minecraft,parent);}
    @Override public boolean isPauseScreen(){return false;}
}
