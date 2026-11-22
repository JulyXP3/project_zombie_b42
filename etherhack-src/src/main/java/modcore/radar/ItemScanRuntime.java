package modcore.radar;

import modcore.radar.ItemScanCoverage.Column;
import modcore.radar.ItemScanCoverage.Pos;
import modcore.radar.ItemScanCoverage.PatrolCursor;
import modcore.radar.ItemScanIndex.Hit;
import modcore.radar.ItemScanIndex.Partition;
import modcore.radar.ItemScanIndex.SourceKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Single-owner world reads, scalar COW changes and complete-result construction share one budget. */
public final class ItemScanRuntime {
    public enum Mode { FULL,MOVE,DIRTY,PATROL }
    public enum State { IDLE,RUNNING,READY,CANCELLED,ERROR,TIMEOUT }
    public enum Phase { COVERAGE,COPY,PRUNE,SQUARES,VEHICLES,DELTA,EXPORT,DONE }
    public interface ResultSink {void append(Hit hit);Object seal();}
    public record Stats(long cells,long squares,long floor,long containers,long bags,long items,long bodies,
                        long columns,long columnLayers,long emitted,long nanos,long prepareNanos,Map<Phase,Long> phaseNanos) {}
    public record Snapshot(long ticket,State state,Phase phase,Stats stats,ItemScanWindow window,Object payload,String error) {}
    private static final long MAX_BUDGET_NS=3_000_000L,TIMEOUT_NS=120_000_000_000L,MAX_TICKET=9_007_199_254_740_991L;
    /** Fixed player-relative window: one floor below, the current floor, then two floors above. */
    public static final int LAYERS_BELOW=1,LAYERS_ABOVE=2;
    /** Selectable layer presets; any unknown value falls back to all loaded floors. */
    public static final int LAYER_ALL=0,LAYER_PM2=1,LAYER_CURRENT=2;
    private final ItemScanWorld world;
    private final LongSupplier clock;
    private final Supplier<ResultSink> sinks;
    private ItemScanIndex committed=new ItemScanIndex();
    private ItemScanCoverage coverage;
    private Set<String> committedTargets=Set.of();
    private Object ownerPlayer,ownerCell;
    private Thread ownerThread;
    private PatrolCursor patrol;
    // Scalar scheduling state is owned by the same committed generation as its source index.
    private LinkedHashSet<SourceKey> refreshQueue=new LinkedHashSet<>();
    private long sequence;
    private Request request;
    private Snapshot terminal;

