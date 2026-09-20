package modcore.utils;

import se.krka.kahlua.integration.annotations.LuaMethod;

/**
 * A1 (2026-09-13, 见 analysis/DLL分析/A-隐蔽性加固-设计方案(待实施).md):
 * 统一发包限速器 —— 滑动窗口, 3 条独立通道, 语义照 ContainerJobs(sent[3][300], 1.001s 窗口)。
 *
 * 目的: 批量功能(陷阱生成 / 弹药农场 / 重掷 / 未来的整理)在短时间内的发包量可能远超
 * 人手可达的形态, 这是最容易被服务端流量统计识别的特征。限速器把窗口内放行量封顶,
 * 满了就丢帧不排队(宁可慢, 不要突发)。
 *
 * 用法(Lua): {@code if rateLimited("item") then ... send ... end}
 * 通道: "move" / "item" / "action" (未知通道回落到 "action")。
 */
public final class PacketRateLimiter {

    /** 每条通道的滑动窗口容量(条/秒) —— 与同事 DLL 同档(App A1)。 */
    private static final int WINDOW_CAPACITY = 300;
    /** 窗口长度(ns): 1.001s, 与 ContainerJobs 的 1001000000L 一致。 */
    private static final long WINDOW_NANOS = 1001000000L;

    private static final int CH_MOVE = 0;
    private static final int CH_ITEM = 1;
    private static final int CH_ACTION = 2;

    private static final long[][] sent = new long[3][WINDOW_CAPACITY];
    private static final int[] first = new int[3];
    private static final int[] count = new int[3];

    private PacketRateLimiter() {
    }

    private static int channelOf(String name) {
        if ("move".equalsIgnoreCase(name)) {
            return CH_MOVE;
        }
        if ("item".equalsIgnoreCase(name)) {
            return CH_ITEM;
        }
        return CH_ACTION;
    }

    /** 时间环清理: 丢掉窗口外的记录, 返回该通道当前有效条数(< 容量才可放行)。 */
    private static synchronized boolean allows(int ch, long now) {
        while (count[ch] > 0 && now - sent[ch][first[ch]] >= WINDOW_NANOS) {
            first[ch] = (first[ch] + 1) % WINDOW_CAPACITY;
            count[ch]--;
        }
        return count[ch] < WINDOW_CAPACITY;
    }

    private static synchronized void record(int ch, long now) {
        sent[ch][(first[ch] + count[ch]) % WINDOW_CAPACITY] = now;
        count[ch]++;
    }

    /**
     * 申请一个发包额度(放行即记账)。
     *
     * @param channel 通道名("move"/"item"/"action", 其余回落 "action")
     * @return true = 可发; false = 本窗口已满, 调用方应跳过本轮(不要排队重试)
     */
    @LuaMethod(name = "rateLimited", global = true)
    public static boolean request(String channel) {
        int ch = channelOf(channel);
        long now = System.nanoTime();
        if (!allows(ch, now)) {
            return false;
        }
        record(ch, now);
        return true;
    }

    /** 诊断: 当前窗口内已用条数(面板/日志用)。 */
    @LuaMethod(name = "rateLimiterUsed", global = true)
    public static int used(String channel) {
        int ch = channelOf(channel);
        long now = System.nanoTime();
        synchronized (PacketRateLimiter.class) {
            while (count[ch] > 0 && now - sent[ch][first[ch]] >= WINDOW_NANOS) {
                first[ch] = (first[ch] + 1) % WINDOW_CAPACITY;
                count[ch]--;
            }
            return count[ch];
        }
    }

    /**
     * 剩余额度 —— 供"整容器提交"型调用方预判: 一批操作必须完整执行 (部分执行会留下
     * 中间态), 先看 remaining >= 预估包数, 不足则整批跳过留到下一秒。
     */
    @LuaMethod(name = "rateLimiterRemaining", global = true)
    public static int remaining(String channel) {
        return WINDOW_CAPACITY - used(channel);
    }

    /** 清空全部通道(切图/重连时调用)。 */
    @LuaMethod(name = "rateLimiterReset", global = true)
    public static void reset() {
        synchronized (PacketRateLimiter.class) {
            for (int ch = 0; ch < 3; ch++) {
                first[ch] = 0;
                count[ch] = 0;
            }
        }
    }
}
