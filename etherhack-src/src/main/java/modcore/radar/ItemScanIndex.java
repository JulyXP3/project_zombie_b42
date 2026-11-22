package modcore.radar;

import modcore.radar.ItemScanCoverage.Pos;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/** Committed scalar source contributions and 3D aggregates; work is isolated by partition COW. */
public final class ItemScanIndex {
    public record Hit(Pos position,String name,long count){
        public Hit{Objects.requireNonNull(position);Objects.requireNonNull(name);if(count<=0)throw new IllegalArgumentException("nonpositive hit count");}
    }
    public record SourceKey(boolean vehicle,int x,int y,int z){
        public SourceKey{if(vehicle&&(x!=0||y!=0||z!=0))throw new IllegalArgumentException("invalid vehicle source key");}
        public static SourceKey grid(Pos p){return new SourceKey(false,p.x(),p.y(),p.z());}
        public static SourceKey vehicles(){return new SourceKey(true,0,0,0);}
    }
    public record Partition(long column,int z){
        public int cx(){return (int)(column>>32);} public int cy(){return (int)column;}
    }
    private record SourceEntry(Map<Pos,Hit> hits,long order) {}
    private record Contribution(String name,long count,long order) {}
    private record Aggregate(Hit hit,Map<SourceKey,Contribution> contributors) {}
    private static final class SourceBucket{final SourceEntry[] entries=new SourceEntry[64];int count;}
    private static final class AggregateBucket{final Aggregate[] entries=new Aggregate[64];int count;}
    private final Map<Partition,SourceBucket> sources;
    private final Map<Partition,AggregateBucket> aggregates;
    private final SourceEntry vehicles;
    private final long maxOrder;
    public ItemScanIndex(){this(Map.of(),Map.of(),null,0);}
    private ItemScanIndex(Map<Partition,SourceBucket> sources,Map<Partition,AggregateBucket> aggregates,
                          SourceEntry vehicles,long maxOrder){
        this.sources=sources;this.aggregates=aggregates;this.vehicles=vehicles;this.maxOrder=maxOrder;
    }
    static Partition partition(Pos p){return new Partition(ItemScanCoverage.columnKey(Math.floorDiv(p.x(),8),Math.floorDiv(p.y(),8)),p.z());}
    static int offset(Pos p){return Math.floorMod(p.x(),8)*8+Math.floorMod(p.y(),8);}
    private static Partition partition(SourceKey s){return partition(new Pos(s.x(),s.y(),s.z()));}
    private SourceEntry source(SourceKey key){
        if(key.vehicle())return vehicles;
        var b=sources.get(partition(key));return b==null?null:b.entries[offset(new Pos(key.x(),key.y(),key.z()))];
    }
    public long maxOrder(){return maxOrder;}
    public Set<Partition> sourcePartitions(){return Collections.unmodifiableSet(sources.keySet());}
    public Map<Pos,Hit> sourceHits(SourceKey key){var s=source(key);return s==null?Map.of():s.hits();}
    public Iterable<SourceKey> sources(Partition p){
        SourceBucket b=sources.get(p);
        return ()->new Iterator<>(){
            int at;SourceKey next;
            private void find(){while(next==null&&b!=null&&at<64){int i=at++;if(b.entries[i]!=null)next=SourceKey.grid(new Pos(p.cx()*8+i/8,p.cy()*8+i%8,p.z()));}}
            public boolean hasNext(){find();return next!=null;}
            public SourceKey next(){find();if(next==null)throw new NoSuchElementException();var n=next;next=null;return n;}
        };
    }
    public Iterable<Hit> hits(){
        return ()->new Iterator<>(){
            final Iterator<AggregateBucket> buckets=aggregates.values().iterator();
            AggregateBucket bucket;int at;Hit next;
            private void find(){
                while(next==null){
                    if(bucket==null||at>=64){if(!buckets.hasNext())return;bucket=buckets.next();at=0;}
                    Aggregate a=bucket.entries[at++];if(a!=null)next=a.hit();
                }
            }
            public boolean hasNext(){find();return next!=null;}
            public Hit next(){find();if(next==null)throw new NoSuchElementException();var n=next;next=null;return n;}
        };
    }
    public Working working(){return new Working(this);}

