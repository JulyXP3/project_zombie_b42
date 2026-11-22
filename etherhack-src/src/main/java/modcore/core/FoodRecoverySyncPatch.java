package modcore.core;

import modcore.utils.Patch;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.ArrayList;
import java.util.List;

/** Observe a successful, consistent incoming statistics payload without touching its values. */
public final class FoodRecoverySyncPatch {
    private static final String TARGET="zombie/network/packets/character/PlayerStatsPacket";
    private static final String DESC="(Lzombie/core/network/ByteBufferReader;Lzombie/network/IConnection;)V";
    private static final String HOOK="modcore/core/FoodRecoveryRuntime";
    private static final int[] OPS={25,180,25,25,182,25,25,182,153,25,180,182,58,25,182,25,180,184,182,
            25,182,25,180,182,25,25,182,182,25,182,25,180,184,182,167,58,178,25,18,178,182,177};
    private FoodRecoverySyncPatch() { }
    public static void install(){
        Patch.injectIntoClass(TARGET,"parse",false,FoodRecoverySyncPatch::requireOriginalShape,method->{
            MethodInsnNode loaded=lastLoad(method);
            InsnList hook=new InsnList();hook.add(new VarInsnNode(Opcodes.ALOAD,3));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"observeServerStats","(Lzombie/characters/IsoPlayer;)V",false));
            method.instructions.insert(loaded,hook);verifyHook(method);
        },HOOK,"observeServerStats");
        Patch.modifyClass(TARGET,c->verifyHook(c.methods.stream().filter(m->m.name.equals("parse")&&m.desc.equals(DESC)).findFirst().orElseThrow()));
    }
    public static void requireOriginalShape(MethodNode m){
        if(!m.name.equals("parse")||!m.desc.equals(DESC))throw new IllegalStateException("food stats parser descriptor changed");
        List<AbstractInsnNode> real=new ArrayList<>();for(AbstractInsnNode n:m.instructions)if(n.getOpcode()>=0)real.add(n);
        if(real.size()!=OPS.length)throw new IllegalStateException("food stats parser instruction count changed");
        for(int i=0;i<OPS.length;i++)if(real.get(i).getOpcode()!=OPS[i])throw new IllegalStateException("food stats parser opcode changed at "+i);
        if(!(real.get(12) instanceof VarInsnNode v)||v.var!=3)throw new IllegalStateException("food stats player local changed");
        if(m.tryCatchBlocks==null||m.tryCatchBlocks.size()!=1||!"java/io/IOException".equals(m.tryCatchBlocks.get(0).type))
            throw new IllegalStateException("food stats parser exception path changed");
        int stats=0,nutrition=0;for(AbstractInsnNode n:real)if(n instanceof MethodInsnNode call){
            if(call.owner.equals("zombie/characters/Stats")&&call.name.equals("load")&&call.desc.equals("(Ljava/nio/ByteBuffer;I)V"))stats++;
            if(call.owner.equals("zombie/characters/BodyDamage/Nutrition")&&call.name.equals("load")&&call.desc.equals("(Ljava/nio/ByteBuffer;)V"))nutrition++;
        }
        if(stats!=1||nutrition!=1)throw new IllegalStateException("food stats load path changed");lastLoad(m);
    }
    private static MethodInsnNode lastLoad(MethodNode m){
        MethodInsnNode result=null;for(AbstractInsnNode n:m.instructions)if(n instanceof MethodInsnNode c
                &&c.getOpcode()==Opcodes.INVOKEVIRTUAL&&c.owner.equals("zombie/characters/BodyDamage/BodyDamage")
                &&c.name.equals("loadMainFields")&&c.desc.equals("(Ljava/nio/ByteBuffer;I)V")){
            if(result!=null)throw new IllegalStateException("multiple final body loads");result=c;
        }
        if(result==null)throw new IllegalStateException("successful body load missing");return result;
    }
    private static void verifyHook(MethodNode m){
        MethodInsnNode load=lastLoad(m);int calls=0;
        for(AbstractInsnNode n:m.instructions)if(n instanceof MethodInsnNode c&&c.owner.equals(HOOK)&&c.name.equals("observeServerStats"))calls++;
        if(calls!=1||!(load.getNext() instanceof VarInsnNode v)||v.getOpcode()!=Opcodes.ALOAD||v.var!=3
                ||!(v.getNext() instanceof MethodInsnNode c)||c.getOpcode()!=Opcodes.INVOKESTATIC||!c.owner.equals(HOOK)
                ||!c.name.equals("observeServerStats")||!c.desc.equals("(Lzombie/characters/IsoPlayer;)V"))
            throw new IllegalStateException("food stats observation is not on the exact successful load path");
    }
}
