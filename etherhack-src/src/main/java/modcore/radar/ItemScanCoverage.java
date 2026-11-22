package modcore.radar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable loaded-column coverage. No player-relative floor restriction is stored. */
public final class ItemScanCoverage {
    public record Pos(int x,int y,int z) {}
    public record Column(long mask,long revision) {}
    public record PatrolCursor(Pos last) {}
    record ColumnRef(long key,int cx,int cy,Column column,int x1,int x2,int y1,int y2,int area) {}
    private final ItemScanWindow window;
    private final Map<Long,Column> columns;
    private final List<ColumnRef> ordered;
    private final long totalCells;

    public ItemScanCoverage(ItemScanWindow window,Map<Long,Column> columns){
        this.window=Objects.requireNonNull(window);
        this.columns=Collections.unmodifiableMap(new LinkedHashMap<>(columns));
        var refs=new ArrayList<ColumnRef>(); long total=0;
        for(var entry:this.columns.entrySet()){
            var ref=columnRef(window,entry.getKey(),entry.getValue());
            if(ref.area()>0){refs.add(ref);total+=ref.area()*(long)Long.bitCount(ref.column().mask());}
        }
        refs.sort(Comparator.comparingInt(ColumnRef::cx).thenComparingInt(ColumnRef::cy));
        ordered=Collections.unmodifiableList(refs); totalCells=total;
    }
    private ItemScanCoverage(ItemScanWindow window,Map<Long,Column> columns,List<ColumnRef> ordered,long total){
        this.window=window;this.columns=Collections.unmodifiableMap(columns);
        this.ordered=Collections.unmodifiableList(ordered);totalCells=total;
    }
    // Builder transfers ownership after its per-column budgeted capture; no late bulk copy.
    static ItemScanCoverage owned(ItemScanWindow w,Map<Long,Column> cols,List<ColumnRef> refs,long total){
        return new ItemScanCoverage(w,cols,refs,total);
    }
    static ColumnRef columnRef(ItemScanWindow w,long key,Column column){
        int cx=(int)(key>>32),cy=(int)key;
        long bx=(long)cx*8,by=(long)cy*8;
        long x1=Math.max(w.x1(),bx),x2=Math.min(w.x2(),bx+7);
        long y1=Math.max(w.y1(),by),y2=Math.min(w.y2(),by+7);
        int area=x1>x2||y1>y2?0:(int)((x2-x1+1)*(y2-y1+1));
        return new ColumnRef(key,cx,cy,column,(int)x1,(int)x2,(int)y1,(int)y2,area);
    }
    public ItemScanWindow window(){return window;}
    public Map<Long,Column> columns(){return columns;}
    public long totalCells(){return totalCells;}
    public static long columnKey(int x,int y){return ((long)x<<32)|(y&0xffffffffL);}
    public static boolean validZ(int z){return z>=-32&&z<=31;}
    /** Player-relative layer window: below floors under the player plus above floors over it, clamped. */
    public static long relativeLayers(int playerZ,int below,int above){
        return mask(playerZ-below,playerZ+above);
    }
    public static long mask(int min,int max){
        int low=Math.max(-32,min),high=Math.min(31,max);
        if(low>high)return 0;
        return (-1L<<(low+32))&(-1L>>>(31-high));
    }
    public boolean contains(Pos p){
        if(p==null||!validZ(p.z())||!window.contains(p.x(),p.y()))return false;
        var c=columns.get(columnKey(Math.floorDiv(p.x(),8),Math.floorDiv(p.y(),8)));
        return c!=null&&(c.mask()&(1L<<(p.z()+32)))!=0;
    }

