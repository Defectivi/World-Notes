package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

final class StructureCheatScreen extends Screen {
    private final Screen parent;
    private final String dimension,profile;
    private int page,left,panelWidth;
    private EditBox search,arrivalY;
    private Button teleport;
    private final java.util.List<Button> findButtons=new java.util.ArrayList<>();
    private StructureCheats.Result target;
    private Component status=Component.literal("Filter a structure, then find the nearest one.");
    StructureCheatScreen(Screen parent) {
        super(Component.literal("Cheat: nearby structures"));this.parent=parent;
        Minecraft client=Minecraft.getInstance();
        dimension=client.level==null?"unknown":WorldNotesClient.dimension(client.level);
        profile=WorldNotesClient.profile(client);
    }
    @Override protected void init() {
        findButtons.clear();
        panelWidth=Math.min(360,width-20);left=(width-panelWidth)/2;
        if(search==null){
            search=new EditBox(font,left,46,panelWidth-56,18,Component.literal("Filter structures"));
            search.setMaxLength(100);search.setHint(Component.literal("Filter structures"));
            search.setResponder(value->{page=0;clearWidgets();init();});
        }else{search.setX(left);search.setY(46);search.setWidth(panelWidth-56);}
        addRenderableWidget(search);
        List<StructureCatalog.Choice> choices=StructureCatalog.choices(dimension,search.getValue());
        int pageSize=Math.max(1,(height-167)/22),pages=Math.max(1,(choices.size()+pageSize-1)/pageSize);
        page=Math.clamp(page,0,pages-1);
        addRenderableWidget(Button.builder(Component.literal("<"),b->{page--;clearWidgets();init();}).bounds(left+panelWidth-52,46,24,18).build()).active=page>0;
        addRenderableWidget(Button.builder(Component.literal(">"),b->{page++;clearWidgets();init();}).bounds(left+panelWidth-26,46,24,18).build()).active=page<pages-1;
        for(int i=page*pageSize;i<Math.min(choices.size(),(page+1)*pageSize);i++){
            var choice=choices.get(i);
            findButtons.add(addRenderableWidget(Button.builder(Component.literal("Find "+choice.name()),b->{
                target=null;
                StructureCheats.locate(minecraft,this,choice,located->{target=located;status=Component.literal("Nearest: "+located.x()+", "+located.z()+" | "+located.distance()+" blocks");
                    if(located.y()!=null)arrivalY.setValue(String.valueOf(located.y()));},message->status=message);
            }).bounds(left,72+(i-page*pageSize)*22,panelWidth,18).build()));
        }
        if(arrivalY==null){arrivalY=new EditBox(font,left+112,height-51,Math.max(50,panelWidth-112),18,Component.literal("Arrival Y"));
            arrivalY.setMaxLength(12);arrivalY.setValue(minecraft.player==null?"64":String.valueOf(minecraft.player.getBlockY()));
        }else{arrivalY.setX(left+112);arrivalY.setY(height-51);arrivalY.setWidth(Math.max(50,panelWidth-112));}
        addRenderableWidget(arrivalY);
        teleport=addRenderableWidget(Button.builder(Component.literal("Teleport"),b->{
            if(target!=null && validY() && StructureCheats.permitted(minecraft)){
                StructureCheats.teleport(minecraft,target,Double.parseDouble(arrivalY.getValue()));
                status=Component.literal("Teleport requested.");
            }
        }).bounds(left,height-27,(panelWidth-4)/2,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(left+(panelWidth+4)/2,height-27,(panelWidth-4)/2,20).build());
        setInitialFocus(search);
        updateButtons();
    }
    private boolean validY(){try{double value=Double.parseDouble(arrivalY.getValue());return Double.isFinite(value)&&Math.abs(value)<=20_000_000;}catch(NumberFormatException exception){return false;}}
    @Override public void tick(){
        if(minecraft.level==null||!dimension.equals(WorldNotesClient.dimension(minecraft.level))||!profile.equals(WorldNotesClient.profile(minecraft))){onClose();return;}
        updateButtons();
        if(!StructureCheats.permitted(minecraft))status=Component.literal("Requires cheats/OP and /locate + /tp permission.");
    }
    private void updateButtons(){
        boolean enabled=StructureCheats.permitted(minecraft)&&!StructureCheats.busy(this);
        findButtons.forEach(button->button.active=enabled);
        teleport.active=target!=null&&validY()&&enabled;
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float partial){WorldNotesTheme.background(g,width,height);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){
        super.extractRenderState(g,x,y,partial);
        g.centeredText(font,title,width/2,10,WorldNotesTheme.TEXT);
        g.centeredText(font,BookmarkScreen.dimensionComponent(dimension),width/2,28,WorldNotesTheme.TEXT);
        if(StructureCatalog.choices(dimension,search.getValue()).isEmpty())g.centeredText(font,Component.literal("No matching built-in structures."),width/2,76,WorldNotesTheme.MUTED);
        g.centeredText(font,Component.literal(font.plainSubstrByWidth(status.getString(),panelWidth)),width/2,height-82,WorldNotesTheme.TEXT);
        g.centeredText(font,Component.literal(font.plainSubstrByWidth("Locate gives X/Z; choose a safe arrival Y.",panelWidth)),width/2,height-69,WorldNotesTheme.MUTED);
        g.text(font,"Arrival Y:",left,height-47,WorldNotesTheme.TEXT,false);
    }
    @Override public void onClose(){StructureCheats.cancel(this);WorldNotesClient.setScreen(minecraft,parent);}
    @Override public boolean isPauseScreen(){return false;}
}
