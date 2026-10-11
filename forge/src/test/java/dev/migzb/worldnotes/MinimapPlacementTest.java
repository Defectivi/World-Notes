package dev.migzb.worldnotes;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinimapPlacementTest {
    @Test void draggingUsesExactPixelsInsteadOfTenPercentSteps() {
        var settings=new MinimapSettings();
        MinimapPlacement.move(settings,854,480,173,129);
        assertEquals(MinimapSettings.Position.CUSTOM,settings.position);
        var bounds=MinimapPlacement.bounds(settings,854,480);
        assertEquals(173,bounds.x());assertEquals(129,bounds.y());
        MinimapPlacement.move(settings,854,480,174,130);
        bounds=MinimapPlacement.bounds(settings,854,480);
        assertEquals(174,bounds.x());assertEquals(130,bounds.y());
    }
    @Test void savedAnchorsRoundTripAndResizeWithoutMovingOutsideTheWindow() {
        var settings=new MinimapSettings();MinimapPlacement.move(settings,854,480,321,211);
        var gson=new Gson();var saved=gson.fromJson(gson.toJson(settings),MinimapSettings.class);saved.normalize();
        assertEquals(MinimapPlacement.bounds(settings,854,480),MinimapPlacement.bounds(saved,854,480));
        double anchorX=saved.customAnchorX,anchorY=saved.customAnchorY;
        var small=MinimapPlacement.bounds(saved,240,180);
        assertTrue(small.x()>=3&&small.x()+small.size()+3<=240);
        assertTrue(small.y()>=16&&small.y()+small.size()+17<=180);
        assertEquals(anchorX,saved.customAnchorX);assertEquals(anchorY,saved.customAnchorY);
        assertEquals(MinimapPlacement.bounds(settings,854,480),MinimapPlacement.bounds(saved,854,480));
    }
    @Test void offScreenDraggingClampsBothEdgesIncludingHeaderAndFooterSpace() {
        var settings=new MinimapSettings();
        MinimapPlacement.move(settings,400,240,-1000,-1000);
        var bounds=MinimapPlacement.bounds(settings,400,240);assertEquals(3,bounds.x());assertEquals(16,bounds.y());
        MinimapPlacement.move(settings,400,240,1000,1000);
        bounds=MinimapPlacement.bounds(settings,400,240);
        assertEquals(400-96-3,bounds.x());assertEquals(240-96-17,bounds.y());
    }
    @Test void legacyPercentPositionsRemainAtTheSameCoordinates() {
        var settings=new Gson().fromJson("{\"position\":\"CUSTOM\",\"customX\":30,\"customY\":60}",MinimapSettings.class);
        settings.normalize();
        var bounds=MinimapPlacement.bounds(settings,854,480);
        assertEquals(9+(854-96-18)*30/100,bounds.x());
        assertEquals(56+(480-96-81)*60/100,bounds.y());
        assertEquals(-1,settings.customAnchorX);
    }
    @Test void movingAnEditorDraftDoesNotModifyTheSavedPosition() {
        var saved=new MinimapSettings();var before=MinimapPlacement.bounds(saved,854,480);
        var draft=saved.copy();MinimapPlacement.move(draft,854,480,120,200);
        assertEquals(before,MinimapPlacement.bounds(saved,854,480));
        assertEquals(MinimapSettings.Position.TOP_RIGHT,saved.position);
        assertFalse(MinimapPlacement.bounds(draft,854,480).contains(119,200));
        assertTrue(MinimapPlacement.bounds(draft,854,480).contains(120,200));
    }
}
