package modcore;

import modcore.radar.ItemScanCoverage;
import modcore.radar.ItemScanCoverage.Column;
import modcore.radar.ItemScanCoverage.Plan;
import modcore.radar.ItemScanCoverage.Pos;
import modcore.radar.ItemScanCoverage.PatrolCursor;
import modcore.radar.ItemScanWindow;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ItemScanCoverageTest {
    @Test
    void groundPlayerPlanIncludesAllLoadedUpperAndBasementLayers() {
        var w=ItemScanWindow.around(0,0,0);
        var c=new ItemScanCoverage(w,Map.of(key(0,0),new Column(ItemScanCoverage.mask(-2,4),1)));
        var got=drain(new Plan(null,c,true,List.of(),null,0));
        assertEquals(Set.of(new Pos(0,0,-2),new Pos(0,0,-1),new Pos(0,0,0),new Pos(0,0,1),
                new Pos(0,0,2),new Pos(0,0,3),new Pos(0,0,4)),new HashSet<>(got));
        assertEquals(7,got.size());
    }

    @Test
    void mixedColumnsDoNotApplyTallBuildingLayersToSingleFloorColumns() {
        var w=ItemScanWindow.around(7,0,1);
        var c=new ItemScanCoverage(w,Map.of(
                key(0,0),new Column(ItemScanCoverage.mask(0,0),1),
                key(1,0),new Column(ItemScanCoverage.mask(4,4),1),
                key(0,-1),new Column(ItemScanCoverage.mask(-2,-2),1),
                key(1,-1),new Column(0,1)));
        var got=drain(new Plan(null,c,true,List.of(),null,0));
        assertEquals(Set.of(new Pos(6,0,0),new Pos(6,1,0),new Pos(7,0,0),new Pos(7,1,0),
                new Pos(8,0,4),new Pos(8,1,4),new Pos(6,-1,-2),new Pos(7,-1,-2)),new HashSet<>(got));
        assertEquals(8,got.size());
    }

    @Test
    void negativeCoordinateAndSignedHighestLayerBitAreNotLost() {
        var w=ItemScanWindow.around(-1,-1,0);
        long layers=ItemScanCoverage.mask(-32,-32)|ItemScanCoverage.mask(31,31);
        var c=new ItemScanCoverage(w,Map.of(key(-1,-1),new Column(layers,1)));
        var got=drain(new Plan(null,c,true,List.of(),null,0));
        assertEquals(Set.of(new Pos(-1,-1,-32),new Pos(-1,-1,31)),new HashSet<>(got));
        assertEquals(2,got.size());
        var all=new ItemScanCoverage(w,Map.of(key(-1,-1),new Column(ItemScanCoverage.mask(-32,31),2)));
        var full=drain(new Plan(null,all,true,List.of(),null,0));
        assertEquals(64,full.size());
        for(int z=-32;z<=31;z++)assertTrue(full.contains(new Pos(-1,-1,z)));
    }

    @Test
    void addedLayerAndItsDuplicateDirtyEventAreVisitedOnlyOnce() {
        var w=ItemScanWindow.around(0,0,0);
        var old=new ItemScanCoverage(w,Map.of(key(0,0),new Column(ItemScanCoverage.mask(0,0),1)));
        var now=new ItemScanCoverage(w,Map.of(key(0,0),new Column(
                ItemScanCoverage.mask(0,0)|ItemScanCoverage.mask(4,4),1)));
        var p=new Pos(0,0,4);
        assertEquals(List.of(p),drain(new Plan(old,now,false,List.of(p,p,new Pos(50,50,4)),null,0)));
    }

    @Test
    void horizontalMoveReadsNewEdgesButNoRetainedCenter() {
        var before=ItemScanWindow.around(0,0,1);
        var after=ItemScanWindow.around(1,0,1);
        var columns=Map.of(key(-1,-1),new Column(ItemScanCoverage.mask(4,4),1),
                key(-1,0),new Column(ItemScanCoverage.mask(4,4),1),
                key(0,-1),new Column(ItemScanCoverage.mask(4,4),1),
                key(0,0),new Column(ItemScanCoverage.mask(4,4),1));
        var got=drain(new Plan(new ItemScanCoverage(before,columns),new ItemScanCoverage(after,columns),
                false,List.of(),null,0));
        assertEquals(Set.of(new Pos(2,-1,4),new Pos(2,0,4),new Pos(2,1,4)),new HashSet<>(got));
        assertEquals(3,got.size());
    }

    @Test
    void changedColumnIdentityRescansEvenWhenItsLayerMaskIsUnchanged() {
        var w=ItemScanWindow.around(0,0,0);
        long mask=ItemScanCoverage.mask(4,4);
        var old=new ItemScanCoverage(w,Map.of(key(0,0),new Column(mask,1)));
        var now=new ItemScanCoverage(w,Map.of(key(0,0),new Column(mask,2)));
        assertEquals(List.of(new Pos(0,0,4)),drain(new Plan(old,now,false,List.of(),null,0)));
    }

    @Test
    void patrolCyclesAcrossBasementAndHighestLayerInsteadOfOnlyPlayerNeighbors() {
        var w=ItemScanWindow.around(0,0,0);
        long mask=ItemScanCoverage.mask(-2,-2)|ItemScanCoverage.mask(0,0)|
                ItemScanCoverage.mask(4,4)|ItemScanCoverage.mask(31,31);
        var c=new ItemScanCoverage(w,Map.of(key(0,0),new Column(mask,1)));
        PatrolCursor cursor=null;
        var levels=new ArrayList<Integer>();
        for(int i=0;i<5;i++){
            var p=new Plan(c,c,false,List.of(),cursor,1);
            var got=drain(p);
            assertEquals(1,got.size()); levels.add(got.get(0).z()); cursor=p.patrolCursor();
        }
        assertEquals(List.of(-2,0,4,31,-2),levels);
    }

    @Test
    void emptyCoverageFinishesAndNoIllegalOrOutOfWindowDirtyCellLeaks() {
        var w=ItemScanWindow.around(0,0,0);
        var c=new ItemScanCoverage(w,Map.of(key(0,0),new Column(0,1)));
        assertEquals(List.of(),drain(new Plan(c,c,false,
                List.of(new Pos(0,0,32),new Pos(10,10,0)),null,128)));
        assertThrows(IllegalArgumentException.class,()->ItemScanWindow.around(Double.NaN,0,1));
        assertThrows(IllegalArgumentException.class,()->ItemScanWindow.around(0,0,-1));
    }

    private static long key(int x,int y){return ItemScanCoverage.columnKey(x,y);}
    private static List<Pos> drain(Plan p){
        var out=new ArrayList<Pos>();
        int steps=0;
        while(!p.done()){
            assertTrue(++steps<20000,"candidate plan stalled");
            Pos pos=p.step(); if(pos!=null)out.add(pos);
        }
        return out;
    }
}
