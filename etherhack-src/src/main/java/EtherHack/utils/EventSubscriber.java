/*
 * Decompiled with CFR 0.152.
 */
package EtherHack.utils;

import EtherHack.annotations.SubscribeLuaEvent;
import EtherHack.utils.Logger;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventSubscriber {
    private static Map<String, List<AbstractMap.SimpleEntry<Object, Method>>> subscribers = new HashMap<String, List<AbstractMap.SimpleEntry<Object, Method>>>();

    public static void register(Object handler) {
        Logger.printLog("Registering a class object and subscribing to Lua events: " + String.valueOf(handler));
        for (Method method : handler.getClass().getMethods()) {
            for (SubscribeLuaEvent annotation : (SubscribeLuaEvent[])method.getAnnotationsByType(SubscribeLuaEvent.class)) {
                String eventName = annotation.eventName();
                subscribers.computeIfAbsent(eventName, k -> new ArrayList()).add(new AbstractMap.SimpleEntry<Object, Method>(handler, method));
            }
        }
    }

    public static void invokeSubscriber(String eventName) {
        List<AbstractMap.SimpleEntry<Object, Method>> handlers = subscribers.get(eventName);
        if (handlers != null) {
            for (AbstractMap.SimpleEntry<Object, Method> entry : handlers) {
                try {
                    entry.getValue().invoke(entry.getKey(), new Object[0]);
                }
                catch (Exception e) {
                    Logger.printLog(String.format("Exception when calling method '%s' for event '%s': %s", entry.getValue().getName(), eventName, e.getMessage()));
                }
            }
        }
    }
}
