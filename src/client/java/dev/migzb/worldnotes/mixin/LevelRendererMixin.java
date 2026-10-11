package dev.migzb.worldnotes.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.migzb.worldnotes.WorldNotesBlockHighlight;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=LevelRenderer.class,remap=false)
public abstract class LevelRendererMixin {
    @Inject(method="submitEntities",at=@At("TAIL"),require=1)
    private void worldnotes$highlight(PoseStack pose,LevelRenderState state,SubmitNodeCollector collector,CallbackInfo callback){
        WorldNotesBlockHighlight.submit(state,collector);
    }
}
