package modcore.core;

/** Pure preset, eligibility and vector arithmetic shared by the movement runtime. */
public final class MovementSpeedPolicy {
    private MovementSpeedPolicy() { }

    public static int normalizePreset(int value) {
        return value == 2 || value == 3 ? value : 1;
    }

    public static float effectiveMultiplier(int preset, boolean currentAlive, boolean knownGround,
            boolean manualMove, boolean specialMotion, boolean aimOrPath, boolean extraMovement) {
        if (!currentAlive || !knownGround || !manualMove || specialMotion || aimOrPath || extraMovement) return 1f;
        return normalizePreset(preset);
    }

    public static boolean canScale(float x, float y, float factor) {
        return (factor == 1f || factor == 2f || factor == 3f)
                && Float.isFinite(x) && Float.isFinite(y)
                && Float.isFinite(x * factor) && Float.isFinite(y * factor);
    }

    public static float scaledComponent(float value, float factor) {
        return value * factor;
    }
}
