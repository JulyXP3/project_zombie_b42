package modcore.core;

import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/** Shared material defaults, with deferred initialization for confirmed generated bundles. */
public final class GeneratedBundleRopes {
    public static final GeneratedBundleRopes INSTANCE = new GeneratedBundleRopes();
    private static final int MAX_PENDING = 128;
    private final LinkedHashMap<Integer, String> pending = new LinkedHashMap<>();
    private Object connection, owner;

    public synchronized void remember(Object connection, Object owner, int id, String type) {
        if (!bind(connection, owner) || id == -1 || ropeCount(type) == 0) return;
        pending.put(id, type);
        if (pending.size() > MAX_PENDING) pending.remove(pending.keySet().iterator().next());
    }

    /** Lookup must resolve the expected type in the current player's inventory, not the ground. */
    public synchronized int synchronize(Object connection, Object owner,
            BiFunction<Integer, String, KahluaTable> lookup, BiConsumer<Integer, KahluaTable> send) {
        if (!bind(connection, owner) || pending.isEmpty()) return 0;
        int synchronizedItems = 0;
        for (var entry : new ArrayList<>(pending.entrySet())) {
            if (this.connection != connection || this.owner != owner) break;
            KahluaTable data = lookup.apply(entry.getKey(), entry.getValue());
            if (data == null) continue;
            Object original = data.rawget("ropeItems");
            if (original instanceof KahluaTable existing) {
                if (!existing.isEmpty()) { pending.remove(entry.getKey()); continue; }
            } else if (original != null) continue;
            KahluaTable ropes = defaultRopeItems(entry.getValue());
            data.rawset("ropeItems", ropes);
            try {
                send.accept(entry.getKey(), data);
                pending.remove(entry.getKey());
                synchronizedItems++;
            } catch (RuntimeException failed) {
                data.rawset("ropeItems", original);
            }
        }
        return synchronizedItems;
    }

    /** Pure material defaults; unbox may use these without any generation-origin record. */
    public static KahluaTable defaultRopeItems(String type) {
        int count = ropeCount(type);
        if (count == 0) return null;
        KahluaTable result = J2SEPlatform.getInstance().newTable();
        for (int i = 0; i < count; i++) result.rawset(i, "Base.Rope");
        return result;
    }

    public synchronized void clear() { pending.clear(); connection = null; owner = null; }

    private boolean bind(Object connection, Object owner) {
        if (connection == null || owner == null) { clear(); return false; }
        if (this.connection != connection || this.owner != owner) {
            pending.clear(); this.connection = connection; this.owner = owner;
        }
        return true;
    }

    private static int ropeCount(String type) {
        if (type == null) return 0;
        return switch (type) {
            case "Base.LogStacks2", "Base.LogStacks3", "Base.LogStacks4" -> 2;
            case "Base.FirewoodBundle" -> 1;
            default -> 0;
        };
    }
}
