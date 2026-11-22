package modcore.core;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;

/** Per-click, lazy breadth-first inventory search. Empty-container bookkeeping is budgeted too. */
public final class FoodRecoverySelection<T,C> {
    public enum State { IDLE, SEARCHING, FOUND, EMPTY, CANCELLED }
    public interface Source<T,C> {
        T primary(); T secondary(); C root();
        int size(C container); T item(C container,int index); C child(T item);
        boolean eligible(T item);
    }
    private static final class Frame<C> { final C container;int index;Frame(C c){container=c;} }
    private final Source<T,C> source;
    private final ArrayDeque<Frame<C>> frames=new ArrayDeque<>();
    private final IdentityHashMap<C,Boolean> seen=new IdentityHashMap<>();
    private State state=State.IDLE;private T selected;private int checked;
    public FoodRecoverySelection(Source<T,C> source){this.source=java.util.Objects.requireNonNull(source);}
    public State state(){return state;}public T selected(){return selected;}public int checked(){return checked;}
    public void start(){
        frames.clear();seen.clear();selected=null;checked=0;
        T primary=source.primary();if(primary!=null&&source.eligible(primary)){selected=primary;state=State.FOUND;return;}
        T secondary=source.secondary();if(secondary!=null&&secondary!=primary&&source.eligible(secondary)){selected=secondary;state=State.FOUND;return;}
        C root=source.root();if(root==null){state=State.EMPTY;return;}enqueue(root);state=State.SEARCHING;
    }
    public void step(int budget){
        if(state!=State.SEARCHING||budget<=0)return;
        int work=0;
        while(work<budget&&!frames.isEmpty()){
            Frame<C> frame=frames.peekFirst();work++;
            if(frame.index>=source.size(frame.container)){frames.removeFirst();continue;}
            T item=source.item(frame.container,frame.index++);checked++;
            if(item!=null&&source.eligible(item)){selected=item;state=State.FOUND;frames.clear();seen.clear();return;}
            if(item!=null){C child=source.child(item);if(child!=null)enqueue(child);}
        }
        if(frames.isEmpty()){state=State.EMPTY;seen.clear();}
    }
    public void cancel(){frames.clear();seen.clear();selected=null;state=State.CANCELLED;}
    private void enqueue(C container){if(seen.put(container,Boolean.TRUE)==null)frames.addLast(new Frame<>(container));}
}