    /** One step advances one dirty/column/layer/cell unit, even when it yields no candidate. */
    public static final class Plan {
        private final ItemScanCoverage previous,current;
        private final boolean full;
        private final Iterator<Pos> dirty;
        private final Set<Pos> dirtySeen=new HashSet<>();
        private final PatrolCursor originalCursor;
        private long patrolLeft;
        private int stage,scanIndex,scanZ,scanCell,locateIndex,patrolIndex,patrolZ,patrolCell;
        private ColumnRef scanRef,patrolRef;
        private long scanLayers,patrolLayers;
        private boolean scanLevel,patrolLevel;
        private Pos lastPatrol;
        public Plan(ItemScanCoverage previous,ItemScanCoverage current,boolean full,Iterable<Pos> dirty,
                    PatrolCursor cursor,int patrolCells){
            this.previous=previous;this.current=Objects.requireNonNull(current);this.full=full||previous==null;
            this.dirty=(dirty==null?List.<Pos>of():dirty).iterator();originalCursor=cursor;
            lastPatrol=cursor==null?null:cursor.last();
            patrolLeft=Math.min(Math.max(0,patrolCells),current.totalCells);
        }
        public boolean done(){return stage==4;}
        public PatrolCursor patrolCursor(){return lastPatrol==null?originalCursor:new PatrolCursor(lastPatrol);}
        public Pos step(){
            if(stage==0){
                if(dirty.hasNext()){
                    Pos p=dirty.next();return current.contains(p)&&dirtySeen.add(p)?p:null;
                }
                stage=1;return null;
            }
            if(stage==1)return stepChanges();
            if(stage==2){setupPatrol();return null;}
            if(stage==3)return stepPatrol();
            return null;
        }
        private long changeLayers(ColumnRef ref){
            long mask=ref.column().mask();
            if(full)return mask;
            Column old=previous.columns.get(ref.key());
            if(old==null||old.revision()!=ref.column().revision())return mask;
            long added=mask&~old.mask();
            if(previous.window.equals(current.window)||
                    previous.window.contains(ref.x1(),ref.y1())&&previous.window.contains(ref.x2(),ref.y2()))return added;
            return mask;
        }
        private boolean isChange(Pos p){
            if(full)return true;
            long key=columnKey(Math.floorDiv(p.x(),8),Math.floorDiv(p.y(),8));
            Column old=previous.columns.get(key),now=current.columns.get(key);
            return old==null||now==null||old.revision()!=now.revision()||
                    !previous.window.contains(p.x(),p.y())||(old.mask()&(1L<<(p.z()+32)))==0;
        }
        private Pos stepChanges(){
            if(scanRef==null){
                if(scanIndex>=current.ordered.size()){stage=2;return null;}
                scanRef=current.ordered.get(scanIndex++);scanLayers=changeLayers(scanRef);scanLevel=false;return null;
            }
            if(!scanLevel){
                if(scanLayers==0){scanRef=null;return null;}
                scanZ=Long.numberOfTrailingZeros(scanLayers)-32;scanLayers&=scanLayers-1;
                scanCell=0;scanLevel=true;return null;
            }
            if(scanCell>=scanRef.area()){scanLevel=false;return null;}
            int height=scanRef.y2()-scanRef.y1()+1;
            Pos p=new Pos(scanRef.x1()+scanCell/height,scanRef.y1()+scanCell%height,scanZ);scanCell++;
            return isChange(p)&&!dirtySeen.contains(p)?p:null;
        }
        private void setupPatrol(){
            if(patrolLeft==0||current.ordered.isEmpty()){stage=4;return;}
            Pos anchor=originalCursor==null?null:originalCursor.last();
            if(anchor==null){patrolIndex=0;stage=3;return;}
            int cx=Math.floorDiv(anchor.x(),8),cy=Math.floorDiv(anchor.y(),8);
            if(locateIndex>=current.ordered.size()){patrolIndex=0;stage=3;return;}
            ColumnRef ref=current.ordered.get(locateIndex);
            int cmp=Integer.compare(ref.cx(),cx);if(cmp==0)cmp=Integer.compare(ref.cy(),cy);
            if(cmp<0){locateIndex++;return;}
            if(cmp>0){patrolIndex=locateIndex;stage=3;return;}
            patrolRef=ref;patrolIndex=locateIndex+1;
            long available=anchor.z()>31?0:anchor.z()<-32?ref.column().mask():
                    ref.column().mask()&(-1L<<(anchor.z()+32));
            if(available!=0){
                patrolZ=Long.numberOfTrailingZeros(available)-32;patrolLayers=available&(available-1);
                patrolCell=patrolZ==anchor.z()?Math.floorMod(anchor.x(),8)*8+Math.floorMod(anchor.y(),8)+1:0;
                patrolLevel=true;
            }else{patrolLayers=0;patrolLevel=false;}
            stage=3;
        }
        private Pos stepPatrol(){
            if(patrolLeft==0){stage=4;return null;}
            if(patrolRef==null){
                if(patrolIndex>=current.ordered.size())patrolIndex=0;
                patrolRef=current.ordered.get(patrolIndex++);patrolLayers=patrolRef.column().mask();
                patrolLevel=false;return null;
            }
            if(!patrolLevel){
                if(patrolLayers==0){patrolRef=null;return null;}
                patrolZ=Long.numberOfTrailingZeros(patrolLayers)-32;patrolLayers&=patrolLayers-1;
                patrolCell=0;patrolLevel=true;return null;
            }
            if(patrolCell>=64){patrolLevel=false;return null;}
            int x=patrolRef.cx()*8+patrolCell/8,y=patrolRef.cy()*8+patrolCell%8;patrolCell++;
            if(!current.window.contains(x,y))return null;
            Pos p=new Pos(x,y,patrolZ);lastPatrol=p;patrolLeft--;
            if(patrolLeft==0)stage=4;
            return !dirtySeen.contains(p)&&!isChange(p)?p:null;
        }
    }
}
