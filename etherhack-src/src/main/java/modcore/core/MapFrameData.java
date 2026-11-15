package modcore.core;

import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoCell;
import zombie.iso.IsoWorld;

/** One UI-frame snapshot with scalar rows only; world objects never enter the Lua buffer. */
public final class MapFrameData {
    private MapFrameData() {}

    interface Source {
        boolean ready();
        void zombies(Writer out);
        void vehicles(Writer out);
        void players(Writer out);
        void localPlayer(Writer out);
    }

    static final class Writer {
        private final KahluaTable buffer;
        private final KahluaTable zombies;
        private final KahluaTable vehicles;
        private final KahluaTable players;
        private int zi = 1;
        private int vi = 1;
        private int pi = 1;

        Writer(KahluaTable buffer) {
            this.buffer = buffer;
            zombies = lane(buffer, "zombies");
            vehicles = lane(buffer, "vehicles");
            players = lane(buffer, "players");
            clear();
        }

        void zombie(double x, double y) {
            if (!finite(x, y)) return;
            zombies.rawset(zi++, Double.valueOf(x));
            zombies.rawset(zi++, Double.valueOf(y));
        }

        void vehicle(double x, double y, String name) {
            if (!finite(x, y)) return;
            vehicles.rawset(vi++, Double.valueOf(x));
            vehicles.rawset(vi++, Double.valueOf(y));
            vehicles.rawset(vi++, name == null ? "" : name);
        }

        void player(double x, double y, String name, String id, boolean invisible) {
            if (!finite(x, y) || id == null) return;
            players.rawset(pi++, Double.valueOf(x));
            players.rawset(pi++, Double.valueOf(y));
            players.rawset(pi++, name == null ? "" : name);
            players.rawset(pi++, id);
            players.rawset(pi++, Boolean.valueOf(invisible));
        }

        void localPlayer(double x, double y, String name) {
            if (!finite(x, y)) return;
            buffer.rawset("localX", x);
            buffer.rawset("localY", y);
            buffer.rawset("localName", name == null ? "" : name);
        }

        void clear() {
            zombies.wipe();
            vehicles.wipe();
            players.wipe();
            buffer.rawset("localX", null);
            buffer.rawset("localY", null);
            buffer.rawset("localName", null);
        }

        private static boolean finite(double x, double y) {
            return Double.isFinite(x) && Double.isFinite(y);
        }

        private static KahluaTable lane(KahluaTable buffer, String key) {
            Object value = buffer.rawget(key);
            if (value instanceof KahluaTable table) return table;
            KahluaTable table = J2SEPlatform.getInstance().newTable();
            buffer.rawset(key, table);
            return table;
        }
    }

    public static boolean collect(KahluaTable buffer, int flags) {
        return collect(buffer, flags, LiveSource.INSTANCE);
    }

    static boolean collect(KahluaTable buffer, int flags, Source source) {
        if (buffer == null || source == null) return false;
        Writer out = new Writer(buffer);
        try {
            if (!source.ready()) return false;
            if ((flags & 1) != 0) source.zombies(out);
            if ((flags & 2) != 0) source.vehicles(out);
            if ((flags & 4) != 0) source.players(out);
            if ((flags & 8) != 0) source.localPlayer(out);
            return true;
        } catch (RuntimeException e) {
            out.clear();
            return false;
        }
    }

    private enum LiveSource implements Source {
        INSTANCE;

        public boolean ready() {
            return IsoWorld.instance != null && IsoWorld.instance.getCell() != null
                    && IsoPlayer.getInstance() != null;
        }

        private IsoCell cell() {
            return IsoWorld.instance.getCell();
        }

        public void zombies(Writer out) {
            var zombies = cell().getZombieList();
            for (int i = 0, n = zombies.size(); i < n; i++) {
                var zombie = zombies.get(i);
                if (zombie != null) out.zombie(zombie.getX(), zombie.getY());
            }
        }

        public void vehicles(Writer out) {
            for (var vehicle : cell().getVehicles()) {
                if (vehicle != null) out.vehicle(vehicle.getX(), vehicle.getY(), vehicle.getScriptName());
            }
        }

        public void players(Writer out) {
            IsoPlayer local = IsoPlayer.getInstance();
            var players = LuaManager.GlobalObject.getOnlinePlayers();
            if (players == null) return;
            for (int i = 0, n = players.size(); i < n; i++) {
                var player = players.get(i);
                if (player != null && player != local) {
                    out.player(player.getX(), player.getY(), player.getUsername(),
                            Short.toString(player.getOnlineID()), player.isInvisible());
                }
            }
        }

        public void localPlayer(Writer out) {
            IsoPlayer player = IsoPlayer.getInstance();
            out.localPlayer(player.getX(), player.getY(), player.getUsername());
        }
    }
}
