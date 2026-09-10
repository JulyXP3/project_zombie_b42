/*
 * Decompiled with CFR 0.152.
 */
package modcore.annotations;

import modcore.annotations.LuaEvents;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value=RetentionPolicy.RUNTIME)
@Target(value={ElementType.METHOD})
@Repeatable(value=LuaEvents.class)
public @interface SubscribeLuaEvent {
    public String eventName();
}
