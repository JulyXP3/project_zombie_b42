package modcore;

import modcore.radar.ItemScanBridge;
import modcore.radar.ItemScanCoverage;
import modcore.radar.ItemScanCoverage.Column;
import modcore.radar.ItemScanCoverage.Pos;
import modcore.radar.ItemScanIndex.Hit;
import modcore.radar.ItemScanRuntime;
import modcore.radar.ItemScanWorld;
import modcore.radar.ItemScanWindow;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static modcore.ItemScanFakeWorld.*;
import static org.junit.jupiter.api.Assertions.*;

/** Same in-process mock world and scope; no game FPS, native libraries or deployment. */
class ItemScanComparisonTest {
    private static final int RADIUS=8;
    private static final Set<String> TARGETS=Set.of("Base.Axe","Base.Plank");
    private record Run(Map<Pos,Hit> hits,long nanos,long grid,int itemReads,long prepare,long scanMerge,long export,
                       ItemScanBridge bridge,KahluaTable frame) {}
    private static final class Sum {final String name;long count=1;Sum(String name){this.name=name;}}
    @Test void fullAndDirtyResultsMatchAllFloorReferenceWithObservedEndToEndCost() {
        ItemScanFakeWorld w=scene();
        for(int i=0;i<2;i++){reference(w);optimized(w,null,"full",null);}
        long[] naiveNs=new long[5],javaNs=new long[5];Run old=null,now=null;
        for(int i=0;i<5;i++){
            old=reference(w);now=optimized(w,null,"full",null);
            assertEquals(old.hits(),now.hits());naiveNs[i]=old.nanos();javaNs[i]=now.nanos();
        }
        assertNotNull(old);assertNotNull(now);assertEquals(7,now.hits().size());
        assertEquals(new Hit(new Pos(100,100,0),"Axe",800),now.hits().get(new Pos(100,100,0)));
        assertEquals(new Hit(new Pos(100,100,-1),"Axe",3),now.hits().get(new Pos(100,100,-1)));
        assertEquals(new Hit(new Pos(101,101,1),"Axe",3),now.hits().get(new Pos(101,101,1)));
        assertNull(now.hits().get(new Pos(103,101,6)),"vehicle outside the layer window must be excluded");
        // Preset "+-2" keeps z=-2 and z=2; only z=4 lies outside -2..+2 and must be excluded.
        assertEquals(new Hit(new Pos(100,100,-2),"Axe",1),now.hits().get(new Pos(100,100,-2)));
        assertNull(now.hits().get(new Pos(100,100,4)),"layer above window must be excluded");
        assertEquals(old.itemReads(),now.itemReads());assertTrue(now.grid()<old.grid());
        Arrays.sort(naiveNs);Arrays.sort(javaNs);
        print("full",old,now,naiveNs[2],javaNs[2]);
        var inventory=w.squares.get(new Pos(100,100,0)).objects.get(0).containers.get(0).items;
        inventory.clear();inventory.add(item("Base.Axe"));inventory.add(item("Base.Axe"));
        KahluaTable dirty=table(),p=table();p.rawset("x",100.0);p.rawset("y",100.0);p.rawset("z",0.0);dirty.rawset("100:100:0",p);
        Run fullAgain=reference(w),delta=optimized(w,now.bridge(),"dirty",dirty);
        print("dirty",fullAgain,delta,fullAgain.nanos(),delta.nanos());
        assertEquals(fullAgain.hits(),delta.hits());assertEquals(new Hit(new Pos(100,100,0),"Axe",2),delta.hits().get(new Pos(100,100,0)));
        assertTrue(delta.grid()<fullAgain.grid(),"dirty grid "+delta.grid()+" vs full "+fullAgain.grid());
        // Dirty work = the one affected square (2 box items) + the live vehicle source (3 items); nothing else is re-read.
        assertEquals(5L,delta.itemReads(),"dirty item reads");
        assertTrue(delta.itemReads()<fullAgain.itemReads(),"dirty "+delta.itemReads()+" vs full "+fullAgain.itemReads());
        // No speed assertion: this micro-scene builds Lua tables from scratch, so fixed export cost
        // can exceed a tiny reference walk. Only the read work above is claimed.
    }
    private static ItemScanFakeWorld scene(){
        var w=new ItemScanFakeWorld();w.columns.clear();
        // Loaded layers exceed the window on purpose: the reference walker and the Bridge must
        // agree on scanning only playerZ-1..playerZ+2.
        w.columns.put(ItemScanCoverage.columnKey(12,12),new Column(ItemScanCoverage.mask(-3,6),1));
        w.columns.put(ItemScanCoverage.columnKey(13,12),new Column(ItemScanCoverage.mask(-3,6),1));
        var ground=w.putSquare(100,100,0);var box=object(100,100,0);
        for(int i=0;i<400;i++){box.containers.get(0).items.add(item("Base.Axe"));box.containers.get(0).items.add(item("Base.Plank"));}ground.objects.add(box);
        w.putSquare(100,100,-1).floor.add(floor(100,100,-1,bag(item("Base.Axe"),item("Base.Axe"),item("Base.Axe"),bag(item("Base.Axe")))));
        var upper=w.putSquare(100,100,2);for(int i=0;i<20;i++)upper.moving.add(new Node("other",100,100,2));
        upper.moving.add(body(100,100,2,item("Base.Axe"),item("Base.Axe"),item("Base.Axe")));
        w.putSquare(102,102,2).floor.add(floor(102,102,2,item("Base.Plank")));
        w.putSquare(105,100,0).objects.add(object(105,100,0,item("Base.Axe")));
        w.vehicles.add(vehicle(101,101,1,item("Base.Axe"),item("Base.Axe"),item("Base.Axe")));
        // A vehicle well outside the preset window: this used to slip through because the only car
        // in the scene happened to sit inside the window.
        w.vehicles.add(vehicle(103,101,6,item("Base.Axe")));
        // Outside the window: must be excluded by both sides.
        w.putSquare(100,100,-2).floor.add(floor(100,100,-2,item("Base.Axe")));
        w.putSquare(100,100,4).floor.add(floor(100,100,4,item("Base.Axe")));
        w.putSquare(101,101,31).floor.add(floor(101,101,31,item("Base.Axe")));
        return w;
    }
    private static KahluaTable table(){return J2SEPlatform.getInstance().newTable();}
    private static void reset(ItemScanFakeWorld w){w.squareReads.clear();w.typeReads=0;w.bodyChecks=0;}
    private static long reads(ItemScanFakeWorld w){return w.squareReads.values().stream().mapToLong(Integer::longValue).sum();}
    private static Run optimized(ItemScanFakeWorld w,ItemScanBridge reused,String mode,KahluaTable dirty){
        reset(w);long begin=System.nanoTime();
        ItemScanBridge bridge=reused==null?new ItemScanBridge(w,System::nanoTime,ItemScanComparisonTest::table):reused;
        KahluaTable targets=table();for(String type:TARGETS)targets.rawset(type,true);
        double id=bridge.begin(targets,mode,RADIUS,dirty,0,ItemScanRuntime.LAYER_PM2);KahluaTable frame=null;
        for(int i=0;i<30000;i++){frame=bridge.step(id,3);if(!"running".equals(frame.rawget("state")))break;}
        assertNotNull(frame);assertEquals("ready",frame.rawget("state"),String.valueOf(frame.rawget("error")));
        Map<Pos,Hit> hits=decode((KahluaTable)frame.rawget("results"));long end=System.nanoTime();
        KahluaTable stats=(KahluaTable)frame.rawget("stats"),phases=(KahluaTable)stats.rawget("phaseMs");
        return new Run(hits,end-begin,reads(w),w.typeReads,nanos(stats,"prepareMs"),nanos(phases,"squares")+nanos(phases,"vehicles")+nanos(phases,"delta"),nanos(phases,"export"),bridge,frame);
    }
    private static Run reference(ItemScanFakeWorld w){
        reset(w);long begin=System.nanoTime();Set<String> targets=Set.copyOf(TARGETS);
        ItemScanWindow window=ItemScanWindow.around(w.x(w.player()),w.y(w.player()),RADIUS);
        Map<Pos,Sum> sums=new LinkedHashMap<>();long prepared=System.nanoTime();
        // Independent plain z/x/y walk over the SAME player-relative layer window the product uses.
        int pz=(int)Math.floor(w.z(w.player()));
        int zLo=Math.max(-32,pz-2),zHi=Math.min(31,pz+2);
        for(int z=zLo;z<=zHi;z++)for(int x=window.x1();x<=window.x2();x++)for(int y=window.y1();y<=window.y2();y++){
            Object square=w.square(x,y,z);if(square==null)continue;
            for(Object object:w.objects(square))for(int i=0;i<w.containerCount(object);i++)scan(w,w.container(object,i),position(w,object),targets,sums);
            for(Object object:w.floor(square)){
                Object item=w.floorItem(object);Pos p=position(w,object);add(w,item,p,targets,sums);
                if(item!=null&&w.isBag(item))scan(w,w.bag(item),p,targets,sums);
            }
            for(Object object:w.moving(square))if(w.isCorpse(object))scan(w,w.corpseContainer(object),position(w,object),targets,sums);
        }
        for(Object vehicle:w.vehicles()){
            Pos p=position(w,vehicle);if(p==null||!window.contains(p.x(),p.y())||p.z()<zLo||p.z()>zHi)continue;
            for(int i=0;i<w.partCount(vehicle);i++){Object part=w.part(vehicle,i);if(part!=null)scan(w,w.partContainer(part),position(w,part),targets,sums);}
        }
        long scanned=System.nanoTime();KahluaTable result=table();int at=0;
        for(var e:sums.entrySet()){
            Pos p=e.getKey();if(p==null||!window.contains(p.x(),p.y()))continue;
            var row=table();row.rawset("x",(double)p.x());row.rawset("y",(double)p.y());row.rawset("z",(double)p.z());
            row.rawset("name",e.getValue().name);row.rawset("count",(double)e.getValue().count);row.rawset("label",e.getValue().name+" x"+e.getValue().count);result.rawset(++at,row);
        }
        Map<Pos,Hit> hits=decode(result);long end=System.nanoTime();
        return new Run(hits,end-begin,reads(w),w.typeReads,prepared-begin,scanned-prepared,end-scanned,null,null);
    }
    private static void scan(ItemScanWorld w,Object container,Pos p,Set<String> targets,Map<Pos,Sum> sums){
        if(container==null||p==null)return;for(Object item:w.items(container))add(w,item,p,targets,sums);
    }
    private static void add(ItemScanWorld w,Object item,Pos p,Set<String> targets,Map<Pos,Sum> sums){
        if(item==null||p==null)return;if(!targets.contains(w.fullType(item))&&!targets.contains(w.shortType(item)))return;
        Sum old=sums.get(p);if(old==null)sums.put(p,new Sum(w.name(item)));else old.count++;
    }
    private static Pos position(ItemScanWorld w,Object o){
        double x=Math.floor(w.x(o)),y=Math.floor(w.y(o)),z=Math.floor(w.z(o));
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||z<-32||z>31)return null;
        return new Pos((int)x,(int)y,(int)z);
    }
    private static Map<Pos,Hit> decode(KahluaTable results){
        assertNotNull(results);var out=new HashMap<Pos,Hit>();
        for(int i=1;i<=results.len();i++){var p=(KahluaTable)results.rawget(i);var pos=new Pos(((Number)p.rawget("x")).intValue(),((Number)p.rawget("y")).intValue(),((Number)p.rawget("z")).intValue());out.put(pos,new Hit(pos,(String)p.rawget("name"),((Number)p.rawget("count")).longValue()));}
        return Map.copyOf(out);
    }
    private static long nanos(KahluaTable table,String key){Object value=table.rawget(key);return value instanceof Number n?(long)(n.doubleValue()*1_000_000):0;}
    private static void print(String mode,Run naive,Run java,long naiveMedian,long javaMedian){
        System.out.println("RADAR_COMPARE|mode="+mode+"|radius="+RADIUS+"|z=-32..31|hits="+java.hits().size()+"|naive_grid="+naive.grid()+"|java_grid="+java.grid()+"|naive_items="+naive.itemReads()+"|java_items="+java.itemReads()+"|naive_ns="+naiveMedian+"|java_ns="+javaMedian+"|naive_prepare_ns="+naive.prepare()+"|java_prepare_ns="+java.prepare()+"|naive_scan_merge_ns="+naive.scanMerge()+"|java_scan_merge_ns="+java.scanMerge()+"|naive_export_ns="+naive.export()+"|java_export_ns="+java.export());
    }
}