    /** Root copies, touched 64-slot buckets and per-position contributor rebuilds all advance one unit. */
    public static final class Working {
        private final Map<Partition,SourceBucket> sources=new HashMap<>();
        private final Map<Partition,AggregateBucket> aggregates=new HashMap<>();
        private final Set<Partition> ownSources=new HashSet<>(),ownAggregates=new HashSet<>();
        private final Iterator<Map.Entry<Partition,SourceBucket>> sourceCopy;
        private final Iterator<Map.Entry<Partition,AggregateBucket>> aggregateCopy;
        private SourceEntry vehicles;private long maxOrder;private int copyPhase;private boolean closed;
        private Change change;
        private Working(ItemScanIndex base){
            sourceCopy=base.sources.entrySet().iterator();aggregateCopy=base.aggregates.entrySet().iterator();
            vehicles=base.vehicles;maxOrder=base.maxOrder;
        }
        private void open(){if(closed)throw new IllegalStateException("work snapshot already sealed");}
        public boolean isIdle(){return copyPhase==2&&change==null;}
        public void advance(){
            open();
            if(copyPhase==0){if(sourceCopy.hasNext()){var e=sourceCopy.next();sources.put(e.getKey(),e.getValue());}else copyPhase=1;return;}
            if(copyPhase==1){if(aggregateCopy.hasNext()){var e=aggregateCopy.next();aggregates.put(e.getKey(),e.getValue());}else copyPhase=2;return;}
            if(change!=null&&change.step())change=null;
        }
        /** Transfers ownership of the completed scalar hit map; its producer must not mutate it later. */
        public void startReplace(SourceKey source,Map<Pos,Hit> next,long order){
            open();if(!isIdle())throw new IllegalStateException("another index unit is pending");
            Objects.requireNonNull(source);Objects.requireNonNull(next);
            if(order<0)throw new IllegalArgumentException("negative source order");
            maxOrder=Math.max(maxOrder,order);change=new Change(source,next,order);
        }
        public ItemScanIndex snapshot(){
            open();if(!isIdle())throw new IllegalStateException("partial source change cannot publish");closed=true;
            return new ItemScanIndex(Collections.unmodifiableMap(sources),Collections.unmodifiableMap(aggregates),vehicles,maxOrder);
        }
        private SourceEntry source(SourceKey key){
            if(key.vehicle())return vehicles;
            var b=sources.get(partition(key));return b==null?null:b.entries[offset(new Pos(key.x(),key.y(),key.z()))];
        }
        private final class Change {
            final SourceKey key;final SourceEntry old,next;
            final Partition part;final int slot;
            final Iterator<Hit> oldHits,newHits;
            SourceBucket before,copy;int copied,phase;AggregateChange aggregate;
            Change(SourceKey key,Map<Pos,Hit> hits,long order){
                this.key=key;old=source(key);next=hits.isEmpty()?null:new SourceEntry(Collections.unmodifiableMap(hits),order);
                part=key.vehicle()?null:partition(key);slot=key.vehicle()?0:offset(new Pos(key.x(),key.y(),key.z()));
                oldHits=old==null?Collections.emptyIterator():old.hits().values().iterator();
                newHits=hits.values().iterator();
            }
            boolean step(){
                if(aggregate!=null){if(aggregate.step())aggregate=null;return false;}
                if(phase==0){
                    if(key.vehicle()){phase=2;return false;}
                    before=sources.get(part);
                    if(before==null){copy=new SourceBucket();sources.put(part,copy);ownSources.add(part);phase=2;}
                    else if(ownSources.contains(part)){copy=before;phase=2;}
                    else{copy=new SourceBucket();phase=1;}
                    return false;
                }
                if(phase==1){
                    SourceEntry e=before.entries[copied];copy.entries[copied++]=e;if(e!=null)copy.count++;
                    if(copied==64){sources.put(part,copy);ownSources.add(part);phase=2;}return false;
                }
                if(phase==2){
                    if(key.vehicle())vehicles=next;
                    else{
                        if(copy.entries[slot]==null&&next!=null)copy.count++;
                        if(copy.entries[slot]!=null&&next==null)copy.count--;
                        copy.entries[slot]=next;if(copy.count==0)sources.remove(part);
                    }
                    phase=3;return false;
                }
                if(phase==3){
                    if(oldHits.hasNext()){
                        Hit h=oldHits.next();Hit replacement=next==null?null:next.hits().get(h.position());
                        aggregate=new AggregateChange(h.position(),key,replacement,next==null?0:next.order());return false;
                    }
                    phase=4;return false;
                }
                if(phase==4){
                    if(newHits.hasNext()){
                        Hit h=newHits.next();if(old==null||!old.hits().containsKey(h.position()))
                            aggregate=new AggregateChange(h.position(),key,h,next.order());
                        return false;
                    }
                    return true;
                }
                throw new IllegalStateException("invalid source-change phase");
            }
        }
        private final class AggregateChange {
            final Pos pos;final SourceKey source;final Hit replacement;final long order;
            final Partition part;final int slot;
            final Map<SourceKey,Contribution> contributors=new LinkedHashMap<>();
            AggregateBucket before,bucket;Aggregate old;int copied,phase;
            Iterator<Map.Entry<SourceKey,Contribution>> copyEntries,sumEntries;
            Map.Entry<SourceKey,Contribution> best;long total;
            AggregateChange(Pos pos,SourceKey source,Hit replacement,long order){
                this.pos=pos;this.source=source;this.replacement=replacement;this.order=order;
                part=partition(pos);slot=offset(pos);
            }
            boolean step(){
                if(phase==0){
                    before=aggregates.get(part);
                    if(before==null){bucket=new AggregateBucket();aggregates.put(part,bucket);ownAggregates.add(part);phase=2;}
                    else if(ownAggregates.contains(part)){bucket=before;phase=2;}
                    else{bucket=new AggregateBucket();phase=1;}
                    return false;
                }
                if(phase==1){
                    Aggregate a=before.entries[copied];bucket.entries[copied++]=a;if(a!=null)bucket.count++;
                    if(copied==64){aggregates.put(part,bucket);ownAggregates.add(part);phase=2;}return false;
                }
                if(phase==2){
                    old=bucket.entries[slot];copyEntries=old==null?Collections.emptyIterator():old.contributors().entrySet().iterator();
                    phase=3;return false;
                }
                if(phase==3){
                    if(copyEntries.hasNext()){var e=copyEntries.next();contributors.put(e.getKey(),e.getValue());return false;}
                    phase=4;return false;
                }
                if(phase==4){
                    if(replacement==null)contributors.remove(source);
                    else contributors.put(source,new Contribution(replacement.name(),replacement.count(),order));
                    sumEntries=contributors.entrySet().iterator();phase=5;return false;
                }
                if(phase==5){
                    if(sumEntries.hasNext()){
                        var e=sumEntries.next();total=Math.addExact(total,e.getValue().count());
                        if(best==null||earlier(e,best))best=e;return false;
                    }
                    Aggregate next=best==null?null:new Aggregate(new Hit(pos,best.getValue().name(),total),Collections.unmodifiableMap(contributors));
                    if(bucket.entries[slot]==null&&next!=null)bucket.count++;
                    if(bucket.entries[slot]!=null&&next==null)bucket.count--;
                    bucket.entries[slot]=next;if(bucket.count==0)aggregates.remove(part);return true;
                }
                throw new IllegalStateException("invalid aggregate-change phase");
            }
            private boolean earlier(Map.Entry<SourceKey,Contribution> a,Map.Entry<SourceKey,Contribution> b){
                if(a.getKey().vehicle()!=b.getKey().vehicle())return !a.getKey().vehicle();
                return a.getValue().order()<b.getValue().order();
            }
        }
    }
}