    public ItemScanRuntime(ItemScanWorld world,LongSupplier clock,Supplier<ResultSink> sinks){
        this.world=Objects.requireNonNull(world);this.clock=Objects.requireNonNull(clock);this.sinks=Objects.requireNonNull(sinks);
    }
    /** Target copying is synchronous and measured. Dirty input transfers an immutable/owned snapshot. */
    public long begin(Set<String> targets,Mode mode,int radius,Iterable<Pos> dirty,int patrolCells){
        return begin(targets,mode,radius,dirty,patrolCells,LAYER_ALL);
    }
    public long begin(Set<String> targets,Mode mode,int radius,Iterable<Pos> dirty,int patrolCells,int layerMode){
        if(ownerThread!=null&&ownerThread!=Thread.currentThread())throw new IllegalStateException("scan owner thread changed");
        ownerThread=Thread.currentThread();
        if(sequence==MAX_TICKET)throw new IllegalStateException("scan ticket range exhausted");
        long id=++sequence,started=clock.getAsLong();request=null;
        try{
            Set<String> copied=Set.copyOf(Objects.requireNonNull(targets));Objects.requireNonNull(mode);
            Object player=world.player(),cell=world.cell();
            if(player==null||cell==null)throw new IllegalStateException("player or world unavailable");
            boolean newOwner=ownerPlayer!=player||ownerCell!=cell;
            if(newOwner){committed=new ItemScanIndex();coverage=null;committedTargets=Set.of();patrol=null;refreshQueue.clear();world.clear();}
            long layerWindow=layerWindowFor(world.z(player),layerMode);
            boolean full=mode==Mode.FULL||coverage==null||newOwner||!copied.equals(committedTargets)||radius!=coverage.window().radius();
            ItemScanWindow window=full||mode==Mode.MOVE?ItemScanWindow.around(world.x(player),world.y(player),radius):coverage.window();
            if(!full&&mode==Mode.MOVE&&(Math.abs((long)window.cx()-coverage.window().cx())>radius||
                    Math.abs((long)window.cy()-coverage.window().cy())>radius))full=true;
            ResultSink sink=Objects.requireNonNull(sinks.get());
            long prepared=Math.max(0,clock.getAsLong()-started);
            request=new Request(id,started,prepared,player,cell,copied,mode,window,full,
                    dirty==null?List.of():dirty,Math.max(0,patrolCells),sink,layerWindow);
            terminal=request.snapshot(State.RUNNING,null,null,started+prepared);
        }catch(RuntimeException ex){terminal=empty(id,State.ERROR,null,ex.toString());}
        return id;
    }
    public Snapshot advance(long id,long budgetNanos){
        Request q=request;
        if(q==null)return terminal!=null&&terminal.ticket()==id?terminal:empty(id,State.CANCELLED,null,null);
        if(q.id!=id)return empty(id,State.CANCELLED,null,null);
        if(ownerThread!=Thread.currentThread())return q.snapshot(State.ERROR,null,"scan owner thread changed",q.started);
        long now=clock.getAsLong();
        try{
            if(world.player()!=q.player||world.cell()!=q.cell){cancel();return terminal;}
            if(now-q.started>TIMEOUT_NS){request=null;terminal=q.snapshot(State.TIMEOUT,null,"timeout",now);return terminal;}
            if(budgetNanos<=0)throw new IllegalArgumentException("nonpositive scan budget");
            if(q.job!=null&&q.job.square!=null){
                Object current=world.square(q.job.origin.x(),q.job.origin.y(),q.job.origin.z());
                if(current!=q.job.square){
                    Pos origin=q.job.origin;q.job=null;
                    if(current==null)q.replace(SourceKey.grid(origin),Map.of(),Phase.SQUARES);
                    else{q.squares++;q.job=new SourceJob(q,origin,current,false);}
                }
            }
            long begin=now,budget=Math.min(budgetNanos,MAX_BUDGET_NS);
            do{
                Phase phase=q.phase;long before=now;q.step();now=clock.getAsLong();
                q.times[phase.ordinal()]=Math.addExact(q.times[phase.ordinal()],Math.max(0,now-before));
                if(q.phase==Phase.DONE){
                    committed=q.shadow;coverage=q.current;committedTargets=q.targets;
                    ownerPlayer=q.player;ownerCell=q.cell;patrol=q.plan.patrolCursor();refreshQueue=q.refreshOrder;
                    Snapshot ready=q.snapshot(State.READY,q.payload,null,now);
                    terminal=q.snapshot(State.READY,null,null,now);request=null;return ready;
                }
            }while(now-begin<budget);
            terminal=q.snapshot(State.RUNNING,null,null,now);return terminal;
        }catch(RuntimeException ex){request=null;terminal=q.snapshot(State.ERROR,null,ex.toString(),now);return terminal;}
    }
    public void cancel(){
        if(ownerThread!=null&&ownerThread!=Thread.currentThread())throw new IllegalStateException("scan owner thread changed");
        long id=request==null?sequence:request.id;ItemScanWindow window=request==null?(coverage==null?null:coverage.window()):request.window;
        request=null;committed=new ItemScanIndex();coverage=null;committedTargets=Set.of();patrol=null;
        ownerPlayer=null;ownerCell=null;refreshQueue.clear();world.clear();terminal=empty(id,State.CANCELLED,window,null);
    }
    private static Snapshot empty(long id,State state,ItemScanWindow w,String error){
        return new Snapshot(id,state,Phase.DONE,new Stats(0,0,0,0,0,0,0,0,0,0,0,0,Map.of()),w,null,error);
    }
    /** All layers stay eligible when the player floor is unknown or the mode is unknown. */
    public static long layerWindowFor(double playerZ,int layerMode){
        double z=Math.floor(playerZ);
        if(!Double.isFinite(z)||z<-32||z>31)return -1L;
        return switch(layerMode){
            case LAYER_PM2 -> ItemScanCoverage.relativeLayers((int)z,2,2);
            case LAYER_CURRENT -> ItemScanCoverage.relativeLayers((int)z,0,0);
            default -> -1L;
        };
    }
    private Pos position(Object o){
        double x=Math.floor(world.x(o)),y=Math.floor(world.y(o)),z=Math.floor(world.z(o));
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||x<Integer.MIN_VALUE||x>Integer.MAX_VALUE||
                y<Integer.MIN_VALUE||y>Integer.MAX_VALUE||z<-32||z>31)return null;
        return new Pos((int)x,(int)y,(int)z);
    }
    private static List<?> list(List<?> value){return value==null?List.of():value;}

    private final class Request {
        final long id,started,prepared;
        final Object player,cell;
        final Set<String> targets;
        final Mode mode;
        final ItemScanWindow window;
        final boolean full;
        final Iterable<Pos> dirty;
        final int patrolCells,refreshLimit;
        int refreshAttempts;
        final List<Pos> refreshing=new ArrayList<>(8);
        final LinkedHashSet<SourceKey> refreshOrder=new LinkedHashSet<>();
        final Iterator<SourceKey> refreshCopy;
        final ResultSink sink;
        final long layerWindow;
        final ItemScanIndex base;
        final ItemScanIndex.Working work;
        final Map<Long,Column> captured=new LinkedHashMap<>();
        final List<ItemScanCoverage.ColumnRef> refs=new ArrayList<>();
        final long[] times=new long[Phase.values().length];
        ItemScanCoverage current;ItemScanCoverage.Plan plan;
        Phase phase=Phase.COVERAGE,resume;
        int cx,cy;boolean retained;
        long total,order,cells,squares,floor,containers,bags,items,bodies,columns,columnLayers,emitted;
        Iterator<Partition> partitions;Iterator<SourceKey> pruning;
        SourceJob job;boolean vehiclesStarted;
        ItemScanIndex shadow;Iterator<Hit> exporting;Object payload;
        Request(long id,long started,long prepared,Object player,Object cell,Set<String> targets,Mode mode,
                ItemScanWindow window,boolean full,Iterable<Pos> dirty,int patrolCells,ResultSink sink,long layerWindow){
            this.id=id;this.started=started;this.prepared=prepared;this.player=player;this.cell=cell;
            this.targets=targets;this.mode=mode;this.window=window;this.full=full;this.dirty=dirty;this.patrolCells=patrolCells;this.sink=sink;
            this.layerWindow=layerWindow;
            // Reserve at least three quarters for broad discovery. Even dense hit sets cannot starve it.
            refreshLimit=full?0:Math.min(8,patrolCells/4);
            refreshCopy=full?Collections.emptyIterator():refreshQueue.iterator();
            base=full?new ItemScanIndex():committed;work=base.working();order=base.maxOrder();
            cx=window.minChunkX();cy=window.minChunkY();
        }
        Snapshot snapshot(State state,Object payload,String error,long now){
            var phases=new EnumMap<Phase,Long>(Phase.class);
            for(Phase p:Phase.values())if(times[p.ordinal()]!=0)phases.put(p,times[p.ordinal()]);
            return new Snapshot(id,state,phase,new Stats(cells,squares,floor,containers,bags,items,bodies,columns,columnLayers,
                    emitted,Math.max(0,now-started),prepared,Collections.unmodifiableMap(phases)),window,payload,error);
        }
        void replace(SourceKey key,Map<Pos,Hit> next,Phase back){
            if(!key.vehicle()){if(next.isEmpty())refreshOrder.remove(key);else refreshOrder.add(key);}
            if(next.isEmpty()&&base.sourceHits(key).isEmpty()){phase=back;return;}
            long rank=full&&!key.vehicle()?window.fullOrder(new Pos(key.x(),key.y(),key.z())):Math.incrementExact(order);
            order=Math.max(order,rank);work.startReplace(key,next,rank);resume=back;phase=Phase.DELTA;
        }
        boolean hasLayer(Partition p){
            Column column=current.columns().get(p.column());return column!=null&&ItemScanCoverage.validZ(p.z())&&
                    (column.mask()&(1L<<(p.z()+32)))!=0;
        }
        void step(){
            switch(phase){
                case COVERAGE -> {
                    if(!retained){world.retainWindow(window);retained=true;return;}
                    if(cx>window.maxChunkX()){
                        current=ItemScanCoverage.owned(window,captured,refs,total);
                        phase=Phase.COPY;return;
                    }
                    long key=ItemScanCoverage.columnKey(cx,cy);Column raw=Objects.requireNonNull(world.probeColumn(cx,cy));
                    Column column=new Column(raw.mask()&layerWindow,raw.revision());
                    captured.put(key,column);var ref=ItemScanCoverage.columnRef(window,key,column);refs.add(ref);
                    columns++;columnLayers+=Long.bitCount(column.mask());total+=ref.area()*(long)Long.bitCount(column.mask());
                    if(cy==window.maxChunkY()){cy=window.minChunkY();cx++;}else cy++;
                }
                case COPY -> {
                    if(!work.isIdle())work.advance();
                    else if(refreshCopy.hasNext()){
                        SourceKey source=refreshCopy.next();
                        if(current.contains(new Pos(source.x(),source.y(),source.z()))&&!base.sourceHits(source).isEmpty())
                            refreshOrder.add(source);
                    }else{refreshAttempts=Math.min(refreshOrder.size(),32);partitions=base.sourcePartitions().iterator();phase=Phase.PRUNE;}
                }
                case PRUNE -> {
                    if(pruning!=null){
                        if(pruning.hasNext()){
                            SourceKey key=pruning.next();if(!current.contains(new Pos(key.x(),key.y(),key.z())))replace(key,Map.of(),Phase.PRUNE);
                        }else pruning=null;
                        return;
                    }
                    if(!partitions.hasNext()){
                        if(refreshing.size()<refreshLimit&&refreshAttempts>0&&!refreshOrder.isEmpty()){
                            refreshAttempts--;Iterator<SourceKey> order=refreshOrder.iterator();
                            SourceKey source=order.next();order.remove();refreshOrder.add(source);
                            refreshing.add(new Pos(source.x(),source.y(),source.z()));
                            return;
                        }
                        Iterable<Pos> candidates=()->new Iterator<>(){
                            final Iterator<Pos> events=dirty.iterator(),positive=refreshing.iterator();
                            public boolean hasNext(){return events.hasNext()||positive.hasNext();}
                            public Pos next(){return events.hasNext()?events.next():positive.next();}
                        };
                        plan=new ItemScanCoverage.Plan(full?null:coverage,current,full,candidates,patrol,full?0:patrolCells-refreshing.size());
                        phase=Phase.SQUARES;return;
                    }
                    Partition p=partitions.next();
                    if(!hasLayer(p)||!window.equals(coverage==null?null:coverage.window())&&
                            (!window.contains(p.cx()*8,p.cy()*8)||!window.contains(p.cx()*8+7,p.cy()*8+7)))pruning=base.sources(p).iterator();
                }
                case SQUARES -> {
                    if(job!=null){
                        if(job.step()){
                            Pos p=job.origin;Map<Pos,Hit> next=job.result;job=null;replace(SourceKey.grid(p),next,Phase.SQUARES);
                        }
                        return;
                    }
                    if(plan.done()){phase=Phase.VEHICLES;return;}
                    Pos p=plan.step();if(p==null)return;cells++;
                    Object square=world.square(p.x(),p.y(),p.z());
                    if(square==null)replace(SourceKey.grid(p),Map.of(),Phase.SQUARES);
                    else{squares++;job=new SourceJob(this,p,square,false);}
                }
                case VEHICLES -> {
                    if(!vehiclesStarted){job=new SourceJob(this,null,null,true);vehiclesStarted=true;return;}
                    if(job!=null){
                        if(job.step()){
                            Map<Pos,Hit> next=job.result;job=null;replace(SourceKey.vehicles(),next,Phase.VEHICLES);
                        }
                        return;
                    }
                    shadow=work.snapshot();exporting=shadow.hits().iterator();phase=Phase.EXPORT;
                }
                case DELTA -> {work.advance();if(work.isIdle())phase=resume;}
                case EXPORT -> {
                    if(exporting.hasNext()){
                        Hit h=exporting.next();if(window.contains(h.position().x(),h.position().y())&&ItemScanCoverage.validZ(h.position().z())){
                            sink.append(h);emitted++;
                        }
                    }else{payload=sink.seal();phase=Phase.DONE;}
                }
                case DONE -> throw new IllegalStateException("completed request advanced");
            }
        }
    }
    private enum Kind { OBJECTS,FLOOR,BODIES,ITEMS,CONTAINERS,ITEM,BAG,VEHICLES,PARTS }
    private static final class Task {
        final Kind kind;final List<?> list;final Object object;final Pos position;int index;
        Task(Kind kind,List<?> list,Object object,Pos position){this.kind=kind;this.list=list;this.object=object;this.position=position;}
    }
    private static final class MutableHit {final String name;long count=1;MutableHit(String name){this.name=name;}}
    private final class SourceJob {
        final Request q;final Pos origin;final Object square;
        final ArrayDeque<Task> stack=new ArrayDeque<>();
        final Map<Pos,MutableHit> found=new LinkedHashMap<>();
        final Map<Pos,Hit> sealed=new LinkedHashMap<>();
        Iterator<Map.Entry<Pos,MutableHit>> finish;
        Map<Pos,Hit> result;
        SourceJob(Request q,Pos origin,Object square,boolean vehicles){
            this.q=q;this.origin=origin;this.square=square;
            if(vehicles)stack.push(new Task(Kind.VEHICLES,list(world.vehicles()),null,null));
            else{
                stack.push(new Task(Kind.BODIES,list(world.moving(square)),null,origin));
                stack.push(new Task(Kind.FLOOR,list(world.floor(square)),null,origin));
                stack.push(new Task(Kind.OBJECTS,list(world.objects(square)),null,origin));
            }
        }
        void pushItems(Object container,Pos position){
            if(container==null||position==null)return;
            q.containers++;List<?> items=world.items(container);if(items!=null)stack.push(new Task(Kind.ITEMS,items,null,position));
        }
        void scanItem(Object item,Pos p){
            if(item==null||p==null)return;q.items++;
            if(!q.targets.contains(world.fullType(item))&&!q.targets.contains(world.shortType(item)))return;
            MutableHit h=found.get(p);
            if(h==null)found.put(p,new MutableHit(Objects.requireNonNull(world.name(item))));else h.count=Math.incrementExact(h.count);
        }
        boolean step(){
            if(stack.isEmpty()){
                if(finish==null){finish=found.entrySet().iterator();return false;}
                if(finish.hasNext()){var e=finish.next();sealed.put(e.getKey(),new Hit(e.getKey(),e.getValue().name,e.getValue().count));return false;}
                result=Collections.unmodifiableMap(sealed);return true;
            }
            Task t=stack.peek();
            if(t.kind==Kind.ITEM){stack.pop();scanItem(t.object,t.position);return false;}
            if(t.kind==Kind.BAG){
                stack.pop();if(world.isBag(t.object)){q.bags++;pushItems(world.bag(t.object),t.position);}return false;
            }
            if(t.kind==Kind.CONTAINERS){
                if(t.index>=world.containerCount(t.object))stack.pop();else pushItems(world.container(t.object,t.index++),position(t.object));return false;
            }
            if(t.kind==Kind.PARTS){
                if(t.index>=world.partCount(t.object))stack.pop();
                else{Object part=world.part(t.object,t.index++);if(part!=null)pushItems(world.partContainer(part),position(part));}return false;
            }
            if(t.index>=t.list.size()){stack.pop();return false;}
            Object o=t.list.get(t.index++);if(o==null)return false;
            switch(t.kind){
                case ITEMS -> scanItem(o,t.position);
                case OBJECTS -> stack.push(new Task(Kind.CONTAINERS,null,o,null));
                case FLOOR -> {
                    q.floor++;Object item=world.floorItem(o);Pos p=position(o);
                    if(item!=null&&p!=null){stack.push(new Task(Kind.BAG,null,item,p));stack.push(new Task(Kind.ITEM,null,item,p));}
                }
                case BODIES -> {q.bodies++;if(world.isCorpse(o))pushItems(world.corpseContainer(o),position(o));}
                case VEHICLES -> {
                    // Vehicles follow the same layer preset as squares; a car on an excluded floor is skipped.
                    Pos p=position(o);
                    if(p!=null&&q.window.contains(p.x(),p.y())&&(q.layerWindow==-1L||(q.layerWindow&(1L<<(p.z()+32)))!=0))
                        stack.push(new Task(Kind.PARTS,null,o,null));
                }
                default -> throw new IllegalStateException("invalid traversal task");
            }
            return false;
        }
    }
}
