package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Loader hooks are client-only; the shared features and local saves are identical across loaders. */
@Mod(value = "worldnotes", dist = Dist.CLIENT)
@EventBusSubscriber(modid = "worldnotes", value = Dist.CLIENT)
public final class WorldNotesNeoForge {
    public WorldNotesNeoForge() { }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        WorldNotesClient.initialize(FMLPaths.CONFIGDIR.get());
        WorldNotesClient.keyMappings().forEach(event::register);
    }

    @SubscribeEvent
    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath("worldnotes", "coordinates"), WorldNotesHud::render);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        WorldNotesClient.tick(Minecraft.getInstance());
    }
}
