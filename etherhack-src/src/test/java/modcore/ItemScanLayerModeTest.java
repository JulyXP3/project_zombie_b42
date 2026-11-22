package modcore;

import modcore.radar.ItemScanCoverage;
import modcore.radar.ItemScanCoverage.Column;
import modcore.radar.ItemScanCoverage.Pos;
import modcore.radar.ItemScanIndex.Hit;
import modcore.radar.ItemScanRuntime;
import modcore.radar.ItemScanRuntime.Mode;
import modcore.radar.ItemScanRuntime.Snapshot;
import modcore.radar.ItemScanRuntime.State;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static modcore.ItemScanFakeWorld.*;
import static org.junit.jupiter.api.Assertions.*;

/** Three selectable layer presets: all loaded floors, player +-2, current floor only. */
class ItemScanLayerModeTest {
    private static final Set<String> AXE = Set.of("Base.Axe");
    private static final long BUDGET = 3_000_000L;

    @Test void presetsProduceTheDocumentedWindows() {
        assertEquals(-1L, ItemScanRuntime.layerWindowFor(3, ItemScanRuntime.LAYER_ALL));
        assertEquals(ItemScanCoverage.mask(1, 5), ItemScanRuntime.layerWindowFor(3, ItemScanRuntime.LAYER_PM2));
        assertEquals(ItemScanCoverage.mask(3, 3), ItemScanRuntime.layerWindowFor(3, ItemScanRuntime.LAYER_CURRENT));
    }

    @Test void unknownModeFallsBackToAllFloorsAndInvalidPlayerZKeepsAllFloors() {
        assertEquals(-1L, ItemScanRuntime.layerWindowFor(3, 99));
        assertEquals(-1L, ItemScanRuntime.layerWindowFor(Double.NaN, ItemScanRuntime.LAYER_PM2));
        assertEquals(-1L, ItemScanRuntime.layerWindowFor(400, ItemScanRuntime.LAYER_PM2));
    }

    @Test void eachPresetScansExactlyItsOwnLayers() {
        assertEquals(Set.of(-3, -2, -1, 0, 1, 2, 3), scanZ(ItemScanRuntime.LAYER_ALL));
        assertEquals(Set.of(-2, -1, 0, 1, 2), scanZ(ItemScanRuntime.LAYER_PM2));
        assertEquals(Set.of(0), scanZ(ItemScanRuntime.LAYER_CURRENT));
    }

    private static Set<Integer> scanZ(int layerMode) {
        var w = new ItemScanFakeWorld();
        w.player.z = 0;
        w.columns.put(ItemScanCoverage.columnKey(12, 12), new Column(ItemScanCoverage.mask(-3, 3), 1));
        for (int z = -3; z <= 3; z++) w.putSquare(100, 100, z).floor.add(floor(100, 100, z, item("Base.Axe")));
        var rt = new ItemScanRuntime(w, new ItemScanRuntimeTest.Clock(), () -> new ItemScanRuntime.ResultSink() {
            final List<Hit> rows = new ArrayList<>();
            public void append(Hit h) { rows.add(h); }
            public Object seal() { return rows; }
        });
        var got = hits(finish(rt, rt.begin(AXE, Mode.FULL, 2, List.of(), 0, layerMode)));
        var zs = new java.util.TreeSet<Integer>();
        for (Pos p : got.keySet()) zs.add(p.z());
        return zs;
    }

    private static Snapshot finish(ItemScanRuntime r, long id) {
        for (int i = 0; i < 30000; i++) { var s = r.advance(id, BUDGET); if (s.state() != State.RUNNING) return s; }
        fail("runtime did not converge"); return null;
    }
    @SuppressWarnings("unchecked") private static Map<Pos, Hit> hits(Snapshot s) {
        assertEquals(State.READY, s.state(), s.error());
        var out = new HashMap<Pos, Hit>();
        for (var h : (List<Hit>) s.payload()) out.put(h.position(), h);
        return out;
    }
}
