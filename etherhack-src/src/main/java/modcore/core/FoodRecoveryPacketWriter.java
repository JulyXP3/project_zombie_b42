package modcore.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.entity.components.fluids.FluidContainer;
import zombie.network.fields.ContainerID;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.ItemStatsPacket;

/** Writes complete numeric Food values so a reused vanilla receiver cannot retain omitted old fields. */
public final class FoodRecoveryPacketWriter {
    private static final int[] FLAGS={16,32,64,128,256,512,1024,2048,4096,8192,16384,32768,65536,131072,262144,524288,2097152};
    private static final int[] WIDTH={4,4,4,4,16,4,4,4,4,4,4,4,4,4,1,1,4};
    private static final int KNOWN_FLAGS=0x3ffffff;
    private FoodRecoveryPacketWriter() { }

    /** Reuse vanilla serializers; only expand absent numeric Food fields after the original write. */
    public static void write(INetworkPacket packet,ByteBufferWriter writer)throws Exception{
        if(!(packet instanceof ItemStatsPacket item)||writer==null)throw new IllegalArgumentException("ItemStats food packet required");
        HeaderAccess access=AccessHolder.VALUE;
        if(access.failure()!=null)throw new IllegalStateException(access.failure());
        int start=writer.position();ByteBuffer scratch=ByteBuffer.allocate(8192).order(writer.bb.order());
        ContainerID container=(ContainerID)access.container().get(item);
        if(container==null)throw new IllegalArgumentException("missing packet container");
        container.write(scratch);int containerBytes=scratch.position(),fluidBytes=0;
        if((boolean)access.isFluid().get(item)){
            FluidContainer fluid=(FluidContainer)access.fluid().get(item);
            if(fluid==null)throw new IllegalArgumentException("missing packet fluid snapshot");
            scratch.clear();fluid.save(scratch);fluidBytes=scratch.position();
        }
        // Three ints, usedDelta, isFluid, optional fluid data, heat and isFood precede the bitmap.
        int foodOffset=Math.addExact(start,Math.addExact(containerBytes,Math.addExact(22,fluidBytes)));
        packet.write(writer);explicitFoodFields(writer.bb,foodOffset,writer.position());
    }
    static void explicitFoodFields(ByteBuffer packet,int offset,int end){
        if(packet==null||packet.isReadOnly()||offset<=0||end>packet.limit()||end-offset<12
                ||packet.get(offset-1)!=1)throw new IllegalArgumentException("invalid Food packet bounds");
        ByteBuffer source=packet.duplicate().order(packet.order());source.limit(end);source.position(offset);
        int flags=source.getInt();
        if((flags&~KNOWN_FLAGS)!=0||(flags&262144)==0)throw new IllegalArgumentException("unsupported Food bitmap shape");
        int added=0,complete=flags;
        for(int i=0;i<FLAGS.length;i++){complete|=FLAGS[i];if((flags&FLAGS[i])==0)added+=WIDTH[i];}
        int newEnd=Math.addExact(end,added);
        if(newEnd>packet.limit())throw new IllegalArgumentException("insufficient Food packet capacity");
        ByteBuffer expanded=ByteBuffer.allocate(end-offset+added).order(packet.order());expanded.putInt(complete);
        for(int i=0;i<FLAGS.length;i++){
            int width=WIDTH[i];
            if((flags&FLAGS[i])!=0){
                if(source.remaining()<width)throw new IllegalArgumentException("truncated Food fields");
                byte[] value=new byte[width];source.get(value);expanded.put(value);
            }else if(FLAGS[i]==524288){
                // Vanilla omits poisonDetectionLevel at -1, not at zero.
                expanded.put((byte)-1);
            }else for(int n=0;n<width;n++)expanded.put((byte)0);
        }
        // Extra ingredients, spices, fertilisation, name/weight and wetness stay byte-for-byte intact.
        if(source.remaining()<7)throw new IllegalArgumentException("truncated Food tail");
        expanded.put(source);expanded.flip();
        ByteBuffer output=packet.duplicate();output.position(offset);output.put(expanded);packet.position(newEnd);
    }
    private record HeaderAccess(VarHandle container,VarHandle isFluid,VarHandle fluid,String failure) { }
    private static final class AccessHolder {
        static final HeaderAccess VALUE=load();
        static HeaderAccess load(){
            try{
                MethodHandles.Lookup lookup=MethodHandles.privateLookupIn(ItemStatsPacket.class,MethodHandles.lookup());
                return new HeaderAccess(lookup.findVarHandle(ItemStatsPacket.class,"containerId",ContainerID.class),
                        lookup.findVarHandle(ItemStatsPacket.class,"isFluidContainer",boolean.class),
                        lookup.findVarHandle(ItemStatsPacket.class,"fluidContainer",FluidContainer.class),null);
            }catch(ReflectiveOperationException|RuntimeException|LinkageError e){
                // This packet has no public header getters. Do not force accessibility or guess offsets.
                return new HeaderAccess(null,null,null,"Food packet header access unavailable: "+e);
            }
        }
    }
}
