package dev.migzb.worldnotes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
/** Render submissions only: never changes block state or entity glow flags. */
public final class WorldNotesBlockHighlight {
    private WorldNotesBlockHighlight(){}
    private static volatile BlockPos highlighted;
    static void update(Minecraft client){
        highlighted=null;
        if(client.player==null||client.level==null||WorldNotesGui.hidden(client)||WorldNotesMinimap.settings()==null||!WorldNotesMinimap.settings().rainbowHighlight)return;
        Bookmark target=WorldNotesClient.trackedBookmark(client);
        if(target==null||target.hidden||!target.dimension.equals(WorldNotesClient.dimension(client.level)))return;
        BlockPos block=BlockPos.containing(target.x,target.y,target.z);
        if(client.player.distanceToSqr(block.getX()+0.5,block.getY()+0.5,block.getZ()+0.5)>1024*1024)return;
        highlighted=block;
    }
    public static void submit(LevelRenderState state,SubmitNodeCollector collector){
        BlockPos block=highlighted;
        if(block==null)return;
        var camera=state.cameraRenderState.pos;
        PoseStack pose=new PoseStack();pose.translate(block.getX()-camera.x,block.getY()-camera.y,block.getZ()-camera.z);
        int[] spectrum={0xFF6060,0xFFD460,0x83EF83,0x60DFFF,0xA18AFF,0xFF85DC};
        double phase=(System.nanoTime()/1_000_000_000.0)%6,fraction=phase-Math.floor(phase);
        int a=spectrum[(int)phase],b=spectrum[((int)phase+1)%6],rgb=0;
        for(int shift:new int[]{0,8,16})rgb|=(int)Math.round(((a>>shift)&255)*(1-fraction)+((b>>shift)&255)*fraction)<<shift;
        var shape=Shapes.create(new AABB(-0.004,-0.004,-0.004,1.004,1.004,1.004));
        final int color=rgb;
        collector.submitCustomGeometry(pose,RenderTypes.lines(),(submittedPose,vertices)->{
            shape.forAllEdges((x1,y1,z1,x2,y2,z2)->{
                edge(submittedPose,vertices,x1,y1,z1,x2,y2,z2,0x55000000|color,6.0F);
                edge(submittedPose,vertices,x1,y1,z1,x2,y2,z2,0xFF000000|color,2.0F);
            });
        });
    }
    private static void edge(PoseStack.Pose pose,VertexConsumer vertices,double x1,double y1,double z1,double x2,double y2,double z2,int color,float width){
        double length=Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
        float nx=(float)((x2-x1)/length),ny=(float)((y2-y1)/length),nz=(float)((z2-z1)/length);
        vertices.addVertex(pose,(float)x1,(float)y1,(float)z1).setColor(color).setNormal(pose,nx,ny,nz).setLineWidth(width);
        vertices.addVertex(pose,(float)x2,(float)y2,(float)z2).setColor(color).setNormal(pose,nx,ny,nz).setLineWidth(width);
    }
}
