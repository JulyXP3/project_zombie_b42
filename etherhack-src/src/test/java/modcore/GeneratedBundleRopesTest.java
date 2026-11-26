package modcore;

import org.junit.jupiter.api.Test;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises real metadata, including its wire serialization; no game/world is started. */
class GeneratedBundleRopesTest {
    @Test void generatedLogBundlesRetainTwoDefaultRopesInTheSerializedPayload() throws Exception {
        for (String type : List.of("Base.LogStacks2", "Base.LogStacks3", "Base.LogStacks4")) {
            Fixture f = new Fixture(); f.remember(42, type); f.data.rawset("note", "keep");
            assertEquals(1, f.sync(42, type, false));
            assertEquals(List.of("Base.Rope", "Base.Rope"), values(f.wire.getFirst().rawget("ropeItems")));
            assertEquals("keep", f.wire.getFirst().rawget("note"));
            assertEquals(0, f.sync(42, type, false), "successful initialization must not repeat");
            assertEquals(1, f.wire.size());
        }
    }
    @Test void aGeneratedFirewoodBundleReturnsOneRope() throws Exception {
        Fixture f = new Fixture(); f.remember(42, "Base.FirewoodBundle");
        assertEquals(1, f.sync(42, "Base.FirewoodBundle", false));
        assertEquals(List.of("Base.Rope"), values(f.data.rawget("ropeItems")));
    }
    @Test void existingMixedRopeMaterialsAreNeverReplacedOrUploadedAgain() throws Exception {
        Fixture f = new Fixture(); KahluaTable ropes = table();
        ropes.rawset(0, "Base.SheetRope"); ropes.rawset(1, "Base.Rope");
        f.data.rawset("ropeItems", ropes); f.remember(42, "Base.LogStacks2");
        assertEquals(0, f.sync(42, "Base.LogStacks2", false));
        assertSame(ropes, f.data.rawget("ropeItems"));
        assertEquals(List.of("Base.SheetRope", "Base.Rope"), values(ropes));
        assertTrue(f.wire.isEmpty());
    }
    @Test void anUnknownEmptyBundleIsNotInventedIntoABoundBundle() throws Exception {
        Fixture f = new Fixture();
        assertEquals(0, f.sync(42, "Base.LogStacks2", false));
        assertNull(f.data.rawget("ropeItems")); assertTrue(f.wire.isEmpty());
    }
    @Test void anotherPlayerOrConnectionCannotUseAnOldGenerationRecord() throws Exception {
        Fixture f = new Fixture(); f.remember(42, "Base.LogStacks2");
        f.connection = new Object();
        assertEquals(0, f.sync(42, "Base.LogStacks2", false));
        assertNull(f.data.rawget("ropeItems"));
        f.remember(42, "Base.LogStacks2"); f.owner = new Object();
        assertEquals(0, f.sync(42, "Base.LogStacks2", false));
        assertNull(f.data.rawget("ropeItems"));
    }
    @Test void aBundleStillOnTheGroundStaysPendingUntilTheOwnedLookupFindsIt() throws Exception {
        Fixture f = new Fixture(); f.remember(42, "Base.LogStacks2");
        assertEquals(0, f.sync(0, "Base.LogStacks2", false));
        assertNull(f.data.rawget("ropeItems"));
        assertEquals(1, f.sync(42, "Base.LogStacks2", false));
    }
    @Test void aChangedTypeWithTheSameIdIsNotInitialized() throws Exception {
        Fixture f = new Fixture(); f.remember(42, "Base.LogStacks2");
        assertEquals(0, f.sync(42, "Base.LogStacks3", false));
        assertNull(f.data.rawget("ropeItems")); assertTrue(f.wire.isEmpty());
    }
    @Test void failedTransmissionRestoresTheMissingKeyAndCanRetry() throws Exception {
        Fixture f = new Fixture(); f.remember(42, "Base.LogStacks2");
        assertEquals(0, f.sync(42, "Base.LogStacks2", true));
        assertNull(f.data.rawget("ropeItems")); assertTrue(f.wire.isEmpty());
        assertEquals(1, f.sync(42, "Base.LogStacks2", false));
        assertEquals(List.of("Base.Rope", "Base.Rope"), values(f.data.rawget("ropeItems")));
    }
    @Test void failedTransmissionRestoresTheExactPreexistingEmptyTable() throws Exception {
        Fixture f = new Fixture(); KahluaTable empty = table(); f.data.rawset("ropeItems", empty);
        f.remember(42, "Base.LogStacks2"); assertEquals(0, f.sync(42, "Base.LogStacks2", true));
        assertSame(empty, f.data.rawget("ropeItems"));
    }
    @Test void malformedExistingDataAndUnsupportedTypesAreNotOverwritten() throws Exception {
        Fixture f = new Fixture(); f.data.rawset("ropeItems", "unexpected"); f.remember(42, "Base.LogStacks2");
        assertEquals(0, f.sync(42, "Base.LogStacks2", false));
        assertEquals("unexpected", f.data.rawget("ropeItems"));
        f.data.rawset("ropeItems", null); f.remember(43, "Base.BoxOfJars");
        assertEquals(0, f.sync(43, "Base.BoxOfJars", false)); assertNull(f.data.rawget("ropeItems"));
    }
    @Test void clearingASessionDropsOnlyPendingInitializationRecords() throws Exception {
        Fixture f = new Fixture(); f.remember(42, "Base.LogStacks2");
        invoke(f.implementation, "clear", new Class[0]);
        assertEquals(0, f.sync(42, "Base.LogStacks2", false)); assertNull(f.data.rawget("ropeItems"));
    }
    @Test void boundedRecordsEvictTheOldestWithoutTouchingItsMetadata() throws Exception {
        Fixture f = new Fixture(); for (int id = 1; id <= 129; id++) f.remember(id, "Base.LogStacks2");
        assertEquals(0, f.sync(1, "Base.LogStacks2", false)); assertNull(f.data.rawget("ropeItems"));
        assertEquals(1, f.sync(2, "Base.LogStacks2", false));
        f.data.rawset("ropeItems", null); assertEquals(1, f.sync(129, "Base.LogStacks2", false));
    }
    @Test void noPendingBundlesCauseNoInventoryLookupOnRepeatedEvents() throws Exception {
        Fixture f=new Fixture();java.util.concurrent.atomic.AtomicInteger reads=new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.BiFunction<Integer,String,KahluaTable> lookup=(id,type)->{reads.incrementAndGet();return f.data;};
        java.util.function.BiConsumer<Integer,KahluaTable> send=(id,data)->fail("Unexpected idle metadata upload");
        for(int i=0;i<100;i++)assertEquals(0,invoke(f.implementation,"synchronize",new Class[]{Object.class,Object.class,java.util.function.BiFunction.class,java.util.function.BiConsumer.class},f.connection,f.owner,lookup,send));
        assertEquals(0,reads.get());
    }
    static KahluaTable table() { return J2SEPlatform.getInstance().newTable(); }
    static List<String> values(Object value) {
        assertInstanceOf(KahluaTable.class, value); List<String> out = new ArrayList<>();
        var it = ((KahluaTable) value).iterator(); while (it.advance()) out.add((String) it.getValue());
        return out;
    }
    static Class<?> initializer() {
        try { return Class.forName("modcore.core.GeneratedBundleRopes"); }
        catch (ClassNotFoundException missing) { return fail("Generated bundle rope initialization is not implemented"); }
    }
    static Object invoke(Object target, String method, Class<?>[] types, Object... args) throws Exception {
        try { return target.getClass().getMethod(method, types).invoke(target, args); }
        catch (InvocationTargetException failure) { throw new AssertionError(failure.getCause()); }
        catch (NoSuchMethodException missing) { return fail("Missing bundle initialization contract: " + method); }
    }
    static class Fixture {
        Object connection = new Object(), owner = new Object();
        final Object implementation = initializer().getConstructor().newInstance();
        final KahluaTable data = table(); final List<KahluaTable> wire = new ArrayList<>();
        Fixture() throws Exception { }
        void remember(int id, String type) throws Exception {
            invoke(implementation, "remember", new Class[]{Object.class, Object.class, int.class, String.class}, connection, owner, id, type);
        }
        int sync(int availableId, String availableType, boolean fail) throws Exception {
            BiFunction<Integer, String, KahluaTable> lookup = (id, type) -> id == availableId && type.equals(availableType) ? data : null;
            BiConsumer<Integer, KahluaTable> send = (id, metadata) -> {
                assertEquals(availableId, id); if (fail) throw new IllegalStateException("network failure");
                try {
                    ByteBuffer b = ByteBuffer.allocate(8192); metadata.save(b); b.flip();
                    KahluaTable received = table(); received.load(b, 251); wire.add(received);
                } catch (java.io.IOException failure) { throw new AssertionError(failure); }
            };
            return (Integer) invoke(implementation, "synchronize", new Class[]{Object.class, Object.class, BiFunction.class, BiConsumer.class}, connection, owner, lookup, send);
        }
    }
}
