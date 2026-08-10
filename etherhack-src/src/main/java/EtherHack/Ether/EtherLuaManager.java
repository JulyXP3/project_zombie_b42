/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.Lua.LuaManager
 */
package EtherHack.Ether;

import EtherHack.Ether.EtherLuaCompiler;
import EtherHack.annotations.LuaEvents;
import EtherHack.annotations.SubscribeLuaEvent;
import EtherHack.utils.EventSubscriber;
import EtherHack.utils.Logger;
import java.util.ArrayList;
import zombie.Lua.LuaManager;

public class EtherLuaManager {
    public final String pathToLuaMainFile = "EtherHack/lua/EtherHackMenu.lua";
    public ArrayList<String> luaFilesList = new ArrayList();

    public EtherLuaManager() {
        EventSubscriber.register(this);
    }

    @LuaEvents(value={@SubscribeLuaEvent(eventName="OnResetLua"), @SubscribeLuaEvent(eventName="OnMainMenuEnter")})
    public void loadLua() {
        Logger.printLog("Loading DeiClient Lua...");
        EtherLuaCompiler.getInstance().addWordToBlacklistLuaCompiler("EtherMain");
        EtherLuaCompiler.getInstance().addPathToWhiteListLuaCompiler("EtherHack/lua/EtherHackMenu.lua");
        LuaManager.RunLua((String)"EtherHack/lua/EtherHackMenu.lua", (boolean)false);
    }
}
