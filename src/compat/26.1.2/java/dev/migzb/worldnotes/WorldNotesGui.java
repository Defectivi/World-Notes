package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Direct, compile-checked HUD calls for Minecraft 26.1.2. */
final class WorldNotesGui {
    private WorldNotesGui() { }
    static net.minecraft.client.gui.screens.Screen screen(Minecraft client) { return client.screen; }
    static boolean hidden(Minecraft client) { return client.options.hideGui; }
    static void overlay(Minecraft client, Component message) { client.gui.setOverlayMessage(message, false); }
    static void chat(Minecraft client, Component message) { client.getChatListener().handleSystemMessage(message, false); }
}
