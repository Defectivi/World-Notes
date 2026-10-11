package dev.migzb.worldnotes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public final class WorldNotesFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorldNotesClient.initialize(FabricLoader.getInstance().getConfigDir());
        WorldNotesClient.keyMappings().forEach(KeyMappingHelper::registerKeyMapping);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("worldnotes", "coordinates"), WorldNotesHud::render);
        ClientTickEvents.END_CLIENT_TICK.register(WorldNotesClient::tick);
    }
}
