package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod.EventBusSubscriber(modid = "worldnotes", value = Dist.CLIENT)
public final class WorldNotesForgeClient {
    private WorldNotesForgeClient() { }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        WorldNotesClient.initialize(FMLPaths.CONFIGDIR.get());
        WorldNotesClient.keyMappings().forEach(event::register);
    }

    @SubscribeEvent
    public static void registerHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(Identifier.fromNamespaceAndPath("worldnotes", "coordinates"), WorldNotesHud::render);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent.Post event) {
        WorldNotesClient.tick(Minecraft.getInstance());
    }
}
