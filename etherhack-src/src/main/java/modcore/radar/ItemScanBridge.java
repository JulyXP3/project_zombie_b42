package modcore.radar;

import modcore.radar.ItemScanCoverage.Pos;
import modcore.radar.ItemScanIndex.Hit;
import modcore.radar.ItemScanRuntime.Snapshot;
import modcore.radar.ItemScanRuntime.State;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** One call per advance: source reads, COW and complete Kahlua result construction stay inside Runtime. */
public final class ItemScanBridge {
    private final Supplier<KahluaTable> tables;
    private final LongSupplier clock;
    private final ItemScanRuntime runtime;
    private long current,argumentPrepare;
    private String setupError;
    public ItemScanBridge(ItemScanWorld world,LongSupplier clock,Supplier<KahluaTable> tables){
        this.tables=Objects.requireNonNull(tables);this.clock=Objects.requireNonNull(clock);
        runtime=new ItemScanRuntime(world,clock,()->new LuaSink());
    }
    public double begin(KahluaTable targets,String mode,int radius,KahluaTable dirty,int patrolCells){
        return begin(targets,mode,radius,dirty,patrolCells,ItemScanRuntime.LAYER_ALL);
    }
    public double begin(KahluaTable targets,String mode,int radius,KahluaTable dirty,int patrolCells,int layerMode){
        long before=clock.getAsLong();setupError=null;
        try{
            Set<String> selected=new HashSet<>();
            if(targets!=null){KahluaTableIterator it=targets.iterator();while(it.advance()){
                if(Boolean.TRUE.equals(it.getValue())){
                    if(!(it.getKey() instanceof String key))throw new IllegalArgumentException("invalid target type");selected.add(key);
                }
            }}
            var parsed=ItemScanRuntime.Mode.valueOf(Objects.requireNonNull(mode).toUpperCase(Locale.ROOT));
            Iterable<Pos> pending=dirty==null?List.of():dirtyPositions(dirty);
            argumentPrepare=Math.max(0,clock.getAsLong()-before);
            current=runtime.begin(selected,parsed,radius,pending,patrolCells,layerMode);return current;
        }catch(RuntimeException ex){runtime.cancel();setupError=ex.toString();current=0;return 0;}
    }
    public KahluaTable step(double id,double budgetMs){
        if(id==0&&setupError!=null)return errorFrame(id,setupError);
        if(!Double.isFinite(id)||id<0||id!=Math.floor(id)||id>9_007_199_254_740_991D)return errorFrame(id,"invalid scan ticket");
        if(!Double.isFinite(budgetMs))return errorFrame(id,"invalid scan budget");
        long budget=Math.max(1L,(long)(Math.max(0D,Math.min(3D,budgetMs))*1_000_000D));
        Snapshot s=runtime.advance((long)id,budget);KahluaTable frame=tables.get();
        frame.rawset("ticket",(double)s.ticket());frame.rawset("state",s.state().name().toLowerCase(Locale.ROOT));
        frame.rawset("phase",s.phase().name().toLowerCase(Locale.ROOT));if(s.error()!=null)frame.rawset("error",s.error());
        var stats=tables.get();var q=s.stats();long prep=s.ticket()==current?argumentPrepare:0;
        stats.rawset("cells",(double)q.cells());stats.rawset("squares",(double)q.squares());stats.rawset("floor",(double)q.floor());
        stats.rawset("containers",(double)q.containers());stats.rawset("bags",(double)q.bags());stats.rawset("items",(double)q.items());
        stats.rawset("bodies",(double)q.bodies());stats.rawset("columns",(double)q.columns());stats.rawset("columnLayers",(double)q.columnLayers());
        stats.rawset("emitted",(double)q.emitted());stats.rawset("elapsedMs",(q.nanos()+prep)/1_000_000D);
        stats.rawset("prepareMs",(q.prepareNanos()+prep)/1_000_000D);
        var phases=tables.get();for(var e:q.phaseNanos().entrySet())phases.rawset(e.getKey().name().toLowerCase(Locale.ROOT),e.getValue()/1_000_000D);
        stats.rawset("phaseMs",phases);frame.rawset("stats",stats);
        if(s.window()!=null){var w=s.window();var bounds=tables.get();bounds.rawset("cx",(double)w.cx());bounds.rawset("cy",(double)w.cy());
            bounds.rawset("radius",(double)w.radius());bounds.rawset("x1",(double)w.x1());bounds.rawset("x2",(double)w.x2());
            bounds.rawset("y1",(double)w.y1());bounds.rawset("y2",(double)w.y2());frame.rawset("window",bounds);}
        if(s.state()==State.READY&&s.payload()!=null)frame.rawset("results",s.payload());return frame;
    }
    public void cancel(){runtime.cancel();setupError=null;current=0;argumentPrepare=0;}
    private KahluaTable errorFrame(double id,String error){var f=tables.get();f.rawset("ticket",id);f.rawset("state","error");f.rawset("phase","done");f.rawset("error",error);return f;}
    /** Caller transfers this detached dirty table; dictionary and array forms are both streamed one row at a time. */
    private static Iterable<Pos> dirtyPositions(KahluaTable table){
        return ()->new Iterator<>(){
            final KahluaTableIterator rows=table.iterator();boolean prepared,has;Pos next;
            private void prepare(){if(prepared)return;prepared=true;has=rows.advance();
                if(has){if(!(rows.getValue() instanceof KahluaTable row))throw new IllegalArgumentException("invalid dirty square");
                    next=new Pos(integer(row.rawget("x")),integer(row.rawget("y")),integer(row.rawget("z")));}}
            public boolean hasNext(){prepare();return has;}
            public Pos next(){prepare();if(!has)throw new NoSuchElementException();Pos p=next;prepared=false;return p;}
        };
    }
    private static int integer(Object value){if(!(value instanceof Number n))throw new IllegalArgumentException("invalid square coordinate");
        double x=Math.floor(n.doubleValue());if(!Double.isFinite(x)||x<Integer.MIN_VALUE||x>Integer.MAX_VALUE)throw new IllegalArgumentException("square coordinate overflow");return (int)x;}
    private final class LuaSink implements ItemScanRuntime.ResultSink {
        final KahluaTable root=tables.get();int count;
        public void append(Hit hit){
            if(hit.count()>9_007_199_254_740_991L)throw new ArithmeticException("Lua count precision exhausted");
            var row=tables.get();row.rawset("x",(double)hit.position().x());row.rawset("y",(double)hit.position().y());
            row.rawset("z",(double)hit.position().z());row.rawset("name",hit.name());row.rawset("count",(double)hit.count());
            row.rawset("label",hit.name()+" x"+hit.count());root.rawset((double)++count,row);
        }
        public Object seal(){return root;}
    }
}
