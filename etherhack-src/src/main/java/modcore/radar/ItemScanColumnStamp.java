package modcore.radar;

import java.lang.ref.WeakReference;
import java.util.List;

/** Observes live mapping identity without pinning chunks or treating a short ID as permanent. */
public final class ItemScanColumnStamp {
    public record Slot(Object identity,long loadedFrame,int loadId,long mask) {}
    private record Witness(WeakReference<Object> identity,long frame,int loadId) {}
    private Witness[] previous=new Witness[0];
    private long revision;
    public ItemScanCoverage.Column observe(List<Slot> slots){
        boolean changed=previous.length!=slots.size();
        Witness[] next=changed?new Witness[slots.size()]:previous;
        long mask=0;
        for(int i=0;i<slots.size();i++){
            Slot s=slots.get(i);mask|=s.mask();Witness old=i<previous.length?previous[i]:null;
            if(old==null||(old.identity()==null?null:old.identity().get())!=s.identity()||old.frame()!=s.loadedFrame()||old.loadId()!=s.loadId()){
                changed=true;next[i]=new Witness(s.identity()==null?null:new WeakReference<>(s.identity()),s.loadedFrame(),s.loadId());
            }else next[i]=old;
        }
        previous=next;if(changed)revision=Math.incrementExact(revision);
        return new ItemScanCoverage.Column(mask,revision);
    }
}
