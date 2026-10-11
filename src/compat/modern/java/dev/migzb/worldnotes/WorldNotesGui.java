package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** HUD ownership moved from Minecraft/Gui into Gui's HUD in 26.2. */
final class WorldNotesGui {
    private WorldNotesGui() { }
    static boolean hidden(Minecraft client) { return client.gui.hud.isHidden(); }
    static void overlay(Minecraft client, Component message) { client.gui.hud.setOverlayMessage(message, false); }
    static void chat(Minecraft client, Component message) { client.gui.chatListener().handleSystemMessage(message, false); }
}
