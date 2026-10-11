package dev.migzb.worldnotes;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class CompassColors {
    static int base(String direction) {
        String name=direction.toLowerCase(java.util.Locale.ROOT);
        if(name.startsWith("n"))return 0x62B6FF;
        if(name.startsWith("s"))return 0xFF6B5E;
        if(name.startsWith("e"))return 0xFFCC4D;
        return 0xC895FF;
    }
    static Component label(String direction,String dimension) {
        return Bookmark.gradientText(direction,base(direction),WorldNotesTheme.dimensionAccent(dimension)&0xFFFFFF);
    }
    static void draw(GuiGraphicsExtractor g,Font font,String direction,int x,int y,String dimension) {
        if(direction.length()>1){g.text(font,label(direction,dimension),x,y,0xFFFFFFFF,true);return;}
        int width=font.width(direction),start=base(direction),end=WorldNotesTheme.dimensionAccent(dimension);
        // A one-letter component cannot carry a text gradient: colour its glyph columns instead.
        for(int column=0;column<width;column++) {
            double blend=Math.clamp((column-(width-1)*0.25)/Math.max(1,(width-1)*0.5),0,1);
            int color=0xFF000000;
            for(int shift:new int[]{16,8,0})color|=(int)Math.round(((start>>shift)&255)*(1-blend)+((end>>shift)&255)*blend)<<shift;
            g.enableScissor(x+column,y,x+column+1,y+font.lineHeight);
            g.text(font,direction,x,y,color,true);g.disableScissor();
        }
    }
}
