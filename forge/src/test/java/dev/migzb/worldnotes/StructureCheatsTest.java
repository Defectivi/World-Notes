package dev.migzb.worldnotes;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StructureCheatsTest {
    @Test void operatorLevelAndBothCommandsAreRequiredTogether() {
        var operator=net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER;
        var moderator=net.minecraft.server.permissions.LevelBasedPermissionSet.MODERATOR;
        assertTrue(StructureCheats.allowed(operator,true,true));
        assertFalse(StructureCheats.allowed(moderator,true,true));
        assertFalse(StructureCheats.allowed(operator,false,true));
        assertFalse(StructureCheats.allowed(operator,true,false));
        assertFalse(StructureCheats.allowed(net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS,true,true));
    }
    private Component located(String name,Object x,Object y,Object z) {
        return Component.translatable("commands.locate.structure.success",name,
                Component.translatable("chat.coordinates",x,y,z),320);
    }
    @Test void nativeResponsePreservesNegativeCoordinatesAndUnknownHeight() {
        assertEquals(new StructureCheats.Result(-123,456,null,320),
                StructureCheats.parse(located("minecraft:fortress",-123,"~",456),"minecraft:fortress"));
    }
    @Test void groupedResponseAndNestedComponentsAreRecognized() {
        var message=Component.literal("").append(located("#minecraft:village (minecraft:village_plains)",10,72,20));
        assertEquals(new StructureCheats.Result(10,20,72,320),StructureCheats.parse(message,"#minecraft:village"));
    }
    @Test void unrelatedAndMalformedMessagesCannotBecomeTeleportTargets() {
        assertNull(StructureCheats.parse(located("minecraft:end_city",1,"~",2),"minecraft:fortress"));
        assertNull(StructureCheats.parse(Component.literal("Nearest fortress at [1, ~, 2]"),"minecraft:fortress"));
        assertNull(StructureCheats.parse(located("minecraft:fortress",30_000_001,"~",2),"minecraft:fortress"));
        assertNull(StructureCheats.parse(located("minecraft:fortress",1,"oops",2),"minecraft:fortress"));
    }
    @Test void catalogContainsOnlyInstalledStructuresInRequestedDimension() {
        var nether=StructureCatalog.choices("minecraft:the_nether","");
        assertFalse(nether.isEmpty());
        assertTrue(nether.stream().allMatch(c->c.dimension().equals("minecraft:the_nether")));
        assertTrue(nether.stream().anyMatch(c->c.id().equals("minecraft:fortress")));
        assertTrue(StructureCatalog.choices("minecraft:the_nether","village").isEmpty());
        assertEquals(1,StructureCatalog.choices("minecraft:the_end"," CITY ").size());
        assertTrue(StructureCatalog.choices("custom:moon","").isEmpty());
        assertFalse(StructureCatalog.choices("minecraft:overworld","village").isEmpty());
        for(String dimension:new String[]{"minecraft:overworld","minecraft:the_nether","minecraft:the_end"})
            for(var choice:StructureCatalog.choices(dimension,""))assertNotNull(getClass().getResource("/"+choice.resource()));
    }
}
