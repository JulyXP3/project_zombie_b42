package modcore;

import modcore.radar.ItemScanCoverage;
import modcore.radar.ItemScanCoverage.Column;
import modcore.radar.ItemScanCoverage.Pos;
import modcore.radar.ItemScanIndex.Hit;
import modcore.radar.ItemScanRuntime;
import modcore.radar.ItemScanRuntime.Mode;
import modcore.radar.ItemScanRuntime.Phase;
import modcore.radar.ItemScanRuntime.Snapshot;
import modcore.radar.ItemScanRuntime.State;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import static modcore.ItemScanFakeWorld.*;
import static org.junit.jupiter.api.Assertions.*;

class ItemScanRuntimeTest {
    private static final Set<String> AXE=Set.of("Base.Axe");
    private static final long BUDGET=3_000_000L;
    static final class Clock implements LongSupplier {long now,step;public long getAsLong(){long old=now;now+=step;return old;}}
    static final class Sink implements ItemScanRuntime.ResultSink {
        final List<Hit> rows=new ArrayList<>();int appends;
        public void append(Hit h){rows.add(h);appends++;} public Object seal(){return Collections.unmodifiableList(rows);}
    }
    private static ItemScanRuntime runtime(ItemScanFakeWorld w,Clock c,List<Sink> sinks){
        return new ItemScanRuntime(w,c,()->{var sink=new Sink();sinks.add(sink);return sink;});
    }
    private static Snapshot finish(ItemScanRuntime r,long id){
        for(int i=0;i<30000;i++){Snapshot s=r.advance(id,BUDGET);if(s.state()!=State.RUNNING)return s;}
        fail("runtime did not converge");return null;
    }
    @SuppressWarnings("unchecked") private static Map<Pos,Hit> hits(Snapshot s){
        assertEquals(State.READY,s.state(),s.error());assertNotNull(s.payload());
        var out=new HashMap<Pos,Hit>();for(var h:(List<Hit>)s.payload())out.put(h.position(),h);return out;
    }
    @Test void everyOriginalSourceIsScannedInsideThePlayerLayerWindow(){
        var w=new ItemScanFakeWorld();var c=new Clock();
        // Loaded column spans far more floors than the window; only z-1..z+2 may be read.
        // Each source sits on its own square so a wrong source shows up as a wrong count, not a sum.
        w.columns.put(ItemScanCoverage.columnKey(12,12),new Column(ItemScanCoverage.mask(-2,5)|ItemScanCoverage.mask(31,31),1));
        w.putSquare(100,100,0).objects.add(object(100,100,0,item("Base.Axe")));
        w.putSquare(100,100,2).floor.add(floor(100,100,2,bag(item("Base.Axe"),item("Base.Axe"),bag(item("Base.Axe")))));
        w.putSquare(101,101,2).moving.add(new Node("other",101,101,2));
        w.putSquare(101,101,2).moving.add(body(101,101,2,item("Base.Axe")));
        w.putSquare(100,100,-1).objects.add(object(100,100,-1,item("Base.Axe"),item("Base.Axe"),item("Base.Axe")));
        w.putSquare(99,99,-1).floor.add(floor(99,99,-1,item("Base.Axe")));
        w.vehicles.add(vehicle(99,100,1,item("Base.Axe")));
        // Outside the window: must never appear even though the squares exist.
        w.putSquare(100,100,-2).floor.add(floor(100,100,-2,item("Base.Axe")));
        w.putSquare(100,100,4).floor.add(floor(100,100,4,item("Base.Axe")));
        w.putSquare(101,101,31).floor.add(floor(101,101,31,item("Base.Axe")));
        var r=runtime(w,c,new ArrayList<>());var got=hits(finish(r,r.begin(AXE,Mode.FULL,2,List.of(),0,ItemScanRuntime.LAYER_PM2)));
        assertEquals(Map.of(new Pos(100,100,0),new Hit(new Pos(100,100,0),"Axe",1),
                new Pos(100,100,2),new Hit(new Pos(100,100,2),"Axe",2),
                new Pos(101,101,2),new Hit(new Pos(101,101,2),"Axe",1),
                new Pos(100,100,-1),new Hit(new Pos(100,100,-1),"Axe",3),
                new Pos(99,99,-1),new Hit(new Pos(99,99,-1),"Axe",1),
                new Pos(100,100,-2),new Hit(new Pos(100,100,-2),"Axe",1),
                new Pos(99,100,1),new Hit(new Pos(99,100,1),"Axe",1)),got);
        assertFalse(w.squareReads.containsKey(new Pos(100,100,4)));
        assertFalse(w.squareReads.containsKey(new Pos(101,101,31)));
    }
    @Test void targetSnapshotIsCopiedAndShortNamesStillMatch(){
        var w=new ItemScanFakeWorld();w.putSquare(100,100,0).floor.add(floor(100,100,0,item("Extra.Axe")));
        var targets=new HashSet<>(Set.of("Axe"));var r=runtime(w,new Clock(),new ArrayList<>());
        long id=r.begin(targets,Mode.FULL,2,List.of(),0);targets.clear();targets.add("Plank");
        assertEquals(new Hit(new Pos(100,100,0),"Axe",1),hits(finish(r,id)).get(new Pos(100,100,0)));
    }
    @Test void dirtyUpdateAndPlayerFloorChangeKeepWindowLayerAndHorizontalAnchor(){
        var w=new ItemScanFakeWorld();var c=new Clock();
        w.columns.put(ItemScanCoverage.columnKey(12,12),new Column(ItemScanCoverage.mask(0,0)|ItemScanCoverage.mask(2,2),1));
        var ground=w.putSquare(100,100,0);ground.objects.add(object(100,100,0,item("Base.Axe")));
        w.putSquare(100,100,2).objects.add(object(100,100,2,item("Base.Axe"),item("Base.Axe")));
        var r=runtime(w,c,new ArrayList<>());finish(r,r.begin(AXE,Mode.FULL,2,List.of(),0));
        ground.objects.clear();w.player.x=101;w.player.z=3;w.squareReads.clear();
        Snapshot s=finish(r,r.begin(AXE,Mode.DIRTY,2,List.of(new Pos(100,100,0)),0));
        assertEquals(Map.of(new Pos(100,100,2),new Hit(new Pos(100,100,2),"Axe",2)),hits(s));
        assertEquals(100,s.window().cx());assertFalse(w.squareReads.containsKey(new Pos(100,100,2)));
    }
    @Test void loadedLayerAdditionRemovalAndColumnReplacementConverge(){
        var w=new ItemScanFakeWorld();var r=runtime(w,new Clock(),new ArrayList<>());
        var ground=w.putSquare(100,100,0);ground.floor.add(floor(100,100,0,item("Base.Axe")));
        finish(r,r.begin(AXE,Mode.FULL,2,List.of(),0));
        w.columns.put(ItemScanCoverage.columnKey(12,12),new Column(ItemScanCoverage.mask(0,0)|ItemScanCoverage.mask(2,2),1));
        w.putSquare(100,100,2).floor.add(floor(100,100,2,item("Base.Axe")));w.squareReads.clear();
        Snapshot added=finish(r,r.begin(AXE,Mode.PATROL,2,List.of(),0));assertEquals(2,hits(added).size());
        assertFalse(w.squareReads.containsKey(new Pos(100,100,0)));
        w.columns.put(ItemScanCoverage.columnKey(12,12),new Column(ItemScanCoverage.mask(0,0),1));w.squareReads.clear();
        assertEquals(1,hits(finish(r,r.begin(AXE,Mode.PATROL,2,List.of(),0))).size());
        assertTrue(w.squareReads.isEmpty());assertEquals(2,hits(added).size());
        ground.floor.clear();w.columns.put(ItemScanCoverage.columnKey(12,12),new Column(ItemScanCoverage.mask(0,0),2));
        assertTrue(hits(finish(r,r.begin(AXE,Mode.PATROL,2,List.of(),0))).isEmpty());
    }
    @Test void movementRetainsCenterRetractsOldEdgeAndReadsOnlyNewEdges(){
        var w=new ItemScanFakeWorld();w.columns.put(ItemScanCoverage.columnKey(13,12),new Column(ItemScanCoverage.mask(0,0),1));
        for(int x:new int[]{98,100,104})w.putSquare(x,100,0).floor.add(floor(x,100,0,item("Base.Axe")));
        var r=runtime(w,new Clock(),new ArrayList<>());Snapshot old=finish(r,r.begin(AXE,Mode.FULL,2,List.of(),0));
        w.player.x=102;w.squareReads.clear();var got=hits(finish(r,r.begin(AXE,Mode.MOVE,2,List.of(),0)));
        assertEquals(Set.of(new Pos(100,100,0),new Pos(104,100,0)),got.keySet());
        assertFalse(w.squareReads.containsKey(new Pos(100,100,0)));assertFalse(w.squareReads.containsKey(new Pos(98,100,0)));
        assertEquals(2,hits(old).size());
    }
    @Test void denseItemsRawCorpseFilteringAndDeltaYieldWithinTheBudget(){
        var w=new ItemScanFakeWorld();var sq=w.putSquare(100,100,0);var box=object(100,100,0);
        for(int i=0;i<400;i++)box.containers.get(0).items.add(item("Base.Axe"));sq.objects.add(box);
        for(int i=0;i<500;i++)sq.moving.add(new Node("other",100,100,0));sq.moving.add(body(100,100,0,item("Base.Axe")));
        var c=new Clock();c.step=1_000_000;var r=runtime(w,c,new ArrayList<>());
        long id=r.begin(AXE,Mode.FULL,2,List.of(),0);var phases=new HashSet<Phase>();Snapshot finalState=null;
        for(int i=0;i<30000;i++){
            int before=w.typeReads,bodies=w.bodyChecks;Snapshot s=r.advance(id,BUDGET);phases.add(s.phase());
            assertTrue(w.typeReads-before<=3);assertTrue(w.bodyChecks-bodies<=3);
            if(s.state()!=State.RUNNING){finalState=s;break;}
        }
        assertNotNull(finalState);assertEquals(new Hit(new Pos(100,100,0),"Axe",401),hits(finalState).get(new Pos(100,100,0)));
        assertTrue(phases.contains(Phase.DELTA));
    }
    @Test void overSixtyHitsExportInSlicesAndCancelDoesNotMutateOldPayload(){
        var w=new ItemScanFakeWorld();
        for(int x=11;x<=13;x++)for(int y=11;y<=13;y++)w.columns.put(ItemScanCoverage.columnKey(x,y),new Column(ItemScanCoverage.mask(0,0),1));
        for(int x=96;x<=104;x++)for(int y=96;y<=104;y++)w.putSquare(x,y,0).floor.add(floor(x,y,0,item("Base.Axe")));
        var c=new Clock();var sinks=new ArrayList<Sink>();var r=runtime(w,c,sinks);
        Snapshot old=finish(r,r.begin(AXE,Mode.FULL,5,List.of(),0));assertEquals(81,hits(old).size());
        c.step=1_000_000;long id=r.begin(AXE,Mode.FULL,5,List.of(),0);boolean exporting=false;
        for(int i=0;i<30000;i++){
            Sink sink=sinks.get(sinks.size()-1);int before=sink.appends;Snapshot s=r.advance(id,BUDGET);
            assertTrue(sink.appends-before<=3);assertNull(s.payload());
            if(s.phase()==Phase.EXPORT&&s.state()==State.RUNNING){exporting=true;break;}
        }
        assertTrue(exporting);r.cancel();assertEquals(State.CANCELLED,r.advance(id,BUDGET).state());
        assertEquals(81,hits(old).size());c.step=0;
        long next=r.begin(AXE,Mode.FULL,5,List.of(),0);assertEquals(81,hits(finish(r,next)).size());
        assertNull(r.advance(next,BUDGET).payload(),"ready payload must be transferred once");
    }
    @Test void targetAndWorldChangesFenceOldTickets(){
        var w=new ItemScanFakeWorld();w.putSquare(100,100,0).objects.add(object(100,100,0,item("Base.Axe"),item("Base.Plank")));
        var c=new Clock();c.step=1_000_000;var r=runtime(w,c,new ArrayList<>());
        long old=r.begin(AXE,Mode.FULL,2,List.of(),0);r.advance(old,BUDGET);
        long next=r.begin(Set.of("Base.Plank"),Mode.FULL,2,List.of(),0);
        assertEquals(State.CANCELLED,r.advance(old,BUDGET).state());
        assertEquals(new Hit(new Pos(100,100,0),"Plank",1),hits(finish(r,next)).get(new Pos(100,100,0)));
        long playerId=r.begin(AXE,Mode.FULL,2,List.of(),0);w.player=new Node("player",100,100,0);
        assertEquals(State.CANCELLED,r.advance(playerId,BUDGET).state());
        long cellId=r.begin(AXE,Mode.FULL,2,List.of(),0);w.cell=new Object();
        assertEquals(State.CANCELLED,r.advance(cellId,BUDGET).state());
    }
    @Test void getterFailureIsTerminalUntilAnExplicitNewRequest(){
        var w=new ItemScanFakeWorld();var item=item("Base.Axe");w.putSquare(100,100,0).floor.add(floor(100,100,0,item));
        var r=runtime(w,new Clock(),new ArrayList<>());Snapshot old=finish(r,r.begin(AXE,Mode.FULL,2,List.of(),0));
        item.broken=true;long id=r.begin(AXE,Mode.DIRTY,2,List.of(new Pos(100,100,0)),0);Snapshot failure=finish(r,id);
        assertEquals(State.ERROR,failure.state());assertNull(failure.payload());int reads=w.typeReads;
        for(int i=0;i<5;i++)assertEquals(State.ERROR,r.advance(id,BUDGET).state());
        assertEquals(reads,w.typeReads);assertEquals(1,hits(old).size());item.broken=false;
        assertEquals(1,hits(finish(r,r.begin(AXE,Mode.FULL,2,List.of(),0))).size());
    }
    @Test void timeoutDoesNotDeliverPartialResults(){
        var w=new ItemScanFakeWorld();w.putSquare(100,100,0).floor.add(floor(100,100,0,item("Base.Axe")));
        var c=new Clock();c.step=1_000_000;var r=runtime(w,c,new ArrayList<>());long id=r.begin(AXE,Mode.FULL,2,List.of(),0);
        assertEquals(State.RUNNING,r.advance(id,BUDGET).state());c.now+=120_000_000_001L;
        Snapshot timeout=r.advance(id,BUDGET);assertEquals(State.TIMEOUT,timeout.state());assertNull(timeout.payload());
    }
    @Test void bagsOnlyExpandOneGroundLevel(){
        var w=new ItemScanFakeWorld();var sq=w.putSquare(100,100,0);
        sq.objects.add(object(100,100,0,bag(item("Base.Axe"))));
        sq.floor.add(floor(100,100,0,bag(item("Base.Axe"),bag(item("Base.Axe")))));
        sq.moving.add(body(100,100,0,bag(item("Base.Axe"))));w.vehicles.add(vehicle(100,100,0,bag(item("Base.Axe"))));
        var r=runtime(w,new Clock(),new ArrayList<>());
        assertEquals(new Hit(new Pos(100,100,0),"Axe",1),hits(finish(r,r.begin(AXE,Mode.FULL,2,List.of(),0))).get(new Pos(100,100,0)));
    }
    @Test void loadedVehicleNeedsNoStaticSquareAndKeepsItsValidHighLayer(){
        var w=new ItemScanFakeWorld();w.columns.clear();
        w.vehicles.add(vehicle(101.5,101.5,31.75,item("Base.Axe")));
        w.vehicles.add(vehicle(110,110,31,item("Base.Axe")));
        w.vehicles.add(vehicle(100,100,Double.NaN,item("Base.Axe")));
        var r=runtime(w,new Clock(),new ArrayList<>());
        assertEquals(Map.of(new Pos(101,101,31),new Hit(new Pos(101,101,31),"Axe",1)),hits(finish(r,r.begin(AXE,Mode.FULL,2,List.of(),0))));
        assertTrue(w.squareReads.isEmpty());
    }
}
