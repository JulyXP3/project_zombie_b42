package modcore;

import modcore.core.FoodRecoverySyncPatch;
import modcore.core.FoodRecoveryRuntime;
import modcore.utils.Patch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoverySyncPatchTest {
    private static final String TARGET="zombie/network/packets/character/PlayerStatsPacket";
    @AfterEach void reset(){Patch.resetSession();}
    @Test void serverObservationRunsOnlyAfterAllStatsAndBodyFieldsLoadedSuccessfully() throws Exception {
        prepare();FoodRecoverySyncPatch.install();MethodNode m=method();int count=0;
        for(AbstractInsnNode n:m.instructions)if(n instanceof MethodInsnNode c&&c.owner.equals("modcore/core/FoodRecoveryRuntime")){
            count++;assertEquals("observeServerStats",c.name);assertEquals("(Lzombie/characters/IsoPlayer;)V",c.desc);
            assertInstanceOf(VarInsnNode.class,c.getPrevious());assertEquals(3,((VarInsnNode)c.getPrevious()).var);
            AbstractInsnNode previous=c.getPrevious().getPrevious();
            assertInstanceOf(MethodInsnNode.class,previous);assertEquals("loadMainFields",((MethodInsnNode)previous).name);
        }
        assertEquals(1,count);assertNotNull(Patch.exportModifiedClasses().get(TARGET));
    }
    @Test void repeatedParserPatchIsByteForByteIdempotent() throws Exception {
        prepare();FoodRecoverySyncPatch.install();byte[] before=Patch.exportModifiedClasses().get(TARGET);
        FoodRecoverySyncPatch.install();assertArrayEquals(before,Patch.exportModifiedClasses().get(TARGET));
    }
    @Test void changedParserShapeIsRejectedWithoutPublishingPartialWork() throws Exception {
        prepare();Patch.modifyClass(TARGET,c->method(c).instructions.insert(new InsnNode(Opcodes.NOP)));
        byte[] before=Patch.exportModifiedClasses().get(TARGET);
        assertThrows(IllegalStateException.class,FoodRecoverySyncPatch::install);
        assertArrayEquals(before,Patch.exportModifiedClasses().get(TARGET));
    }
    @Test void alteredPlayerSlotOrMissingFinalLoadCannotBecomeObservationProof() throws Exception {
        prepare();MethodNode m=copy(method());
        for(AbstractInsnNode n:m.instructions)if(n instanceof VarInsnNode v&&v.getOpcode()==Opcodes.ASTORE){v.var=4;break;}
        assertThrows(IllegalStateException.class,()->FoodRecoverySyncPatch.requireOriginalShape(m));
        MethodNode missing=copy(method());
        for(AbstractInsnNode n:missing.instructions.toArray())if(n instanceof MethodInsnNode c&&c.name.equals("loadMainFields")){missing.instructions.remove(n);break;}
        assertThrows(IllegalStateException.class,()->FoodRecoverySyncPatch.requireOriginalShape(missing));
    }
    @Test void missingGamePlayerObservationIsANoOpAndRuntimeSignaturesAreAsmFree() {
        assertDoesNotThrow(()->FoodRecoveryRuntime.observeServerStats(null));
        for(java.lang.reflect.Method m:FoodRecoveryRuntime.class.getDeclaredMethods()){
            assertFalse(m.getReturnType().getName().startsWith("org.objectweb.asm."));
            for(Class<?> p:m.getParameterTypes())assertFalse(p.getName().startsWith("org.objectweb.asm."));
        }
    }
    private static void prepare(){Patch.resetSession();Patch.setProjectZomboidJarPath(System.getProperty("headshot.gameJar",Path.of("lib/projectzomboid.jar").toAbsolutePath().toString()));Patch.modifyClass(TARGET,c->{});}
    @SuppressWarnings("unchecked") private static MethodNode method() throws Exception {
        VarHandle h=MethodHandles.privateLookupIn(Patch.class,MethodHandles.lookup()).findStaticVarHandle(Patch.class,"classNodeMap",Map.class);
        return method(((Map<String,ClassNode>)h.get()).get(TARGET));
    }
    private static MethodNode method(ClassNode c){return c.methods.stream().filter(m->m.name.equals("parse")).findFirst().orElseThrow();}
    private static MethodNode copy(MethodNode m){MethodNode c=new MethodNode(Opcodes.ASM9,m.access,m.name,m.desc,m.signature,null);m.accept(c);return c;}
}
