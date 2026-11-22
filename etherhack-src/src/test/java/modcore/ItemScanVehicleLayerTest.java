package modcore;

import modcore.radar.ItemScanCoverage;
import modcore.radar.ItemScanCoverage.Column;
import modcore.radar.ItemScanIndex.Hit;
import modcore.radar.ItemScanRuntime;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static modcore.ItemScanFakeWorld.*;
import static org.junit.jupiter.api.Assertions.*;

/** Vehicles follow the selected layer preset, same as squares. */
class ItemScanVehicleLayerTest {
    private static final Set<String> AXE = Set.of("Base.Axe");
    private static final long BUDGET = 3_000_000L;
    private static final long OTHER_FLOOR = 6L;

    private static Map<modcore.radar.ItemScanCoverage.Pos, Hit> scan(int layerMode) {
        var w = new ItemScanFakeWorld();
        w.player.z = 0;
        w.columns.put(ItemScanCoverage.columnKey(12, 12), new Column(ItemScanCoverage.mask(-5, 10), 1));
        w.vehicles.add(vehicle(100, 100, 0, item("Base.Axe")));
        w.vehicles.add(vehicle(101, 100, OTHER_FLOOR, item("Base.Axe")));
        var rt = new ItemScanRuntime(w, new ItemScanRuntimeTest.Clock(), () -> new ItemScanRuntime.ResultSink() {
            final List<Hit> rows = new ArrayList<>();
            public void append(Hit h) { rows.add(h); }
            public Object seal() { return rows; }
        });
        long id = rt.begin(AXE, ItemScanRuntime.Mode.FULL, 3, List.of(), 0, layerMode);
        var s = rt.advance(id, BUDGET);
        for (int i = 0; i < 20000 && s.state() == ItemScanRuntime.State.RUNNING; i++) s = rt.advance(id, BUDGET);
        assertEquals(ItemScanRuntime.State.READY, s.state(), s.error());
        @SuppressWarnings("unchecked") var rows = (List<Hit>) s.payload();
        var out = new HashMap<modcore.radar.ItemScanCoverage.Pos, Hit>();
        for (var h : rows) out.put(h.position(), h);
        return out;
    }

    @Test void currentFloorPresetExcludesVehiclesOnOtherFloors() {
        var got = scan(ItemScanRuntime.LAYER_CURRENT);
        assertEquals(Set.of(new modcore.radar.ItemScanCoverage.Pos(100, 100, 0)), got.keySet());
    }

    @Test void plusMinusTwoPresetKeepsNearbyFloorsButNotFarOnes() {
        var w = new ItemScanFakeWorld();
        w.player.z = 0;
        w.columns.put(ItemScanCoverage.columnKey(12, 12), new Column(ItemScanCoverage.mask(-5, 10), 1));
        w.vehicles.add(vehicle(100, 100, 0, item("Base.Axe")));
        w.vehicles.add(vehicle(101, 100, 2, item("Base.Axe")));
        w.vehicles.add(vehicle(102, 100, OTHER_FLOOR, item("Base.Axe")));
        var rt = new ItemScanRuntime(w, new ItemScanRuntimeTest.Clock(), () -> new ItemScanRuntime.ResultSink() {
            final List<Hit> rows = new ArrayList<>();
            public void append(Hit h) { rows.add(h); }
            public Object seal() { return rows; }
        });
        long id = rt.begin(AXE, ItemScanRuntime.Mode.FULL, 3, List.of(), 0, ItemScanRuntime.LAYER_PM2);
        var s = rt.advance(id, BUDGET);
        for (int i = 0; i < 20000 && s.state() == ItemScanRuntime.State.RUNNING; i++) s = rt.advance(id, BUDGET);
        @SuppressWarnings("unchecked") var rows = (List<Hit>) s.payload();
        var zs = new java.util.TreeSet<Long>();
        for (var h : rows) zs.add((long) h.position().z());
        assertEquals(new java.util.TreeSet<>(List.of(0L, 2L)), zs);
    }

    @Test void allFloorsPresetStillFindsEveryVehicle() {
        var got = scan(ItemScanRuntime.LAYER_ALL);
        assertEquals(2, got.size());
        assertTrue(got.containsKey(new modcore.radar.ItemScanCoverage.Pos(101, 100, (int) OTHER_FLOOR)));
    }
}
