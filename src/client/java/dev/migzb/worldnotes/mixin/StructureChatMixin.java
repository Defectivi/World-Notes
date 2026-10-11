package dev.migzb.worldnotes.mixin;
import dev.migzb.worldnotes.StructureCheats;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ClientPacketListener.class,remap=false)
public abstract class StructureChatMixin {
    @Inject(method="handleSystemChat",at=@At("TAIL"),require=1)
    private void worldnotes$locateResponse(ClientboundSystemChatPacket packet,CallbackInfo callback){StructureCheats.onMessage(packet.content());}
}
