package modcore.core;

/** 载具 ESP 附加字段格式化: 缺失或非法值显示为未知, 不伪造 0。 */
public final class VehicleEspStats {
    private VehicleEspStats() {
    }

    /**
     * @param fuelPercent  剩余燃油百分比; null/NaN/Inf = 未知
     * @param batteryUses  电瓶当前电量; 与 max 同时有效才算已知
     * @param batteryMax   电瓶最大电量; <=0 = 未知
     * @param engineCondition 引擎耐久; 负数 = 未知
     * @return 形如 "油 62% · 电 80% · 引擎 47"; 三项都未知返回 null
     */
    public static String buildLine(Float fuelPercent, Integer batteryUses, Integer batteryMax,
                                   Integer engineCondition,
                                   String fuelLabel, String batteryLabel, String engineLabel) {
        String fuel = percentPart(fuelPercent, fuelLabel);
        String battery = batteryPart(batteryUses, batteryMax, batteryLabel);
        String engine = conditionPart(engineCondition, engineLabel);
        StringBuilder line = new StringBuilder();
        for (String part : new String[]{fuel, battery, engine}) {
            if (part == null) continue;
            if (line.length() > 0) line.append(" · ");
            line.append(part);
        }
        return line.length() == 0 ? null : line.toString();
    }

    private static String percentPart(Float value, String label) {
        if (value == null || !Float.isFinite(value)) return null;
        return label + " " + Math.round(clamp(value, 0.0f, 100.0f)) + "%";
    }

    private static String batteryPart(Integer uses, Integer max, String label) {
        if (uses == null || max == null || max <= 0) return null;
        float pct = clamp(uses, 0, max) * 100.0f / max;
        return label + " " + Math.round(pct) + "%";
    }

    private static String conditionPart(Integer condition, String label) {
        if (condition == null || condition < 0) return null;
        return label + " " + Math.round(clamp(condition, 0, 100));
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }
}
