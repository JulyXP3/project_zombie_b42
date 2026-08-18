/*
 * Decompiled with CFR 0.152.
 */
package EtherHack.utils;

import EtherHack.utils.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FieldCache {
    private static final Map<String, Field> fieldCache = new HashMap<String, Field>();
    private static final Map<String, Method> methodCache = new HashMap<String, Method>();
    private static final Set<String> warnedFieldKeys = new HashSet<String>();
    private static final Set<String> warnedMethodKeys = new HashSet<String>();

    public static Field getField(Class<?> clazz, String fieldName) {
        String key = clazz.getName() + "." + fieldName;
        return fieldCache.computeIfAbsent(key, k -> {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                Logger.print("[FieldCache] Cached field: " + key);
                return field;
            }
            catch (NoSuchFieldException e) {
                for (Class current = clazz.getSuperclass(); current != null; current = current.getSuperclass()) {
                    try {
                        Field field = current.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Logger.print("[FieldCache] Cached field from superclass: " + key);
                        return field;
                    }
                    catch (NoSuchFieldException ignored) {
                        continue;
                    }
                }
                if (warnedFieldKeys.add(key)) {
                    Logger.warn("[FieldCache] Field not found: " + key);
                }
                return null;
            }
            catch (Exception e) {
                Logger.error("[FieldCache] Error accessing field: " + key, e);
                return null;
            }
        });
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?> ... parameterTypes) {
        String key = clazz.getName() + "." + methodName + "(" + FieldCache.paramTypesToString(parameterTypes) + ")";
        return methodCache.computeIfAbsent(key, k -> {
            try {
                Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                Logger.print("[FieldCache] Cached method: " + key);
                return method;
            }
            catch (NoSuchMethodException e) {
                for (Class current = clazz.getSuperclass(); current != null; current = current.getSuperclass()) {
                    try {
                        Method method = current.getDeclaredMethod(methodName, parameterTypes);
                        method.setAccessible(true);
                        Logger.print("[FieldCache] Cached method from superclass: " + key);
                        return method;
                    }
                    catch (NoSuchMethodException ignored) {
                        continue;
                    }
                }
                if (warnedMethodKeys.add(key)) {
                    Logger.warn("[FieldCache] Method not found: " + key);
                }
                return null;
            }
            catch (Exception e) {
                Logger.error("[FieldCache] Error accessing method: " + key, e);
                return null;
            }
        });
    }

    public static void setFieldValue(Object target, Field field, Object value) {
        if (field == null) {
            Logger.warn("[FieldCache] Cannot set value on null field");
            return;
        }
        try {
            field.set(target, value);
        }
        catch (IllegalAccessException e) {
            Logger.error("[FieldCache] Failed to set field: " + field.getName(), e);
        }
    }

    public static Object getFieldValue(Object target, Field field) {
        if (field == null) {
            Logger.warn("[FieldCache] Cannot get value from null field");
            return null;
        }
        try {
            return field.get(target);
        }
        catch (IllegalAccessException e) {
            Logger.error("[FieldCache] Failed to get field: " + field.getName(), e);
            return null;
        }
    }

    public static Object invokeMethod(Object target, Method method, Object ... args) {
        if (method == null) {
            Logger.warn("[FieldCache] Cannot invoke null method");
            return null;
        }
        try {
            return method.invoke(target, args);
        }
        catch (Exception e) {
            Logger.error("[FieldCache] Failed to invoke method: " + method.getName(), e);
            return null;
        }
    }

    public static void initializeCommonFields() {
        Logger.print("[FieldCache] Initializing common field cache...");
        try {
            Field field;
            try {
                Field field2;
                Class<?> gameClientClass = Class.forName("zombie.network.GameClient");
                String[] netDataFieldNames = new String[]{"incomingNetData", "MainLoopNetData", "LoadingMainLoopNetData", "DelayedCoopNetData"};
                boolean foundNetData = false;
                for (String fieldName : netDataFieldNames) {
                    field = FieldCache.getField(gameClientClass, fieldName);
                    if (field == null) continue;
                    foundNetData = true;
                    Logger.print("[FieldCache] Using GameClient." + fieldName + " field");
                    break;
                }
                if (!foundNetData) {
                    Logger.warn("[FieldCache] GameClient network data field not found - may affect packet filtering");
                }
                if ((field2 = FieldCache.getField(gameClientClass, "connection")) == null) {
                    Logger.warn("[FieldCache] GameClient.connection not found - may have been renamed");
                }
            }
            catch (ClassNotFoundException e) {
                Logger.warn("[FieldCache] GameClient class not found");
            }
            try {
                Class<?> isoPlayerClass = Class.forName("zombie.characters.IsoPlayer");
                String[] cheatsFieldNames = new String[]{"cheats", "playerCheats", "m_cheats"};
                boolean foundCheats = false;
                for (String fieldName : cheatsFieldNames) {
                    field = FieldCache.getField(isoPlayerClass, fieldName);
                    if (field == null) continue;
                    foundCheats = true;
                    Logger.print("[FieldCache] Using IsoPlayer." + fieldName + " field");
                    break;
                }
                if (!foundCheats) {
                    Logger.warn("[FieldCache] IsoPlayer cheats field not found");
                }
                String[] stringArray = new String[]{"stats", "characterStats", "m_stats"};
                boolean foundStats = false;
                for (String fieldName : stringArray) {
                    Field field2 = FieldCache.getField(isoPlayerClass, fieldName);
                    if (field2 == null) continue;
                    foundStats = true;
                    Logger.print("[FieldCache] Using IsoPlayer." + fieldName + " field");
                    break;
                }
                if (!foundStats) {
                    Logger.warn("[FieldCache] IsoPlayer stats field not found");
                }
                String[] traitsFieldNames = new String[]{"traits", "characterTraits", "m_traits"};
                boolean foundTraits = false;
                for (String fieldName : traitsFieldNames) {
                    Field field3 = FieldCache.getField(isoPlayerClass, fieldName);
                    if (field3 == null) continue;
                    foundTraits = true;
                    Logger.print("[FieldCache] Using IsoPlayer." + fieldName + " field");
                    break;
                }
                if (!foundTraits) {
                    Logger.warn("[FieldCache] IsoPlayer traits field not found");
                }
            }
            catch (ClassNotFoundException e) {
                Logger.warn("[FieldCache] IsoPlayer class not found");
            }
            try {
                Class<?> playerCheatsClass = Class.forName("zombie.characters.PlayerCheats");
                try {
                    FieldCache.getField(playerCheatsClass, "cheats");
                }
                catch (Exception e) {
                    Logger.warn("[FieldCache] PlayerCheats.cheats not found - may have been renamed");
                }
            }
            catch (ClassNotFoundException e) {
                Logger.warn("[FieldCache] PlayerCheats class not found");
            }
            Logger.print("[FieldCache] Common fields initialization completed");
        }
        catch (Exception e) {
            Logger.error("[FieldCache] Failed to initialize common fields", e);
        }
    }

    public static void clearCache() {
        fieldCache.clear();
        methodCache.clear();
        Logger.print("[FieldCache] Cache cleared");
    }

    public static String getCacheStats() {
        return String.format("[FieldCache] Stats: %d fields, %d methods cached", fieldCache.size(), methodCache.size());
    }

    private static String paramTypesToString(Class<?>[] paramTypes) {
        if (paramTypes == null || paramTypes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramTypes.length; ++i) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(paramTypes[i].getSimpleName());
        }
        return sb.toString();
    }
}
