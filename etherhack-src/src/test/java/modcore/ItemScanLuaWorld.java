package modcore;

import modcore.radar.ItemScanBridge;
import modcore.radar.ItemScanCoverage;
import modcore.radar.ItemScanCoverage.Column;
import modcore.radar.ItemScanWorld;
import modcore.radar.ItemScanWindow;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import java.util.AbstractList;
import java.util.List;

/** Test-only game boundary; all scan/bridge/Lua-service algorithms remain the production implementations. */
final class ItemScanLuaWorld implements ItemScanWorld {
    private final LuaTestRuntime lua;
    private ItemScanLuaWorld(LuaTestRuntime lua){this.lua=lua;}
    static void install(LuaTestRuntime lua){
        var source=new ItemScanLuaWorld(lua);
        var bridge=new ItemScanBridge(source,()->((Number)source.global("getTimestampMs")).longValue()*1_000_000L,()->J2SEPlatform.getInstance().newTable());
        lua.environment.rawset("itemScanBegin",(JavaFunction)(f,n)->f.push(bridge.begin((KahluaTable)f.get(0),(String)f.get(1),((Number)f.get(2)).intValue(),(KahluaTable)f.get(3),((Number)f.get(4)).intValue(),n>5?((Number)f.get(5)).intValue():0)));
        lua.environment.rawset("itemScanStep",(JavaFunction)(f,n)->f.push(bridge.step(((Number)f.get(0)).doubleValue(),((Number)f.get(1)).doubleValue())));
        lua.environment.rawset("itemScanCancel",(JavaFunction)(f,n)->{bridge.cancel();return 0;});
    }
    private Object global(String name,Object... args){return lua.call(lua.environment.rawget(name),args);}
    private Object invoke(Object object,String name,Object... args){
        if(object==null)return null;Object[] values=new Object[args.length+1];values[0]=object;System.arraycopy(args,0,values,1,args.length);
        return lua.call(((KahluaTable)object).rawget(name),values);
    }
    private List<?> view(Object list){
        if(list==null)return List.of();
        return new AbstractList<Object>(){
            public int size(){return ((Number)invoke(list,"size")).intValue();}
            public Object get(int index){return invoke(list,"get",(double)index);}
        };
    }
    public Object player(){return global("getPlayer");} public Object cell(){return global("getCell");}
    public double x(Object o){return ((Number)invoke(o,"getX")).doubleValue();}
    public double y(Object o){return ((Number)invoke(o,"getY")).doubleValue();}
    public double z(Object o){return ((Number)invoke(o,"getZ")).doubleValue();}
    public Column probeColumn(int x,int y){
        Object raw=lua.environment.rawget("scanLayers");KahluaTable row=raw instanceof KahluaTable t?(KahluaTable)t.rawget(x+","+y):null;
        if(row==null)return new Column(ItemScanCoverage.mask(-1,1),1);
        long revision=row.rawget("version") instanceof Number n?n.longValue():1;
        if(Boolean.FALSE.equals(row.rawget("loaded")))return new Column(0,revision);
        int min=row.rawget("min") instanceof Number n?n.intValue():-1,max=row.rawget("max") instanceof Number n?n.intValue():1;
        return new Column(ItemScanCoverage.mask(min,max),revision);
    }
    public Object square(int x,int y,int z){return invoke(cell(),"getGridSquare",(double)x,(double)y,(double)z);}
    public List<?> objects(Object sq){return view(invoke(sq,"getObjects"));} public List<?> floor(Object sq){return view(invoke(sq,"getWorldObjects"));}
    public List<?> moving(Object sq){return view(invoke(sq,"getStaticMovingObjects"));}
    public boolean isCorpse(Object o){return Boolean.TRUE.equals(global("instanceof",o,"IsoDeadBody"));}
    public Object corpseContainer(Object o){return invoke(o,"getContainer");}
    public int containerCount(Object o){return ((Number)invoke(o,"getContainerCount")).intValue();}
    public Object container(Object o,int i){return invoke(o,"getContainerByIndex",(double)i);}
    public Object floorItem(Object o){return invoke(o,"getItem");} public List<?> items(Object c){return view(invoke(c,"getItems"));}
    public String fullType(Object i){return (String)invoke(i,"getFullType");} public String shortType(Object i){return (String)invoke(i,"getType");}
    public String name(Object i){return (String)invoke(i,"getDisplayName");} public boolean isBag(Object i){return Boolean.TRUE.equals(invoke(i,"IsInventoryContainer"));}
    public Object bag(Object i){return invoke(i,"getInventory");} public List<?> vehicles(){return view(invoke(cell(),"getVehicles"));}
    public int partCount(Object v){return ((Number)invoke(v,"getPartCount")).intValue();} public Object part(Object v,int i){return invoke(v,"getPartByIndex",(double)i);}
    public Object partContainer(Object p){return invoke(p,"getItemContainer");}
    public void retainWindow(ItemScanWindow window){} public void clear(){}
}
