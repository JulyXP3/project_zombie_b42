package modcore;

import modcore.core.FoodRecoveryFoodPatch;
import modcore.core.FoodRecoveryRuntime;
import modcore.utils.Patch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.lang.invoke.*;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoveryFoodPatchTest {
    private static final String EAT="zombie/network/packets/actions/EatFoodPacket",FIELDS="zombie/network/packets/SyncItemFieldsPacket",HOOK="modcore/core/FoodRecoveryRuntime";
    @AfterEach void reset(){Patch.resetSession();}
    @Test void fullFoodSnapshotIsObservedBeforeTheVanillaOnEatCallback() throws Exception {
        prepare();FoodRecoveryFoodPatch.install();MethodNode m=method(EAT);int hook=0,original=0,index=0,hookIndex=-1,originalIndex=-1;
        for(AbstractInsnNode n:m.instructions){if(n instanceof MethodInsnNode c){if(c.owner.equals(HOOK)&&c.name.equals("observeServerFood")){hook++;hookIndex=index;assertEquals("(Lzombie/characters/IsoPlayer;Lzombie/inventory/types/Food;FLzombie/core/raknet/UdpConnection;)V",c.desc);}if(c.name.equals("EatOnClient")){original++;originalIndex=index;}}index++;}
        assertEquals(1,hook);assertEquals(1,original);assertTrue(hookIndex<originalIndex);assertNotNull(Patch.exportModifiedClasses().get(EAT));
    }
    @Test void remainderIsObservedOnlyAfterTheOriginalFieldApplicationReturns() throws Exception {
        prepare();FoodRecoveryFoodPatch.install();MethodNode m=method(FIELDS);int count=0;
        for(AbstractInsnNode n:m.instructions)if(n.getOpcode()==Opcodes.RETURN){assertInstanceOf(MethodInsnNode.class,n.getPrevious());MethodInsnNode c=(MethodInsnNode)n.getPrevious();assertEquals(HOOK,c.owner);assertEquals("observeServerRemainder",c.name);count++;}
        assertEquals(1,count);assertNotNull(Patch.exportModifiedClasses().get(FIELDS));
    }
    @Test void repeatInstallationIsIdempotentForBothTargets() throws Exception {
        prepare();FoodRecoveryFoodPatch.install();Map<String,byte[]> before=Patch.exportModifiedClasses();FoodRecoveryFoodPatch.install();
        assertArrayEquals(before.get(EAT),Patch.exportModifiedClasses().get(EAT));assertArrayEquals(before.get(FIELDS),Patch.exportModifiedClasses().get(FIELDS));
    }
    @Test void changedFoodBodyRejectsBeforePublishingAPartialFoodPatch() throws Exception {
        prepare();Patch.modifyClass(EAT,c->body(c).instructions.insert(new InsnNode(Opcodes.NOP)));byte[] before=Patch.exportModifiedClasses().get(EAT);
        assertThrows(IllegalStateException.class,FoodRecoveryFoodPatch::install);assertArrayEquals(before,Patch.exportModifiedClasses().get(EAT));
    }
    @Test void changedItemSlotCannotBecomeAnAuthoritativeRemainder() throws Exception {
        prepare();MethodNode m=copy(method(FIELDS));for(AbstractInsnNode n:m.instructions)if(n instanceof VarInsnNode v&&v.getOpcode()==Opcodes.ASTORE){v.var=4;break;}
        assertThrows(IllegalStateException.class,()->FoodRecoveryFoodPatch.requireFieldsShape(m));
    }
    @Test void nullReceiptsDoNotTouchGameContextAndSignaturesStayAsmFree() {
        assertDoesNotThrow(()->FoodRecoveryRuntime.observeServerFood(null,null,0f,null));
        assertDoesNotThrow(()->FoodRecoveryRuntime.observeServerRemainder(null,null));
        for(java.lang.reflect.Method m:FoodRecoveryRuntime.class.getDeclaredMethods())for(Class<?> p:m.getParameterTypes())assertFalse(p.getName().startsWith("org.objectweb.asm."));
    }
    private static void prepare(){Patch.resetSession();Patch.setProjectZomboidJarPath(System.getProperty("headshot.gameJar",Path.of("lib/projectzomboid.jar").toAbsolutePath().toString()));Patch.modifyClass(EAT,c->{});Patch.modifyClass(FIELDS,c->{});}
    @SuppressWarnings("unchecked")private static MethodNode method(String name)throws Exception{VarHandle h=MethodHandles.privateLookupIn(Patch.class,MethodHandles.lookup()).findStaticVarHandle(Patch.class,"classNodeMap",Map.class);return body(((Map<String,ClassNode>)h.get()).get(name));}
    private static MethodNode body(ClassNode c){return c.methods.stream().filter(m->m.name.equals("processClient")).findFirst().orElseThrow();}
    private static MethodNode copy(MethodNode m){MethodNode n=new MethodNode(Opcodes.ASM9,m.access,m.name,m.desc,m.signature,null);m.accept(n);return n;}
}
