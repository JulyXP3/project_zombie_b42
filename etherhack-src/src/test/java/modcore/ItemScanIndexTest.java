package modcore;

import modcore.radar.ItemScanCoverage.Pos;
import modcore.radar.ItemScanIndex;
import modcore.radar.ItemScanIndex.Hit;
import modcore.radar.ItemScanIndex.SourceKey;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ItemScanIndexTest {
    @Test
    void differentFloorsAtTheSameXYKeepIndependentCounts() {
        var ground=new Pos(4,4,0); var basement=new Pos(4,4,-2);
        var a=SourceKey.grid(new Pos(0,0,0)); var b=SourceKey.grid(new Pos(1,1,0));
        var state=replace(new ItemScanIndex(),a,Map.of(ground,new Hit(ground,"Axe",2),
                basement,new Hit(basement,"Nails",5)),0);
        state=replace(state,b,Map.of(ground,new Hit(ground,"Plank",1)),1);
        assertEquals(new Hit(ground,"Axe",3),hits(state).get(ground));
        assertEquals(new Hit(basement,"Nails",5),hits(state).get(basement));
        assertEquals(2,hits(state).size());
    }

    @Test
    void deltaReplacementDoesNotAccumulateAndRemovedFirstNameIsRechosen() {
        var p=new Pos(1,2,4);
        var a=SourceKey.grid(new Pos(0,0,0)); var b=SourceKey.grid(new Pos(2,0,4));
        var v=SourceKey.vehicles();
        var state=replace(new ItemScanIndex(),a,Map.of(p,new Hit(p,"Axe",2)),0);
        state=replace(state,b,Map.of(p,new Hit(p,"Plank",1)),1);
        state=replace(state,v,Map.of(p,new Hit(p,"Nails",1)),0);
        var old=state;
        assertEquals(new Hit(p,"Axe",4),hits(old).get(p));
        state=replace(state,a,Map.of(p,new Hit(p,"Axe",3)),10);
        state=replace(state,a,Map.of(p,new Hit(p,"Axe",3)),11);
        assertEquals(new Hit(p,"Plank",5),hits(state).get(p));
        state=replace(state,a,Map.of(),12);
        assertEquals(new Hit(p,"Plank",2),hits(state).get(p));
        state=replace(state,b,Map.of(),13);
        assertEquals(new Hit(p,"Nails",1),hits(state).get(p));
        assertEquals(new Hit(p,"Axe",4),hits(old).get(p));
    }

    @Test
    void partiallyAdvancedCOWCannotPublishAndDoesNotMutateThePreviousSnapshot() {
        var p=new Pos(-1,-1,31); var a=SourceKey.grid(p);
        var old=replace(new ItemScanIndex(),a,Map.of(p,new Hit(p,"Axe",2)),0);
        var work=old.working(); drain(work);
        work.startReplace(a,Map.of(p,new Hit(p,"Plank",3)),1);
        for(int i=0;i<5;i++)work.advance();
        assertFalse(work.isIdle());
        assertThrows(IllegalStateException.class,work::snapshot);
        assertEquals(new Hit(p,"Axe",2),hits(old).get(p));
        drain(work); var now=work.snapshot();
        assertEquals(new Hit(p,"Plank",3),hits(now).get(p));
        assertEquals(new Hit(p,"Axe",2),hits(old).get(p));
        assertThrows(IllegalStateException.class,()->work.startReplace(a,Map.of(),2));
    }

    @Test
    void emptySourceReplacementRetractsAllHitsIncludingOtherDestinationLayers() {
        var a=SourceKey.grid(new Pos(8,-1,31));
        var p=new Pos(8,-1,31); var q=new Pos(8,-1,-32);
        var old=replace(new ItemScanIndex(),a,Map.of(p,new Hit(p,"Axe",7),q,new Hit(q,"Nails",2)),0);
        var now=replace(old,a,Map.of(),1);
        assertTrue(hits(now).isEmpty());
        assertEquals(2,hits(old).size());
    }

    private static ItemScanIndex replace(ItemScanIndex old,SourceKey source,Map<Pos,Hit> next,long order){
        var work=old.working(); drain(work); work.startReplace(source,next,order); drain(work); return work.snapshot();
    }
    private static void drain(ItemScanIndex.Working w){
        int steps=0;
        while(!w.isIdle()){assertTrue(++steps<20000,"index mutation stalled");w.advance();}
    }
    private static Map<Pos,Hit> hits(ItemScanIndex index){
        var out=new HashMap<Pos,Hit>();for(Hit h:index.hits())out.put(h.position(),h);return out;
    }
}
