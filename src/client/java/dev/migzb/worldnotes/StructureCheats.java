package dev.migzb.worldnotes;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import java.util.function.Consumer;

/** Uses advertised server commands and structured, locale-independent locate responses. */
public final class StructureCheats {
    record Result(int x,int z,Integer y,int distance) { }
    private record Pending(Object owner,String profile,String dimension,String id,long deadline,Consumer<Result> result,Consumer<Component> status) { }
    private static Pending pending;
    static boolean permitted(Minecraft client) {
        if(client.player==null||client.level==null)return false;
        var root=client.player.connection.getCommands().getRoot();
        return allowed(client.player.permissions(),root.getChild("locate")!=null && root.getChild("locate").getChild("structure")!=null,
                root.getChild("tp")!=null || root.getChild("teleport")!=null);
    }
    static boolean allowed(net.minecraft.server.permissions.PermissionSet permissions,boolean locate,boolean teleport) {
        return permissions.hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)&&locate&&teleport;
    }
    static boolean busy(Object owner){return pending!=null && pending.owner==owner;}
    static void cancel(Object owner){if(busy(owner))pending=null;}
    static void locate(Minecraft client,Object owner,StructureCatalog.Choice choice,Consumer<Result> result,Consumer<Component> status) {
        if(!permitted(client)||!choice.dimension().equals(WorldNotesClient.dimension(client.level))||pending!=null)return;
        pending=new Pending(owner,WorldNotesClient.profile(client),choice.dimension(),choice.id(),System.nanoTime()+60_000_000_000L,result,status);
        status.accept(Component.literal("Finding nearest "+choice.name()+"..."));
        client.player.connection.sendCommand("locate structure "+choice.id());
    }
    static void tick(Minecraft client) {
        if(pending==null)return;
        if(client.level==null||client.player==null||!pending.profile.equals(WorldNotesClient.profile(client))
                ||!pending.dimension.equals(WorldNotesClient.dimension(client.level))) {pending=null;return;}
        if(System.nanoTime()>pending.deadline){var request=pending;pending=null;request.status.accept(Component.literal("Locate timed out. Try again."));}
    }
    static TranslatableContents translation(Component component,String key) {
        if(component.getContents() instanceof TranslatableContents contents) {
            if(contents.getKey().equals(key))return contents;
            for(Object arg:contents.getArgs())if(arg instanceof Component child){var found=translation(child,key);if(found!=null)return found;}
        }
        for(Component child:component.getSiblings()){var found=translation(child,key);if(found!=null)return found;}
        return null;
    }
    static Result parse(Component message,String id) {
        var success=translation(message,"commands.locate.structure.success");
        if(success==null||success.getArgs().length<3)return null;
        Object name=success.getArgs()[0];String reported=name instanceof Component component?component.getString():String.valueOf(name);
        if(!reported.equals(id)&&!reported.startsWith(id+" ("))return null;
        if(!(success.getArgs()[1] instanceof Component coordinates))return null;
        var point=translation(coordinates,"chat.coordinates");
        if(point==null||point.getArgs().length!=3)return null;
        try {
            int x=Integer.parseInt(String.valueOf(point.getArgs()[0])),z=Integer.parseInt(String.valueOf(point.getArgs()[2]));
            if(Math.abs((long)x)>30_000_000||Math.abs((long)z)>30_000_000)return null;
            String y=String.valueOf(point.getArgs()[1]);
            return new Result(x,z,y.equals("~")?null:Integer.valueOf(y),Integer.parseInt(String.valueOf(success.getArgs()[2])));
        }catch(NumberFormatException exception){return null;}
    }
    public static void onMessage(Component message) {
        if(pending==null)return;
        tick(Minecraft.getInstance());if(pending==null)return;
        Result located=parse(message,pending.id);
        if(located!=null){var request=pending;pending=null;request.result.accept(located);return;}
        if(translation(message,"commands.locate.structure.not_found")!=null){var request=pending;pending=null;request.status.accept(message);}
    }
    static void teleport(Minecraft client,Result target,double y) {
        if(!permitted(client)||!Double.isFinite(y)||Math.abs(y)>20_000_000)return;
        String command=client.player.connection.getCommands().getRoot().getChild("tp")!=null?"tp":"teleport";
        client.player.connection.sendCommand(command+" @s "+(target.x+0.5)+" "+y+" "+(target.z+0.5));
        WorldNotesGui.overlay(client,Component.literal("Teleport requested; the server decides whether it succeeds."));
    }
}
