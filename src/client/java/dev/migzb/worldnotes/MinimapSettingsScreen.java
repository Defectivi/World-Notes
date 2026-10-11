package dev.migzb.worldnotes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;
final class MinimapSettingsScreen extends Screen {
    private final Screen parent;
    private final MinimapSettings draft=WorldNotesMinimap.settings().copy();
    private boolean failed;
    private Button cheat;
    MinimapSettingsScreen(Screen parent) { super(Component.literal("World Notes settings")); this.parent=parent; }
    private void option(int index,Supplier<String> label,Runnable change) {
        int w=Math.min(170,(width-28)/2),left=width/2-w-3;
        addRenderableWidget(Button.builder(Component.literal(label.get()),b->{change.run();b.setMessage(Component.literal(label.get()));})
                .bounds(left+index%2*(w+6),32+index/2*Math.min(23,(height-74)/6),w,Math.min(20,(height-74)/6-2)).build());
    }
    private static String state(boolean enabled) { return enabled?"ON":"OFF"; }
    @Override protected void init() {
        option(0,()->"Minimap: "+state(draft.enabled),()->draft.enabled=!draft.enabled);
        option(1,()->"Rainbow block: "+state(draft.rainbowHighlight),()->draft.rainbowHighlight=!draft.rainbowHighlight);
        option(2,()->"Items: "+state(draft.items),()->draft.items=!draft.items);
        option(3,()->"Players: "+state(draft.players),()->draft.players=!draft.players);
        option(4,()->"Neutral/passive: "+state(draft.neutralMobs),()->draft.neutralMobs=!draft.neutralMobs);
        option(5,()->"Hostile mobs: "+state(draft.hostileMobs),()->draft.hostileMobs=!draft.hostileMobs);
        int cheatWidth=Math.min(170,(width-28)/2);
        cheat=addRenderableWidget(Button.builder(Component.literal("Cheat"),b->{if(StructureCheats.permitted(minecraft))WorldNotesClient.setScreen(minecraft,new StructureCheatScreen(this));})
                .bounds(width/2-cheatWidth-3,32+3*Math.min(23,(height-74)/6),cheatWidth,Math.min(20,(height-74)/6-2)).build());
        cheat.active=StructureCheats.permitted(minecraft);
        cheat.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Requires operator/cheats level 2 and /locate + /tp permission.")));
        option(7,()->"Size: "+draft.size,()->draft.size=draft.size==160?96:draft.size+32);
        option(8,()->"Position: "+draft.position.name().replace('_',' '),()->{
            var next=MinimapSettings.Position.values()[(draft.position.ordinal()+1)%5];
            if(next==MinimapSettings.Position.CUSTOM)WorldNotesClient.setScreen(minecraft,new MinimapPositionScreen(this,draft));else draft.position=next;
        });
        option(9,()->"Move map...",()->WorldNotesClient.setScreen(minecraft,new MinimapPositionScreen(this,draft)));
        option(10,()->"Reset position",()->{draft.position=MinimapSettings.Position.TOP_RIGHT;draft.customAnchorX=-1;draft.customAnchorY=-1;draft.customX=100;draft.customY=0;clearWidgets();init();});
        option(11,()->"Nearby heights: "+state(draft.nearbyHeights),()->draft.nearbyHeights=!draft.nearbyHeights);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->{if(WorldNotesMinimap.apply(draft))onClose();else failed=true;}).bounds(width/2-84,height-27,80,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(width/2+4,height-27,80,20).build());
    }
    @Override public void tick(){cheat.active=StructureCheats.permitted(minecraft);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float p) { WorldNotesTheme.background(g,width,height); }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float p) {
        super.extractRenderState(g,x,y,p);g.centeredText(font,title,width/2,12,WorldNotesTheme.TEXT);
        if(failed)g.centeredText(font,Component.translatable("worldnotes.save_failed"),width/2,height-40,0xFFFF8C83);
    }
    @Override public void onClose() { WorldNotesClient.setScreen(minecraft,parent); }
    @Override public boolean isPauseScreen() { return false; }
}
