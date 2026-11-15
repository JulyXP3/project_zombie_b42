package modcore.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapSegmentClipTest {
    @Test
    void crossingSegmentClipsToBothViewportEdges() {
        double[] out = new double[4];
        assertTrue(MapSegmentClip.clip(-1000, 50, 1000, 50, 200, 100, out));
        assertArrayEquals(new double[]{0, 50, 200, 50}, out, 0.00001);
    }

    @Test
    void diagonalAndVerticalSegmentsKeepTheirIntersectionPoints() {
        double[] out = new double[4];
        assertTrue(MapSegmentClip.clip(-100, -100, 300, 300, 200, 100, out));
        assertArrayEquals(new double[]{0, 0, 100, 100}, out, 0.00001);
        assertTrue(MapSegmentClip.clip(50, -50, 50, 200, 200, 100, out));
        assertArrayEquals(new double[]{50, 0, 50, 100}, out, 0.00001);
    }

    @Test
    void offscreenAndNonFiniteSegmentsAreRejected() {
        double[] out = new double[4];
        assertFalse(MapSegmentClip.clip(-100, 5, -10, 10, 200, 100, out));
        assertFalse(MapSegmentClip.clip(Double.NaN, 1, 2, 3, 200, 100, out));
        assertFalse(MapSegmentClip.clip(1, 2, Double.POSITIVE_INFINITY, 3, 200, 100, out));
        assertFalse(MapSegmentClip.clip(1, 2, 3, 4, 0, 100, out));
    }
}
