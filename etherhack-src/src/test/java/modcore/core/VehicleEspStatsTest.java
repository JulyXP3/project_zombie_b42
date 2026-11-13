package modcore.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VehicleEspStatsTest {
    private static final String FUEL = "油";
    private static final String BATT = "电";
    private static final String ENG = "引擎";

    @Test
    void allThreeValuesAreJoinedOnOneLine() {
        assertEquals("油 62% · 电 80% · 引擎 47",
                VehicleEspStats.buildLine(62.4f, 800, 1000, 47, FUEL, BATT, ENG));
    }

    @Test
    void missingPartsAreOmittedInsteadOfShowingZero() {
        assertEquals("油 62%", VehicleEspStats.buildLine(62.0f, null, null, null, FUEL, BATT, ENG));
        assertEquals("电 80% · 引擎 47", VehicleEspStats.buildLine(null, 80, 100, 47, FUEL, BATT, ENG));
    }

    @Test
    void allUnknownProducesNoLine() {
        assertNull(VehicleEspStats.buildLine(null, null, null, null, FUEL, BATT, ENG));
    }

    @Test
    void nonFiniteFuelIsTreatedAsUnknown() {
        assertNull(VehicleEspStats.buildLine(Float.NaN, null, null, null, FUEL, BATT, ENG));
        assertEquals("引擎 47",
                VehicleEspStats.buildLine(Float.POSITIVE_INFINITY, null, null, 47, FUEL, BATT, ENG));
    }

    @Test
    void valuesAreClampedAndRounded() {
        assertEquals("油 100% · 电 100% · 引擎 100",
                VehicleEspStats.buildLine(150.0f, 120, 100, 150, FUEL, BATT, ENG));
        assertEquals("油 0% · 电 0% · 引擎 0",
                VehicleEspStats.buildLine(-5.0f, 0, 100, 0, FUEL, BATT, ENG));
        assertEquals("油 48%", VehicleEspStats.buildLine(47.6f, null, null, null, FUEL, BATT, ENG));
    }

    @Test
    void zeroMaxUsesMeansUnknownBattery() {
        assertEquals("引擎 47", VehicleEspStats.buildLine(null, 80, 0, 47, FUEL, BATT, ENG));
        assertEquals("引擎 47", VehicleEspStats.buildLine(null, 80, -1, 47, FUEL, BATT, ENG));
    }

    @Test
    void negativeEngineConditionIsUnknown() {
        assertEquals("油 62%", VehicleEspStats.buildLine(62.0f, null, null, -1, FUEL, BATT, ENG));
    }
}
