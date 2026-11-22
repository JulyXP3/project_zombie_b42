package modcore;

import modcore.core.MovementSpeedPatch;
import modcore.core.MovementSpeedRuntime;
import modcore.utils.Patch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import zombie.iso.Vector2;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MovementSpeedPatchTest {
    private static final String TARGET = "zombie/characters/IsoPlayer";
    private static final String DESC = "(Lzombie/iso/Vector2;Z)Lzombie/iso/Vector2;";
    private static final String HOOK = "modcore/core/MovementSpeedRuntime";
    @AfterEach void reset() { Patch.resetSession(); }

    @Test void everyOriginalReturnIsDecoratedAndTheOriginalMotionIsPreserved() throws Exception {
        prepare();
        MovementSpeedPatch.install();
        MethodNode method = target();
        assertEquals(3, calls(method, HOOK, "apply"));
        assertEquals(1, calls(method, "zombie/characters/IsoLivingCharacter", "getDeferredMovement"));
        assertEquals(2, calls(method, "zombie/iso/Vector2", "scale"));
        int returns = 0;
        for (AbstractInsnNode node : method.instructions) {
            if (node.getOpcode() != Opcodes.ARETURN) continue;
            returns++;
            assertInstanceOf(MethodInsnNode.class, node.getPrevious());
            MethodInsnNode call = (MethodInsnNode)node.getPrevious();
            assertEquals(HOOK, call.owner);
            assertEquals("apply", call.name);
            assertEquals("(Lzombie/iso/Vector2;Lzombie/characters/IsoPlayer;)Lzombie/iso/Vector2;", call.desc);
            assertInstanceOf(VarInsnNode.class, call.getPrevious());
            assertEquals(0, ((VarInsnNode)call.getPrevious()).var);
        }
        assertEquals(3, returns);
        assertNotNull(Patch.exportModifiedClasses().get(TARGET));
    }
    @Test void repeatedInstallationIsByteForByteIdempotent() throws Exception {
        prepare(); MovementSpeedPatch.install();
        byte[] before = Patch.exportModifiedClasses().get(TARGET);
        MovementSpeedPatch.install();
        assertArrayEquals(before, Patch.exportModifiedClasses().get(TARGET));
    }
    @Test void unexpectedOpcodeRejectsThePatchAndPreservesTheCachedClass() throws Exception {
        prepare();
        Patch.modifyClass(TARGET, c -> target(c).instructions.insert(new InsnNode(Opcodes.NOP)));
        byte[] before = Patch.exportModifiedClasses().get(TARGET);
        assertThrows(IllegalStateException.class, MovementSpeedPatch::install);
        assertTrue(Patch.getFailedClasses().contains(TARGET));
        assertArrayEquals(before, Patch.exportModifiedClasses().get(TARGET));
    }
    @Test void wrongDescriptorMissingSuperAndChangedReturnsFailTheOriginalShapeGuard() throws Exception {
        prepare();
        MethodNode source = target();
        MethodNode changed = copy(source); changed.desc = "(Lzombie/iso/Vector2;I)Lzombie/iso/Vector2;";
        MethodNode descriptor = changed;
        assertThrows(IllegalStateException.class, () -> MovementSpeedPatch.requireOriginalShape(descriptor));
        changed = copy(source);
        for (AbstractInsnNode n : changed.instructions.toArray())
            if (n instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL)
                changed.instructions.remove(n);
        MethodNode missingSuper = changed;
        assertThrows(IllegalStateException.class, () -> MovementSpeedPatch.requireOriginalShape(missingSuper));
        changed = copy(source); changed.instructions.add(new InsnNode(Opcodes.ARETURN));
        MethodNode addedReturn = changed;
        assertThrows(IllegalStateException.class, () -> MovementSpeedPatch.requireOriginalShape(addedReturn));
        changed = copy(source);
        for (AbstractInsnNode n : changed.instructions.toArray()) if (n.getOpcode() == Opcodes.ARETURN) {
            changed.instructions.remove(n); break;
        }
        MethodNode removedReturn = changed;
        assertThrows(IllegalStateException.class, () -> MovementSpeedPatch.requireOriginalShape(removedReturn));
    }
    @Test void aMarkedPatchWithOnlyTwoReturnsHookedCannotSilentlyPassReadback() throws Exception {
        prepare(); MovementSpeedPatch.install();
        Patch.modifyClass(TARGET, c -> {
            MethodNode method = target(c);
            for (AbstractInsnNode n : method.instructions.toArray()) {
                if (n instanceof MethodInsnNode call && HOOK.equals(call.owner)) {
                    method.instructions.remove(n.getPrevious());
                    method.instructions.remove(n);
                    break;
                }
            }
        });
        byte[] before = Patch.exportModifiedClasses().get(TARGET);
        assertThrows(IllegalStateException.class, MovementSpeedPatch::install);
        assertArrayEquals(before, Patch.exportModifiedClasses().get(TARGET));
    }
    @Test void nullOrUnavailableRuntimeContextLeavesTheExactVectorObjectUntouched() {
        Vector2 vector = new Vector2(1.25f, -2.5f);
        assertSame(vector, MovementSpeedRuntime.apply(vector, null));
        assertEquals(1.25f, vector.x); assertEquals(-2.5f, vector.y);
        assertNull(MovementSpeedRuntime.apply(null, null));
    }
    @Test void runtimeMethodSignaturesAreAsmFree() {
        for (java.lang.reflect.Method method : MovementSpeedRuntime.class.getDeclaredMethods()) {
            assertFalse(method.getReturnType().getName().startsWith("org.objectweb.asm."));
            for (Class<?> parameter : method.getParameterTypes())
                assertFalse(parameter.getName().startsWith("org.objectweb.asm."));
        }
    }
    @Test void survivalPresetButtonsSelectOneTwoThreeThroughRealMouseHandlers() throws Exception {
        LuaTestRuntime lua = movementUi();
        Object[] values = lua.execute("""
            EtherCharacterPanel.placeMovementPresets(panel,0,0,180)
            for i=1,3 do
                local b=panel.movementPresetButtons[i]
                b:onMouseDown(0,0); b:onMouseUp(0,0)
            end
            return selected[1],selected[2],selected[3]
            """);
        assertArrayEquals(new Object[]{1.0,2.0,3.0}, values);
    }
    @Test void narrowPresetRowsWrapWithoutExtendingBeyondTheirModule() throws Exception {
        LuaTestRuntime lua = movementUi();
        Object[] values = lua.execute("""
            local h=EtherCharacterPanel.placeMovementPresets(panel,0,0,30)
            local a,b,c=panel.movementPresetButtons[1],panel.movementPresetButtons[2],panel.movementPresetButtons[3]
            return h,a.x+a.width,b.x+b.width,c.x+c.width,a.y,b.y,c.y
            """);
        assertArrayEquals(new Object[]{80.0,30.0,30.0,30.0,0.0,28.0,56.0},values);
    }
    @Test void missingMovementApiDisablesPresetChangesAndDisplaysOriginalSpeed() throws Exception {
        LuaTestRuntime lua = movementUi();
        Object[] values = lua.execute("""
            EtherCharacterPanel.placeMovementPresets(panel,0,0,180)
            setMovementSpeedMultiplier=nil; getMovementSpeedMultiplier=nil
            EtherCharacterPanel.refreshMovementPresets(panel)
            local b=panel.movementPresetButtons[3]
            b:onMouseDown(0,0); b:onMouseUp(0,0)
            return #selected,panel.movementPresetButtons[1].title,b.isEnable
            """);
        assertArrayEquals(new Object[]{0.0,"[1x]",false},values);
    }
    @Test void externalProfileResetUpdatesTheSelectedPresetMarker() throws Exception {
        LuaTestRuntime lua = movementUi();
        Object[] values = lua.execute("""
            EtherCharacterPanel.placeMovementPresets(panel,0,0,180)
            preset=3; EtherCharacterPanel.refreshMovementPresets(panel)
            local before=panel.movementPresetButtons[3].title
            preset=1; EtherCharacterPanel.refreshMovementPresets(panel)
            return before,panel.movementPresetButtons[1].title,panel.movementPresetButtons[3].title
            """);
        assertArrayEquals(new Object[]{"[3x]","[1x]","3x"},values);
    }
    private static LuaTestRuntime movementUi() throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
            require=function() end
            UIFont={Small=1}
            ISPanel={}
            function ISPanel:derive(name)
                local c={Type=name}; c.__index=c;setmetatable(c,{__index=self});return c
            end
            function ISPanel:new(x,y,w,h)
                local o={x=x,y=y,width=w,height=h};setmetatable(o,self);self.__index=self;return o
            end
            function ISPanel:initialise() end
            function ISPanel:instantiate() end
            function ISPanel:getIsVisible() return true end
            EtherFormPanel=ISPanel:derive('EtherFormPanel')
            function EtherFormPanel:prerender() end
            EtherTheme={ctrlGap=4,ctrlH=24,ctrlPadX=6}
            getTextManager=function() return {MeasureStringX=function(_,font,text) return #text*5 end} end
            getSoundManager=function() return {playUISound=function() end} end
            tr=function(key) return key end
            preset=2; selected={}
            getMovementSpeedMultiplier=function() return preset end
            setMovementSpeedMultiplier=function(value) preset=value;selected[#selected+1]=value end
            panel={children={}}
            function panel:addWidget(child) self.children[#self.children+1]=child end
            function panel:addChild(child) self.children[#self.children+1]=child end
            """);
        lua.resource("modcore/lua/components/ui/UIButton.lua");
        lua.resource("modcore/lua/components/panels/EtherCharacterPanel.lua");
        return lua;
    }
    private static void prepare() {
        Patch.resetSession();
        Patch.setProjectZomboidJarPath(System.getProperty("headshot.gameJar", Path.of("lib/projectzomboid.jar").toAbsolutePath().toString()));
        Patch.modifyClass(TARGET, c -> { });
    }
    @SuppressWarnings("unchecked") private static Map<String, ClassNode> cache() throws Exception {
        VarHandle h = MethodHandles.privateLookupIn(Patch.class, MethodHandles.lookup()).findStaticVarHandle(Patch.class, "classNodeMap", Map.class);
        return (Map<String, ClassNode>)h.get();
    }
    private static MethodNode target() throws Exception { return target(cache().get(TARGET)); }
    private static MethodNode target(ClassNode node) {
        return node.methods.stream().filter(m -> m.name.equals("getDeferredMovement") && m.desc.equals(DESC)).findFirst().orElseThrow();
    }
    private static MethodNode copy(MethodNode method) {
        MethodNode copy = new MethodNode(Opcodes.ASM9, method.access, method.name, method.desc, method.signature, null);
        method.accept(copy); return copy;
    }
    private static int calls(MethodNode method, String owner, String name) {
        int count=0; for (AbstractInsnNode n : method.instructions)
            if(n instanceof MethodInsnNode c && c.owner.equals(owner) && c.name.equals(name)) count++;
        return count;
    }
}
