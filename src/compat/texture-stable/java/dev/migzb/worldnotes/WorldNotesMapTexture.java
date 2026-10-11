package dev.migzb.worldnotes;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Dynamic textures use nearest-neighbour filtering, preserving individual block squares. */
final class WorldNotesMapTexture {
    private final Minecraft client;
    private final Identifier id;
    private final DynamicTexture texture;
    WorldNotesMapTexture(Minecraft client, Identifier id, int width) {
        this.client=client; this.id=id;
        texture=new DynamicTexture(()->"World Notes block map",width,width,false);
        client.getTextureManager().register(id,texture);
    }
    NativeImage image() { return texture.getPixels(); }
    void upload() { texture.upload(); }
    void close() { client.getTextureManager().release(id); }
}
