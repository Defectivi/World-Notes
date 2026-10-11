package dev.migzb.worldnotes.mixin;

import dev.migzb.worldnotes.WorldNotesClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ClientPacketListener.class,remap=false)
public abstract class CaveTerrainMixin {
    @Inject(method="handleBlockUpdate",at=@At("TAIL"),require=1)
    private void worldnotes$caveBlockChanged(ClientboundBlockUpdatePacket packet,CallbackInfo callback) {
        WorldNotesClient.terrainChanged(packet.getPos());
    }
    @Inject(method="handleChunkBlocksUpdate",at=@At("TAIL"),require=1)
    private void worldnotes$caveSectionChanged(ClientboundSectionBlocksUpdatePacket packet,CallbackInfo callback) {
        packet.runUpdates((position,state)->WorldNotesClient.terrainChanged(position));
    }
}
