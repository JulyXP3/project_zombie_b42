package modcore.core;

import java.nio.ByteBuffer;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoveryPacketWriterTest {
    // Independent description of the numeric fields consumed by ItemStatsPacket.parse.
    static final int[] FLAGS={16,32,64,128,256,512,1024,2048,4096,8192,16384,32768,65536,131072,262144,524288,2097152};
    static final int[] WIDTH={4,4,4,4,16,4,4,4,4,4,4,4,4,4,1,1,4};
    static final byte[] PREFIX={(byte)0x80,0,0,0,7,8,9,1};
    static final byte[] TAIL={0,0,0x3f,0,0,0,0};
    @Test void zeroRollbackClearsAReusedReceiverInsteadOfKeepingTheLastDose(){
        Map<Integer,byte[]> dose=new HashMap<>();dose.put(512,floats(-2.8f));dose.put(4096,floats(4f));
        dose.put(16384,floats(-2.4f));dose.put(32768,floats(-4f));dose.put(65536,floats(-240f));dose.put(131072,floats(-320f));
        Receiver receiver=new Receiver();ByteBuffer first=raw(dose,0,1024);receiver.read(first);
        ByteBuffer vanillaZero=raw(Map.of(),0,1024);receiver.read(vanillaZero);
        assertEquals(-2.8f,receiver.value(512)); // Characterises the vanilla omitted-field reuse bug.
        ByteBuffer explicitZero=raw(Map.of(),0,1024);
        FoodRecoveryPacketWriter.explicitFoodFields(explicitZero,PREFIX.length,explicitZero.position());receiver.read(explicitZero);
        for(int flag:new int[]{512,4096,16384,32768,65536,131072})assertEquals(0f,receiver.value(flag));
    }
    @Test void nonzeroFieldsAndOpaqueTailRemainUnchanged(){
        Map<Integer,byte[]> values=new HashMap<>();values.put(128,floats(-.15f));values.put(256,floats(300f,6f,9f,30f));
        values.put(512,floats(-.075f));values.put(131072,floats(9f));ByteBuffer b=raw(values,0,1024);
        FoodRecoveryPacketWriter.explicitFoodFields(b,PREFIX.length,b.position());Receiver r=new Receiver();r.read(b);
        assertEquals(-.15f,r.value(128));assertEquals(-.075f,r.value(512));assertEquals(9f,r.value(131072));
        assertArrayEquals(PREFIX,Arrays.copyOf(b.array(),PREFIX.length));assertArrayEquals(TAIL,r.tail);
        assertEquals(b.position(),r.end);
    }
    @Test void anOmittedPoisonDetectionLevelMeansMinusOneNotZero(){
        ByteBuffer b=raw(Map.of(),0,1024);FoodRecoveryPacketWriter.explicitFoodFields(b,PREFIX.length,b.position());
        Receiver r=new Receiver();r.read(b);assertEquals(-1,r.fields.get(524288)[0]);assertEquals(0,r.fields.get(262144)[0]);
    }
    @Test void fullyExplicitNumericPayloadIsIdempotent(){
        Map<Integer,byte[]> all=new HashMap<>();for(int i=0;i<FLAGS.length;i++)all.put(FLAGS[i],new byte[WIDTH[i]]);
        all.put(524288,new byte[]{-1});ByteBuffer b=raw(all,0,1024);byte[] original=Arrays.copyOf(b.array(),b.position());
        FoodRecoveryPacketWriter.explicitFoodFields(b,PREFIX.length,b.position());
        assertArrayEquals(original,Arrays.copyOf(b.array(),b.position()));
    }
    @Test void unknownFlagShapesRejectWithoutChangingTheBuffer(){
        ByteBuffer b=raw(Map.of(),1<<26,1024);int end=b.position();byte[] original=b.array().clone();
        assertThrows(IllegalArgumentException.class,()->FoodRecoveryPacketWriter.explicitFoodFields(b,PREFIX.length,end));
        assertEquals(end,b.position());assertArrayEquals(original,b.array());
    }
    @Test void insufficientSpaceRejectsBeforeMutatingAnyBytes(){
        ByteBuffer temp=raw(Map.of(),0,1024);ByteBuffer b=ByteBuffer.allocate(temp.position());b.put(temp.array(),0,temp.position());
        int end=b.position();byte[] original=b.array().clone();
        assertThrows(IllegalArgumentException.class,()->FoodRecoveryPacketWriter.explicitFoodFields(b,PREFIX.length,end));
        assertEquals(end,b.position());assertArrayEquals(original,b.array());
    }
    @Test void currentJarHeaderAndInstantiationContractsMatchTheCodec() throws Exception {
        ClassNode n=load("zombie/network/packets/ItemStatsPacket.class");
        assertFalse(n.methods.stream().anyMatch(m->m.name.equals("shouldInstantiate")));
        MethodNode write=n.methods.stream().filter(m->m.name.equals("write")).findFirst().orElseThrow();
        List<String> calls=new ArrayList<>();for(AbstractInsnNode i:write.instructions){
            if(i instanceof MethodInsnNode m&&m.owner.equals("zombie/core/network/ByteBufferWriter")){
                calls.add(m.name);if(m.name.equals("putBoolean")&&Collections.frequency(calls,"putBoolean")==2)break;
            }
        }
        assertEquals(List.of("putInt","putInt","putInt","putFloat","putBoolean","putFloat","putBoolean"),calls);
        Map<Integer,Integer> widths=new HashMap<>();int flag=0;
        for(AbstractInsnNode i:write.instructions){
            if(i instanceof MethodInsnNode m&&m.name.equals("addFlags")&&m.desc.equals("(I)V")){
                AbstractInsnNode p=i.getPrevious();while(p!=null&&p.getOpcode()<0)p=p.getPrevious();
                if(p instanceof org.objectweb.asm.tree.LdcInsnNode ldc&&ldc.cst instanceof Integer value)flag=value;
                else if(p instanceof org.objectweb.asm.tree.IntInsnNode number)flag=number.operand;
                else if(p!=null&&p.getOpcode()>=org.objectweb.asm.Opcodes.ICONST_0&&p.getOpcode()<=org.objectweb.asm.Opcodes.ICONST_5)
                    flag=p.getOpcode()-org.objectweb.asm.Opcodes.ICONST_0;
                else throw new AssertionError("unexpected bitmap constant shape");
            }else if(i instanceof MethodInsnNode m&&m.owner.equals("zombie/core/network/ByteBufferWriter")){
                int width=switch(m.name){case "putFloat","putInt"->4;case "putByte"->1;default->0;};
                if(width!=0)widths.merge(flag,width,Integer::sum);
            }
        }
        for(int i=0;i<FLAGS.length;i++)assertEquals(WIDTH[i],widths.get(FLAGS[i]),"flag "+FLAGS[i]);
        assertTrue(n.fields.stream().anyMatch(f->f.name.equals("containerId")&&f.desc.equals("Lzombie/network/fields/ContainerID;")));
        assertTrue(n.fields.stream().anyMatch(f->f.name.equals("fluidContainer")&&f.desc.equals("Lzombie/entity/components/fluids/FluidContainer;")));
    }
    static ClassNode load(String name)throws Exception{
        try(var in=FoodRecoveryPacketWriterTest.class.getClassLoader().getResourceAsStream(name)){
            assertNotNull(in);ClassNode n=new ClassNode();new ClassReader(in).accept(n,0);return n;
        }
    }
    static byte[] floats(float... values){ByteBuffer b=ByteBuffer.allocate(values.length*4);for(float v:values)b.putFloat(v);return b.array();}
    static ByteBuffer raw(Map<Integer,byte[]> values,int extraFlags,int capacity){
        Map<Integer,byte[]> fields=new HashMap<>(values);fields.putIfAbsent(262144,new byte[]{0});
        int flags=extraFlags;for(int f:fields.keySet())flags|=f;ByteBuffer b=ByteBuffer.allocate(capacity);b.put(PREFIX).putInt(flags);
        for(int f:FLAGS)if(fields.containsKey(f))b.put(fields.get(f));b.put(TAIL);return b;
    }
    static final class Receiver {
        final Map<Integer,byte[]> fields=new HashMap<>();byte[] tail;int end;
        void read(ByteBuffer input){
            ByteBuffer b=input.duplicate();b.limit(input.position());b.position(PREFIX.length);int flags=b.getInt();
            for(int i=0;i<FLAGS.length;i++)if((flags&FLAGS[i])!=0){byte[] v=new byte[WIDTH[i]];b.get(v);fields.put(FLAGS[i],v);}
            tail=new byte[b.remaining()];b.get(tail);end=b.position();
        }
        float value(int flag){return ByteBuffer.wrap(fields.get(flag)).getFloat();}
    }
}
