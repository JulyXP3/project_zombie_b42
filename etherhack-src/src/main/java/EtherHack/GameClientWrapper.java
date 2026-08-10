/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.characters.IsoPlayer
 *  zombie.network.GameClient
 *  zombie.network.ZomboidNetData
 */
package EtherHack;

import EtherHack.utils.FieldCache;
import EtherHack.utils.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.network.GameClient;
import zombie.network.ZomboidNetData;

public class GameClientWrapper {
    private final GameClient gameClient;
    private static Field incomingNetDataField = null;
    private static Field connectionField = null;
    private static String netDataFieldName = null;

    public GameClientWrapper(GameClient client) {
        this.gameClient = client;
        GameClientWrapper.initializeFieldCache();
    }

    public static Object getConnection() {
        try {
            if (connectionField == null) {
                GameClientWrapper.initializeFieldCache();
            }
            if (connectionField == null) {
                return null;
            }
            return FieldCache.getFieldValue(null, connectionField);
        }
        catch (Exception e) {
            return null;
        }
    }

    public static GameClient getInstance() {
        try {
            Field instanceField = FieldCache.getField(GameClient.class, "instance");
            if (instanceField == null) {
                return null;
            }
            return (GameClient)FieldCache.getFieldValue(null, instanceField);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static void initializeFieldCache() {
        if (incomingNetDataField == null) {
            String[] netDataFieldNames;
            for (String fieldName : netDataFieldNames = new String[]{"incomingNetData", "MainLoopNetData", "LoadingMainLoopNetData", "DelayedCoopNetData"}) {
                incomingNetDataField = FieldCache.getField(GameClient.class, fieldName);
                if (incomingNetDataField == null) continue;
                netDataFieldName = fieldName;
                Logger.print("GameClientWrapper using field: " + fieldName);
                break;
            }
            if (incomingNetDataField == null) {
                Logger.warn("Failed to find any network data field in GameClient");
            }
        }
        if (connectionField == null && (connectionField = FieldCache.getField(GameClient.class, "connection")) == null) {
            Logger.warn("Failed to cache GameClient.connection field - may not exist in Build  42");
        }
    }

    public boolean gameLoadingDealWithNetData(ZomboidNetData data) {
        try {
            Method method = FieldCache.getMethod(GameClient.class, "gameLoadingDealWithNetData", ZomboidNetData.class);
            if (method == null) {
                return false;
            }
            Object result = FieldCache.invokeMethod(this.gameClient, method, data);
            return result instanceof Boolean && (Boolean)result != false;
        }
        catch (Exception e) {
            return false;
        }
    }

    public void mainLoopDealWithNetData(ZomboidNetData data) {
        try {
            Method method = FieldCache.getMethod(GameClient.class, "mainLoopDealWithNetData", ZomboidNetData.class);
            if (method != null) {
                FieldCache.invokeMethod(this.gameClient, method, data);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mainLoopHandlePacketInternal(ZomboidNetData data, ByteBuffer buffer) {
        try {
            Method method = FieldCache.getMethod(GameClient.class, "mainLoopHandlePacketInternal", ZomboidNetData.class, ByteBuffer.class);
            if (method != null) {
                FieldCache.invokeMethod(this.gameClient, method, data, buffer);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delayPacket(int x, int y, int z) {
        try {
            Method method = FieldCache.getMethod(GameClient.class, "delayPacket", Integer.TYPE, Integer.TYPE, Integer.TYPE);
            if (method != null) {
                FieldCache.invokeMethod(this.gameClient, method, x, y, z);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IsoPlayer getPlayerFromUsername(String username) {
        try {
            Method method = FieldCache.getMethod(GameClient.class, "getPlayerFromUsername", String.class);
            if (method == null) {
                return null;
            }
            Object result = FieldCache.invokeMethod(this.gameClient, method, username);
            return result instanceof IsoPlayer ? (IsoPlayer)result : null;
        }
        catch (Exception e) {
            return null;
        }
    }

    public ArrayList<ZomboidNetData> getIncomingNetData() {
        try {
            if (incomingNetDataField == null) {
                GameClientWrapper.initializeFieldCache();
            }
            if (incomingNetDataField == null) {
                return new ArrayList<ZomboidNetData>();
            }
            GameClient instance = GameClientWrapper.getInstance();
            if (instance == null) {
                return new ArrayList<ZomboidNetData>();
            }
            Object value = FieldCache.getFieldValue(instance, incomingNetDataField);
            if (value instanceof ArrayList) {
                return (ArrayList)value;
            }
            return new ArrayList<ZomboidNetData>();
        }
        catch (Exception e) {
            Logger.printLog("Failed to access incomingNetData: " + e.getMessage());
            return new ArrayList<ZomboidNetData>();
        }
    }

    public void clearIncomingNetData() {
        ArrayList<ZomboidNetData> netData = this.getIncomingNetData();
        if (netData != null) {
            netData.clear();
        }
    }

    public static GameClientWrapper get() {
        GameClient instance = GameClientWrapper.getInstance();
        if (instance == null) {
            return null;
        }
        return new GameClientWrapper(instance);
    }
}
