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

/** Relative layer maths and the default preset window; preset selection itself lives in ItemScanLayerModeTest. */
class ItemScanLayerWindowTest {
    private static final Set<String> AXE = Set.of("Base.Axe");
    private static final long BUDGET = 3_000_000L;

    @Test void windowIsOneBelowTwoAboveAndClampsAtWorldBounds() {
        assertEquals(ItemScanCoverage.mask(-1, 2), ItemScanCoverage.relativeLayers(0, 1, 2));
        assertEquals(ItemScanCoverage.mask(2, 5), ItemScanCoverage.relativeLayers(3, 1, 2));
        assertEquals(ItemScanCoverage.mask(-32, -30), ItemScanCoverage.relativeLayers(-32, 1, 2));
        assertEquals(ItemScanCoverage.mask(30, 31), ItemScanCoverage.relativeLayers(31, 1, 2));
    }

    @Test void onlyThePresetWindowLayersAreScanned() {
        var w = new ItemScanFakeWorld();
        w.columns.put(ItemScanCoverage.columnKey(12, 12), new Column(ItemScanCoverage.mask(-3, 6), 1));
        for (int z : new int[]{-3, -2, -1, 0, 1, 2, 3, 4}) {
            w.putSquare(100, 100, z).floor.add(floor(100, 100, z, item("Base.Axe")));
        }
        var rt = runtime(w).runtime;
        var got = hits(finish(rt, rt.begin(AXE, Mode.FULL, 2, List.of(), 0, ItemScanRuntime.LAYER_PM2)));
        assertEquals(Set.of(new Pos(100, 100, -2), new Pos(100, 100, -1), new Pos(100, 100, 0),
                new Pos(100, 100, 1), new Pos(100, 100, 2)), got.keySet());
    }

    @Test void climbingPrunesTheLayerThatLeftThePresetWindow() {
        var w = new ItemScanFakeWorld();
        w.columns.put(ItemScanCoverage.columnKey(12, 12), new Column(ItemScanCoverage.mask(-3, 6), 1));
        w.putSquare(100, 100, -1).floor.add(floor(100, 100, -1, item("Base.Axe")));
        w.putSquare(100, 100, 1).floor.add(floor(100, 100, 1, item("Base.Axe")));
        var rt = runtime(w);
        long id = rt.runtime.begin(AXE, Mode.FULL, 2, List.of(), 0, ItemScanRuntime.LAYER_PM2);
        assertEquals(2, hits(finish(rt.runtime, id)).size());
        w.player.z = 3;
        long next = rt.runtime.begin(AXE, Mode.DIRTY, 2, List.of(), 0, ItemScanRuntime.LAYER_PM2);
        var got = hits(finish(rt.runtime, next));
        assertEquals(Set.of(new Pos(100, 100, 1)), got.keySet());
    }

    private static final class Harness { final ItemScanRuntime runtime; Harness(ItemScanRuntime r) { runtime = r; } }
    private static Harness runtime(ItemScanFakeWorld w) {
        return new Harness(new ItemScanRuntime(w, new ItemScanRuntimeTest.Clock(), () -> new ItemScanRuntime.ResultSink() {
            final List<Hit> rows = new ArrayList<>();
            public void append(Hit h) { rows.add(h); }
            public Object seal() { return rows; }
        }));
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
