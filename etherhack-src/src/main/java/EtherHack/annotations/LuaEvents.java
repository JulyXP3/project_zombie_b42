/*
 * Decompiled with CFR 0.152.
 */
package EtherHack.annotations;

import EtherHack.annotations.SubscribeLuaEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value=RetentionPolicy.RUNTIME)
@Target(value={ElementType.METHOD})
public @interface LuaEvents {
    public SubscribeLuaEvent[] value();
}
