package modcore.core;

import org.junit.jupiter.api.Test;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;

import static org.junit.jupiter.api.Assertions.*;

class MapFrameDataTest {
    @Test
    void serializesOnlyNumbersStringsAndBooleansAndReusesLaneTables() {
        KahluaTable buffer = J2SEPlatform.getInstance().newTable();
        assertTrue(MapFrameData.collect(buffer, 15, source()));
        KahluaTable zombies = (KahluaTable) buffer.rawget("zombies");
        KahluaTable vehicles = (KahluaTable) buffer.rawget("vehicles");
        KahluaTable players = (KahluaTable) buffer.rawget("players");
        assertEquals(2, zombies.len());
        assertEquals(100.0, zombies.rawget(1));
        assertEquals(101.0, zombies.rawget(2));
        assertEquals("Base.Car", vehicles.rawget(3));
        assertEquals("other", players.rawget(3));
        assertEquals("2", players.rawget(4));
        assertEquals(true, players.rawget(5));
        assertEquals("self", buffer.rawget("localName"));
        assertTrue(MapFrameData.collect(buffer, 0, source()));
        assertSame(zombies, buffer.rawget("zombies"));
        assertSame(vehicles, buffer.rawget("vehicles"));
        assertSame(players, buffer.rawget("players"));
        assertEquals(0, zombies.len());
        assertEquals(0, vehicles.len());
        assertEquals(0, players.len());
        assertNull(buffer.rawget("localName"));
    }

    @Test
    void flagsAvoidDisabledSourceReadsAndInvalidCoordinatesAreSkipped() {
        KahluaTable buffer = J2SEPlatform.getInstance().newTable();
        var source = new MapFrameData.Source() {
            public boolean ready() { return true; }
            public void zombies(MapFrameData.Writer out) {
                out.zombie(Double.NaN, 1);
                out.zombie(2, Double.POSITIVE_INFINITY);
                out.zombie(3, 4);
            }
            public void vehicles(MapFrameData.Writer out) { fail("disabled vehicles were read"); }
            public void players(MapFrameData.Writer out) { fail("disabled players were read"); }
            public void localPlayer(MapFrameData.Writer out) { fail("disabled local player was read"); }
        };
        assertTrue(MapFrameData.collect(buffer, 1, source));
        KahluaTable lane = (KahluaTable) buffer.rawget("zombies");
        assertEquals(2, lane.len());
        assertEquals(3.0, lane.rawget(1));
        assertEquals(4.0, lane.rawget(2));
    }

    @Test
    void failedAndUnreadyCollectionsNeverPublishPartialOrStaleRows() {
        KahluaTable buffer = J2SEPlatform.getInstance().newTable();
        assertTrue(MapFrameData.collect(buffer, 15, source()));
        var broken = new MapFrameData.Source() {
            public boolean ready() { return true; }
            public void zombies(MapFrameData.Writer out) { out.zombie(7, 8); throw new IllegalStateException(); }
            public void vehicles(MapFrameData.Writer out) {}
            public void players(MapFrameData.Writer out) {}
            public void localPlayer(MapFrameData.Writer out) {}
        };
        assertFalse(MapFrameData.collect(buffer, 15, broken));
        assertEquals(0, ((KahluaTable) buffer.rawget("zombies")).len());
        assertEquals(0, ((KahluaTable) buffer.rawget("vehicles")).len());
        assertNull(buffer.rawget("localName"));
    }

    private static MapFrameData.Source source() {
        return new MapFrameData.Source() {
            public boolean ready() { return true; }
            public void zombies(MapFrameData.Writer out) { out.zombie(100, 101); }
            public void vehicles(MapFrameData.Writer out) { out.vehicle(102, 103, "Base.Car"); }
            public void players(MapFrameData.Writer out) { out.player(104, 105, "other", "2", true); }
            public void localPlayer(MapFrameData.Writer out) { out.localPlayer(106, 107, "self"); }
        };
    }
}
