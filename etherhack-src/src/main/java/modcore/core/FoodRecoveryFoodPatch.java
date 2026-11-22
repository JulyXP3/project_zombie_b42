package modcore.core;

import modcore.utils.Patch;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.ArrayList;
import java.util.List;

/** Authoritative food consumption and remaining-field observations, never local getter proof. */
public final class FoodRecoveryFoodPatch {
    private static final String EAT="zombie/network/packets/actions/EatFoodPacket",FIELDS="zombie/network/packets/SyncItemFieldsPacket",HOOK="modcore/core/FoodRecoveryRuntime";
    private static final String DESC="(Lzombie/core/raknet/UdpConnection;)V";
    private static final int[] EAT_OPS={25,180,182,25,180,25,180,182,87,177};
    private static final int[] FIELD_OPS={25,180,182,25,180,182,58,25,25,180,182,25,182,153,25,25,180,182,25,25,180,182,25,25,180,182,25,25,180,182,25,25,180,182,25,25,180,182,25,25,180,182,25,25,180,182,25,180,153,25,25,180,182,25,193,153,25,192,58,25,25,180,182,25,25,180,182,25,25,180,182,25,180,153,25,25,180,182,182,25,25,180,182,182,25,25,180,182,182,25,182,25,180,182,87,25,4,182,25,193,153,25,25,180,182,25,193,153,25,192,58,25,25,180,182,25,25,180,182,25,193,153,25,192,58,25,25,180,182,25,25,180,182,25,25,182,25,25,180,182,25,25,180,182,25,25,180,182,25,182,153,25,25,180,182,25,182,25,25,182,25,180,182,58,25,198,25,182,25,193,153,18,184,177};
    private FoodRecoveryFoodPatch() { }
    public static void install(){
        Patch.injectIntoClass(EAT,"processClient",false,FoodRecoveryFoodPatch::requireEatShape,m->{
            InsnList h=new InsnList();h.add(new VarInsnNode(Opcodes.ALOAD,0));h.add(new FieldInsnNode(Opcodes.GETFIELD,EAT,"player","Lzombie/network/fields/character/ConnectionPlayer;"));
            h.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"zombie/network/fields/character/ConnectionPlayer","getPlayer","()Lzombie/characters/IsoPlayer;",false));
            h.add(new VarInsnNode(Opcodes.ALOAD,0));h.add(new FieldInsnNode(Opcodes.GETFIELD,EAT,"food","Lzombie/inventory/types/Food;"));
            h.add(new VarInsnNode(Opcodes.ALOAD,0));h.add(new FieldInsnNode(Opcodes.GETFIELD,EAT,"percentage","F"));h.add(new VarInsnNode(Opcodes.ALOAD,1));
            h.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"observeServerFood","(Lzombie/characters/IsoPlayer;Lzombie/inventory/types/Food;FLzombie/core/raknet/UdpConnection;)V",false));
            m.instructions.insert(h);
        },HOOK,"observeServerFood");
        Patch.injectIntoClass(FIELDS,"processClient",false,FoodRecoveryFoodPatch::requireFieldsShape,m->{
            for(AbstractInsnNode n:m.instructions.toArray())if(n.getOpcode()==Opcodes.RETURN){InsnList h=new InsnList();h.add(new VarInsnNode(Opcodes.ALOAD,2));h.add(new VarInsnNode(Opcodes.ALOAD,1));
                h.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"observeServerRemainder","(Lzombie/inventory/InventoryItem;Lzombie/core/raknet/UdpConnection;)V",false));m.instructions.insertBefore(n,h);}
        },HOOK,"observeServerRemainder");
        Patch.modifyClass(EAT,c->verifyEat(body(c)));Patch.modifyClass(FIELDS,c->verifyFields(body(c)));
    }
    public static void requireEatShape(MethodNode m){shape(m,EAT_OPS);int calls=0;for(AbstractInsnNode n:m.instructions)if(n instanceof MethodInsnNode c&&c.name.equals("EatOnClient")&&c.desc.equals("(Lzombie/inventory/InventoryItem;F)Z"))calls++;if(calls!=1)throw new IllegalStateException("original OnEat call changed");}
    public static void requireFieldsShape(MethodNode m){shape(m,FIELD_OPS);List<AbstractInsnNode> real=real(m);if(!(real.get(6) instanceof VarInsnNode v)||v.var!=2)throw new IllegalStateException("remainder item slot changed");
        int hunger=0,weight=0;for(AbstractInsnNode n:real)if(n instanceof MethodInsnNode c&&c.owner.equals("zombie/inventory/types/Food")){if(c.name.equals("setHungChange"))hunger++;if(c.name.equals("setActualWeight"))weight++;}if(hunger!=1||weight!=1)throw new IllegalStateException("food remainder application changed");}
    private static void shape(MethodNode m,int[] ops){if(!m.name.equals("processClient")||!m.desc.equals(DESC))throw new IllegalStateException("food receipt descriptor changed");List<AbstractInsnNode> r=real(m);if(r.size()!=ops.length)throw new IllegalStateException("food receipt instruction count changed");for(int i=0;i<ops.length;i++)if(r.get(i).getOpcode()!=ops[i])throw new IllegalStateException("food receipt opcode changed at "+i);}
    private static List<AbstractInsnNode> real(MethodNode m){List<AbstractInsnNode> r=new ArrayList<>();for(AbstractInsnNode n:m.instructions)if(n.getOpcode()>=0)r.add(n);return r;}
    private static MethodNode body(ClassNode c){return c.methods.stream().filter(m->m.name.equals("processClient")&&m.desc.equals(DESC)).findFirst().orElseThrow();}
    private static void verifyEat(MethodNode m){int hooks=0,original=0;boolean seen=false;for(AbstractInsnNode n:m.instructions)if(n instanceof MethodInsnNode c){if(c.owner.equals(HOOK)&&c.name.equals("observeServerFood")){hooks++;seen=true;}if(c.name.equals("EatOnClient")){original++;if(!seen)throw new IllegalStateException("food observation after callback");}}if(hooks!=1||original!=1)throw new IllegalStateException("food receipt hook missing or duplicated");}
    private static void verifyFields(MethodNode m){int returns=0;for(AbstractInsnNode n:m.instructions)if(n.getOpcode()==Opcodes.RETURN){returns++;if(!(n.getPrevious() instanceof MethodInsnNode c)||!c.owner.equals(HOOK)||!c.name.equals("observeServerRemainder")||!(c.getPrevious() instanceof VarInsnNode conn)||conn.var!=1||!(conn.getPrevious() instanceof VarInsnNode item)||item.var!=2)throw new IllegalStateException("remainder observation not after successful application");}if(returns!=1)throw new IllegalStateException("remainder return changed");}
}
