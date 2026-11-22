package modcore;

import modcore.radar.ItemScanCoverage.Pos;
import modcore.radar.ItemScanIndex.Hit;
import modcore.radar.ItemScanRuntime;
import modcore.radar.ItemScanRuntime.Mode;
import modcore.radar.ItemScanRuntime.State;
import modcore.radar.ItemScanRuntime.Snapshot;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static modcore.ItemScanFakeWorld.*;
import static org.junit.jupiter.api.Assertions.*;

/** Sources with positive contributions get a bounded share of the existing patrol quota. */
class ItemScanHitRefreshTest {
    private static final Set<String> AXE=Set.of("Base.Axe");
    private static ItemScanRuntime runtime(ItemScanFakeWorld w){
        return new ItemScanRuntime(w,new ItemScanRuntimeTest.Clock(),()->new ItemScanRuntime.ResultSink(){
            final List<Hit> rows=new ArrayList<>();
            public void append(Hit hit){rows.add(hit);} public Object seal(){return List.copyOf(rows);}
        });
    }
    private static Snapshot finish(ItemScanRuntime r,long id){
        for(int i=0;i<30000;i++){var s=r.advance(id,3_000_000L);if(s.state()!=State.RUNNING){assertEquals(State.READY,s.state(),s.error());return s;}}
        fail("recheck did not converge");return null;
    }
    @SuppressWarnings("unchecked") private static Map<Pos,Hit> hits(Snapshot s){
        var out=new HashMap<Pos,Hit>();for(Hit h:(List<Hit>)s.payload())out.put(h.position(),h);return out;
    }
    private static long reads(ItemScanFakeWorld w){return w.squareReads.values().stream().mapToLong(Integer::longValue).sum();}
    @Test void aKnownHitIsRecheckedWithoutAChangeEventOrAFullWindowScan(){
        var w=new ItemScanFakeWorld();var sq=w.putSquare(100,100,0);
        sq.floor.add(floor(100,100,0,item("Base.Axe")));var r=runtime(w);
        Snapshot old=finish(r,r.begin(AXE,Mode.FULL,12,List.of(),0));
        sq.floor.clear();w.squareReads.clear();
        assertTrue(hits(finish(r,r.begin(AXE,Mode.PATROL,12,List.of(),8))).isEmpty());
        assertEquals(1,hits(old).size());assertTrue(reads(w)<=8);assertTrue(w.squareReads.containsKey(new Pos(100,100,0)));
    }
    @Test void rechecksUseTheOriginSquareRatherThanTheAggregatedDestination(){
        var w=new ItemScanFakeWorld();var origin=w.putSquare(100,100,0);
        origin.floor.add(floor(101,101,0,item("Base.Axe")));var r=runtime(w);
        var old=hits(finish(r,r.begin(AXE,Mode.FULL,12,List.of(),0)));assertTrue(old.containsKey(new Pos(101,101,0)));
        origin.floor.clear();w.squareReads.clear();
        assertTrue(hits(finish(r,r.begin(AXE,Mode.PATROL,12,List.of(),8))).isEmpty());
        assertTrue(w.squareReads.containsKey(new Pos(100,100,0)));assertFalse(w.squareReads.containsKey(new Pos(101,101,0)));
    }
    @Test void removingOneTypeKeepsTheOtherTypeAtTheSamePosition(){
        var w=new ItemScanFakeWorld();var sq=w.putSquare(100,100,0);
        sq.floor.add(floor(100,100,0,item("Base.Axe")));sq.floor.add(floor(100,100,0,item("Base.Plank")));
        var targets=Set.of("Base.Axe","Base.Plank");var r=runtime(w);
        Snapshot old=finish(r,r.begin(targets,Mode.FULL,12,List.of(),0));
        sq.floor.remove(0);var current=hits(finish(r,r.begin(targets,Mode.PATROL,12,List.of(),8)));
        assertEquals(new Hit(new Pos(100,100,0),"Plank",1),current.get(new Pos(100,100,0)));
        assertEquals(new Hit(new Pos(100,100,0),"Axe",2),hits(old).get(new Pos(100,100,0)));
    }
    @Test void positiveOriginsRotateFairlyWhileEachBatchRetainsItsReadLimit(){
        var w=new ItemScanFakeWorld();
        for(int x=99;x<=102;x++)for(int y=99;y<=102;y++)w.putSquare(x,y,0).floor.add(floor(x,y,0,item("Base.Axe")));
        var r=runtime(w);assertEquals(16,hits(finish(r,r.begin(AXE,Mode.FULL,12,List.of(),0))).size());
        w.squares.values().forEach(s->s.floor.clear());
        Map<Pos,Hit> got=Map.of();
        for(int i=0;i<8;i++){
            w.squareReads.clear();got=hits(finish(r,r.begin(AXE,Mode.PATROL,12,List.of(),8)));
            if(i==0)assertEquals(14,got.size(),"two origins should be verified before the distant broad patrol");
            assertTrue(reads(w)<=8,"verification must share the patrol quota");
        }
        assertTrue(got.isEmpty());
    }
    @Test void repeatedFullWindowsCannotPutOldOriginsAheadOfTheCurrentWindow(){
        var w=new ItemScanFakeWorld();var r=runtime(w);
        for(int i=0;i<40;i++){
            int x=100+i*64;w.player.x=x;w.columns.clear();
            w.columns.put(modcore.radar.ItemScanCoverage.columnKey(Math.floorDiv(x,8),12),
                    new modcore.radar.ItemScanCoverage.Column(modcore.radar.ItemScanCoverage.mask(0,0),i+1));
            w.putSquare(x,100,0).floor.add(floor(x,100,0,item("Base.Axe")));
            finish(r,r.begin(AXE,Mode.FULL,12,List.of(),0));
        }
        int x=(int)w.player.x;w.squares.get(new Pos(x,100,0)).floor.clear();w.squareReads.clear();
        assertTrue(hits(finish(r,r.begin(AXE,Mode.PATROL,12,List.of(),8))).isEmpty(),
                "rechecks must use origins of the committed window rather than all previous full windows");
        assertTrue(w.squareReads.containsKey(new Pos(x,100,0)));assertTrue(reads(w)<=8);
    }
    @Test void anUncommittedFullRefreshDoesNotDiscardTheCommittedRecheckSchedule(){
        var w=new ItemScanFakeWorld();var sq=w.putSquare(100,100,0);sq.floor.add(floor(100,100,0,item("Base.Axe")));
        var clock=new ItemScanRuntimeTest.Clock();var r=new ItemScanRuntime(w,clock,()->new ItemScanRuntime.ResultSink(){
            final List<Hit> rows=new ArrayList<>();public void append(Hit hit){rows.add(hit);}public Object seal(){return List.copyOf(rows);}
        });
        Snapshot old=finish(r,r.begin(AXE,Mode.FULL,12,List.of(),0));
        clock.step=1_000_000;long replaced=r.begin(AXE,Mode.FULL,12,List.of(),0);r.advance(replaced,3_000_000);
        sq.floor.clear();clock.step=0;
        Snapshot now=finish(r,r.begin(AXE,Mode.PATROL,12,List.of(),8));
        assertTrue(hits(now).isEmpty());assertEquals(1,hits(old).size());
        assertEquals(State.CANCELLED,r.advance(replaced,3_000_000).state());
    }
    @Test void reservingFreshnessQuotaStillDiscoversNewHitsInPreviouslyEmptySquares(){
        var w=new ItemScanFakeWorld();w.putSquare(100,100,0).floor.add(floor(100,100,0,item("Base.Axe")));
        var r=runtime(w);finish(r,r.begin(AXE,Mode.FULL,12,List.of(),0));
        w.putSquare(96,96,0).floor.add(floor(96,96,0,item("Base.Axe")));w.squareReads.clear();
        var got=hits(finish(r,r.begin(AXE,Mode.PATROL,12,List.of(),8)));
        assertTrue(got.containsKey(new Pos(96,96,0)));assertTrue(got.containsKey(new Pos(100,100,0)));assertTrue(reads(w)<=8);
    }
}
