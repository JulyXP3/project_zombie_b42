package modcore;

import modcore.radar.ItemScanBridge;
import modcore.radar.ItemScanCoverage;
import modcore.radar.ItemScanCoverage.Column;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import org.junit.jupiter.api.Test;
import static modcore.ItemScanFakeWorld.*;
import static org.junit.jupiter.api.Assertions.*;

class ItemScanBridgeTest {
    private static KahluaTable table(){return J2SEPlatform.getInstance().newTable();}
    private static KahluaTable targets(String type){var t=table();t.rawset(type,true);return t;}
    private static KahluaTable finish(ItemScanBridge b,double id){
        for(int i=0;i<30000;i++){var frame=b.step(id,3);if(!"running".equals(frame.rawget("state")))return frame;assertNull(frame.rawget("results"));}
        fail("bridge did not converge");return null;
    }
    private static KahluaTable results(KahluaTable frame){assertEquals("ready",frame.rawget("state"),String.valueOf(frame.rawget("error")));return (KahluaTable)frame.rawget("results");}
    @Test void exportsFullXYZAndCountInOneCompleteContinuousLuaTable(){
        var w=new ItemScanFakeWorld();w.columns.put(ItemScanCoverage.columnKey(12,12),new Column(ItemScanCoverage.mask(-2,4),1));
        w.putSquare(100,100,-1).floor.add(floor(100,100,-1,item("Base.Axe")));
        w.putSquare(100,100,2).objects.add(object(100,100,2,item("Base.Axe"),item("Base.Axe")));
        var c=new ItemScanRuntimeTest.Clock();c.step=1_000_000;var b=new ItemScanBridge(w,c,ItemScanBridgeTest::table);
        var out=results(finish(b,b.begin(targets("Base.Axe"),"full",2,null,0)));
        assertEquals(2,out.len());boolean basement=false,upper=false;
        for(int i=1;i<=out.len();i++){
            var p=(KahluaTable)out.rawget(i);assertEquals(100.0,p.rawget("x"));assertEquals(100.0,p.rawget("y"));
            double z=((Number)p.rawget("z")).doubleValue();
            if(z==-1){basement=true;assertEquals(1.0,p.rawget("count"));assertEquals("Axe x1",p.rawget("label"));}
            if(z==2){upper=true;assertEquals(2.0,p.rawget("count"));assertEquals("Axe x2",p.rawget("label"));}
        }
        assertTrue(basement&&upper);
    }
    @Test void targetAndDictionaryDirtySnapshotsDoNotReintroducePlayerFloorFiltering(){
        var w=new ItemScanFakeWorld();w.columns.put(ItemScanCoverage.columnKey(12,12),new Column(ItemScanCoverage.mask(2,2),1));
        var sq=w.putSquare(100,100,2);sq.floor.add(floor(100,100,2,item("Base.Axe")));
        var b=new ItemScanBridge(w,new ItemScanRuntimeTest.Clock(),ItemScanBridgeTest::table);var selected=targets("Base.Axe");
        double id=b.begin(selected,"full",2,null,0);selected.rawset("Base.Axe",false);selected.rawset("Base.Plank",true);
        KahluaTable old=results(finish(b,id));assertEquals(1,old.len());
        sq.floor.clear();var dirty=table();var p=table();p.rawset("x",100.0);p.rawset("y",100.0);p.rawset("z",2.0);dirty.rawset("100,100,2",p);
        assertEquals(0,results(finish(b,b.begin(targets("Base.Axe"),"dirty",2,dirty,0))).len());assertEquals(1,old.len());
    }
    @Test void realLuaExportIsNotCappedAtSixtyAndCancellationKeepsPublishedData(){
        var w=new ItemScanFakeWorld();for(int x=11;x<=13;x++)for(int y=11;y<=13;y++)w.columns.put(ItemScanCoverage.columnKey(x,y),new Column(ItemScanCoverage.mask(0,0),1));
        for(int x=96;x<=104;x++)for(int y=96;y<=104;y++)w.putSquare(x,y,0).floor.add(floor(x,y,0,item("Base.Axe")));
        var c=new ItemScanRuntimeTest.Clock();var b=new ItemScanBridge(w,c,ItemScanBridgeTest::table);
        KahluaTable old=results(finish(b,b.begin(targets("Base.Axe"),"full",5,null,0)));assertEquals(81,old.len());
        c.step=1_000_000;double id=b.begin(targets("Base.Axe"),"full",5,null,0);boolean export=false;
        for(int i=0;i<30000;i++){var frame=b.step(id,3);assertNull(frame.rawget("results"));if("export".equals(frame.rawget("phase"))){export=true;break;}}
        assertTrue(export);b.cancel();assertEquals("cancelled",b.step(id,3).rawget("state"));assertEquals(81,old.len());
    }
    @Test void anErrorFrameIsNotAReadyEmptyResultAndOldTicketsDoNotCancelNewOnes(){
        var w=new ItemScanFakeWorld();var item=item("Base.Axe");w.putSquare(100,100,0).floor.add(floor(100,100,0,item));
        var c=new ItemScanRuntimeTest.Clock();c.step=1_000_000;var b=new ItemScanBridge(w,c,ItemScanBridgeTest::table);
        double old=b.begin(targets("Base.Plank"),"full",2,null,0);double current=b.begin(targets("Base.Axe"),"full",2,null,0);
        assertEquals("cancelled",b.step(old,3).rawget("state"));assertEquals(1,results(finish(b,current)).len());
        item.broken=true;var failed=finish(b,b.begin(targets("Base.Axe"),"full",2,null,0));
        assertEquals("error",failed.rawget("state"));assertNull(failed.rawget("results"));assertNotNull(failed.rawget("error"));
    }
}
