/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Core
 *  zombie.ui.UIFont
 */
package EtherHack.Ether;

import EtherHack.Ether.EtherMain;
import EtherHack.annotations.SubscribeLuaEvent;
import EtherHack.utils.EventSubscriber;
import EtherHack.utils.Info;
import EtherHack.utils.Rendering;
import zombie.core.Core;
import zombie.ui.UIFont;

public class EtherCredits {
    public EtherCredits() {
        EventSubscriber.register(this);
    }

    @SubscribeLuaEvent(eventName="OnPreUIDraw")
    public void draw() {
        if (EtherMain.getInstance().etherAPI.isVisualDrawCredits) {
            Core var1 = Core.getInstance();
            float var2 = var1.getScreenHeight();
            Rendering.drawText(Info.CHEAT_CREDITS_TITLE, UIFont.Small, 15.0f, var2 - 30.0f, 255.0f, 255.0f, 255.0f, 255.0f);
            Rendering.drawText("Author: dei0", UIFont.Small, 15.0f, var2 - 15.0f, 255.0f, 255.0f, 255.0f, 255.0f);
        }
    }
}
