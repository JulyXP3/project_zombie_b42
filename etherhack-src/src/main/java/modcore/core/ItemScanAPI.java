package modcore.core;

import modcore.radar.GameItemScanWorld;
import modcore.radar.ItemScanBridge;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

/** Lua-facing batch entry points; no ASM types or manual environment writes. */
public final class ItemScanAPI {
    private ItemScanAPI() {}
    private static final class Holder {
        static final ItemScanBridge BRIDGE=new ItemScanBridge(new GameItemScanWorld(),System::nanoTime,()->LuaManager.platform.newTable());
    }
    @LuaMethod(name="itemScanBegin",global=true)
    public static double begin(KahluaTable targets,String mode,int radius,KahluaTable dirty,int patrolCells,int layerMode){return Holder.BRIDGE.begin(targets,mode,radius,dirty,patrolCells,layerMode);}
    @LuaMethod(name="itemScanStep",global=true)
    public static KahluaTable step(double ticket,double budgetMs){return Holder.BRIDGE.step(ticket,budgetMs);}
    @LuaMethod(name="itemScanCancel",global=true)
    public static void cancel(){Holder.BRIDGE.cancel();}
}
