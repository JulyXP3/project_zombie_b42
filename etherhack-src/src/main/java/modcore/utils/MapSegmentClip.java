package modcore.utils;

/** Cohen-Sutherland clipping with a caller-owned result buffer. */
public final class MapSegmentClip {
    private MapSegmentClip() {}

    public static boolean clip(double x1, double y1, double x2, double y2,
                               double width, double height, double[] out) {
        if (!Double.isFinite(x1) || !Double.isFinite(y1) || !Double.isFinite(x2)
                || !Double.isFinite(y2) || !Double.isFinite(width) || !Double.isFinite(height)
                || width <= 0 || height <= 0 || out == null || out.length < 4) return false;
        int c1 = code(x1, y1, width, height);
        int c2 = code(x2, y2, width, height);
        for (int i = 0; i < 8; i++) {
            if ((c1 | c2) == 0) {
                out[0] = x1;
                out[1] = y1;
                out[2] = x2;
                out[3] = y2;
                return true;
            }
            if ((c1 & c2) != 0) return false;
            int c = c1 != 0 ? c1 : c2;
            double x, y;
            if ((c & 8) != 0) {
                x = x1 + (x2 - x1) * (height - y1) / (y2 - y1);
                y = height;
            } else if ((c & 4) != 0) {
                x = x1 + (x2 - x1) * -y1 / (y2 - y1);
                y = 0;
            } else if ((c & 2) != 0) {
                y = y1 + (y2 - y1) * (width - x1) / (x2 - x1);
                x = width;
            } else {
                y = y1 + (y2 - y1) * -x1 / (x2 - x1);
                x = 0;
            }
            if (!Double.isFinite(x) || !Double.isFinite(y)) return false;
            if (c == c1) {
                x1 = x;
                y1 = y;
                c1 = code(x1, y1, width, height);
            } else {
                x2 = x;
                y2 = y;
                c2 = code(x2, y2, width, height);
            }
        }
        return false;
    }

    private static int code(double x, double y, double width, double height) {
        return (x < 0 ? 1 : x > width ? 2 : 0) | (y < 0 ? 4 : y > height ? 8 : 0);
    }
}
