package modcore.core;

/** 传送与重置共用的有界落地观察；不依赖游戏或原生库。 */
public final class VehicleLandingState {

    private static final long HOLD_MS = 200;
    private static final long MAX_GAP_MS = 250;
    private static final long MAX_OBSERVE_MS = 3000;
    private static final float MAX_VERTICAL_SPEED = 0.1f;
    private static final float MAX_HEIGHT_RANGE = MAX_VERTICAL_SPEED * HOLD_MS / 1000.0f;

    private long startedMs;
    private long lastSampleMs = -1;
    private long windowMs = -1;
    private float windowZ;
    private float minZ;
    private float maxZ;

    public void begin(long now) {
        startedMs = now;
        lastSampleMs = -1;
        windowMs = -1;
    }

    public String sample(long now, float z, float verticalSpeed, boolean supported, boolean aligned) {
        if (!Float.isFinite(z) || !Float.isFinite(verticalSpeed)) {
            return waiting(now, "no physics");
        }
        if (!aligned) {
            return waiting(now, "syncing");
        }
        if (!supported) {
            return waiting(now, "unsupported");
        }
        if (Math.abs(verticalSpeed) > MAX_VERTICAL_SPEED) {
            return waiting(now, "falling");
        }
        if (windowMs < 0 || now < lastSampleMs || now - lastSampleMs > MAX_GAP_MS) {
            startWindow(now, z);
        }
        lastSampleMs = now;
        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z);
        long elapsed = now - windowMs;
        float speed = elapsed > 0 ? Math.abs(z - windowZ) * 1000.0f / elapsed : 0.0f;
        if (maxZ - minZ > MAX_HEIGHT_RANGE || speed > MAX_VERTICAL_SPEED) {
            return waiting(now, "falling");
        }
        if (elapsed >= HOLD_MS && now >= startedMs && now - startedMs < MAX_OBSERVE_MS) {
            return "ok";
        }
        return timed(now, "settling");
    }

    public String awaitingSample(long now) {
        return timed(now, "waiting-physics-frame");
    }

    public String waiting(long now, String reason) {
        windowMs = -1;
        lastSampleMs = now;
        return timed(now, reason);
    }

    private void startWindow(long now, float z) {
        windowMs = now;
        windowZ = minZ = maxZ = z;
    }

    private String timed(long now, String reason) {
        if (now < startedMs) {
            return "timeout-clock-reset";
        }
        return now - startedMs >= MAX_OBSERVE_MS ? "timeout-" + reason : reason;
    }
}
