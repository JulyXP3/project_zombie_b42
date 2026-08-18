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
        // 用 RunLua 直接加载 fixes 模块: require 走 PZ vanilla loader, 只认 media/lua + mods,
        // 找不到游戏根目录 EtherHack/lua 下的文件(否则 ServerSyncBlocker 全局表缺失 -> 开关报 not loaded)
        LuaManager.RunLua((String)"EtherHack/lua/fixes/ISChatFix.lua", (boolean)false);
        LuaManager.RunLua((String)"EtherHack/lua/fixes/ServerSyncBlocker.lua", (boolean)false);
        LuaManager.RunLua((String)"EtherHack/lua/EtherHackMenu.lua", (boolean)false);
    }
}
