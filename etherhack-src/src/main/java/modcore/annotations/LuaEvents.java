/*
 * Decompiled with CFR 0.152.
 */
package modcore.annotations;

import modcore.annotations.SubscribeLuaEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value=RetentionPolicy.RUNTIME)
@Target(value={ElementType.METHOD})
public @interface LuaEvents {
    public SubscribeLuaEvent[] value();
}
