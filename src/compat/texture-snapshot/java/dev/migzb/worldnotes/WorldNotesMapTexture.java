package dev.migzb.worldnotes;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureResources;
import net.minecraft.resources.Identifier;

/** 26.4 replaced DynamicTexture with TextureResources and explicit image uploads. */
final class WorldNotesMapTexture {
    private final Minecraft client;
    private final Identifier id;
    private final NativeImage image;
    private final TextureResources texture;
    WorldNotesMapTexture(Minecraft client, Identifier id, int width) {
        this.client=client; this.id=id;
        image=new NativeImage(width,width,true);
        texture=TextureResources.from2dImage(()->"World Notes block map",image,
                RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST));
        client.getTextureManager().register(id,texture);
    }
    NativeImage image() { return image; }
    void upload() { image.writeToGpuTexture(RenderSystem.getDevice().createCommandEncoder(),texture.texture()); }
    void close() { client.getTextureManager().release(id); image.close(); }
}
