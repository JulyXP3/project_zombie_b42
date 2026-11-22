package modcore;

import modcore.radar.ItemScanColumnStamp;
import modcore.radar.ItemScanColumnStamp.Slot;
import modcore.radar.ItemScanCoverage;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ItemScanColumnStampTest {
    @Test void extendingOneLayerKeepsIdentityRevisionButUnionsMappingCoverage(){
        var s=new ItemScanColumnStamp();Object a=new Object(),b=new Object();
        var before=s.observe(List.of(new Slot(a,10,7,ItemScanCoverage.mask(0,0))));
        var grown=s.observe(List.of(new Slot(a,10,7,ItemScanCoverage.mask(0,0)|ItemScanCoverage.mask(4,4))));
        assertEquals(before.revision(),grown.revision());
        assertEquals(ItemScanCoverage.mask(0,0)|ItemScanCoverage.mask(4,4),grown.mask());
        var union=s.observe(List.of(new Slot(a,10,7,ItemScanCoverage.mask(0,0)),new Slot(b,20,8,ItemScanCoverage.mask(-2,-2))));
        assertEquals(ItemScanCoverage.mask(0,0)|ItemScanCoverage.mask(-2,-2),union.mask());
        assertTrue(union.revision()>grown.revision());
    }
    @Test void pooledIdentityReloadAndWrappedShortIdAreNotPersistentIdentity(){
        var s=new ItemScanColumnStamp();Object a=new Object(),b=new Object();long mask=ItemScanCoverage.mask(31,31);
        var first=s.observe(List.of(new Slot(a,10,32766,mask)));
        var replaced=s.observe(List.of(new Slot(b,10,32766,mask)));
        var reused=s.observe(List.of(new Slot(b,11,32766,mask)));
        var wrapped=s.observe(List.of(new Slot(b,11,0,mask)));
        assertTrue(replaced.revision()>first.revision());assertTrue(reused.revision()>replaced.revision());
        assertTrue(wrapped.revision()>reused.revision());assertEquals(mask,wrapped.mask());
    }
    @Test void unloadAndReloadChangeRevisionAndRetractUnavailableLayers(){
        var s=new ItemScanColumnStamp();Object a=new Object();
        var loaded=s.observe(List.of(new Slot(a,1,2,ItemScanCoverage.mask(-32,-32))));
        var absent=s.observe(List.of(new Slot(null,0,-1,0)));
        var again=s.observe(List.of(new Slot(a,2,3,ItemScanCoverage.mask(-32,-32))));
        assertEquals(0,absent.mask());assertTrue(absent.revision()>loaded.revision());assertTrue(again.revision()>absent.revision());
    }
}
