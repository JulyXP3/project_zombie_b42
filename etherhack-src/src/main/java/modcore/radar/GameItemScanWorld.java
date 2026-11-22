package modcore.radar;

import modcore.radar.ItemScanColumnStamp.Slot;
import modcore.radar.ItemScanCoverage.Column;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Public getters only. Live objects are borrowed by an in-flight request, never stored in scalar indexes. */
public final class GameItemScanWorld implements ItemScanWorld {
    private final Map<Long,ItemScanColumnStamp> columns=new HashMap<>();
    public Object player(){return LuaManager.GlobalObject.getPlayer();}
    public Object cell(){return LuaManager.GlobalObject.getCell();}
    public double x(Object o){return o instanceof VehiclePart p?p.getX():((IsoObject)o).getX();}
    public double y(Object o){return o instanceof VehiclePart p?p.getY():((IsoObject)o).getY();}
    public double z(Object o){return o instanceof VehiclePart p?p.getZ():((IsoObject)o).getZ();}
    public Column probeColumn(int cx,int cy){
        IsoCell cell=(IsoCell)cell();if(cell==null)return new Column(0,0);
        var slots=new ArrayList<Slot>(IsoPlayer.numPlayers);
        for(int pl=0;pl<IsoPlayer.numPlayers;pl++){
            IsoChunkMap map=cell.getChunkMap(pl);IsoChunk chunk=null;
            if(map!=null&&!map.ignore){chunk=map.getChunk(cx-map.getWorldXMin(),cy-map.getWorldYMin());if(chunk!=null&&!chunk.loaded)chunk=null;}
            if(chunk==null)slots.add(new Slot(null,0,-1,0));
            else slots.add(new Slot(chunk,chunk.loadedFrame,chunk.getLoadID(),ItemScanCoverage.mask(chunk.getMinLevel(),chunk.getMaxLevel())));
        }
        return columns.computeIfAbsent(ItemScanCoverage.columnKey(cx,cy),key->new ItemScanColumnStamp()).observe(slots);
    }
    public Object square(int x,int y,int z){IsoCell c=(IsoCell)cell();return c==null?null:c.getGridSquare(x,y,z);}
    public List<?> objects(Object square){return ((IsoGridSquare)square).getObjects();}
    public List<?> floor(Object square){return ((IsoGridSquare)square).getWorldObjects();}
    public List<?> moving(Object square){return ((IsoGridSquare)square).getStaticMovingObjects();}
    public boolean isCorpse(Object o){return o instanceof IsoDeadBody;}
    public Object corpseContainer(Object o){return ((IsoDeadBody)o).getContainer();}
    public int containerCount(Object o){return ((IsoObject)o).getContainerCount();}
    public Object container(Object o,int index){return ((IsoObject)o).getContainerByIndex(index);}
    public Object floorItem(Object o){return ((IsoWorldInventoryObject)o).getItem();}
    public List<?> items(Object container){return ((ItemContainer)container).getItems();}
    public String fullType(Object item){return ((InventoryItem)item).getFullType();}
    public String shortType(Object item){return ((InventoryItem)item).getType();}
    public String name(Object item){return ((InventoryItem)item).getDisplayName();}
    public boolean isBag(Object item){return ((InventoryItem)item).IsInventoryContainer();}
    public Object bag(Object item){return ((InventoryContainer)item).getInventory();}
    public List<?> vehicles(){IsoCell cell=(IsoCell)cell();return cell==null?List.of():Arrays.asList(cell.getVehicles().toArray());}
    public int partCount(Object v){return ((BaseVehicle)v).getPartCount();}
    public Object part(Object v,int index){return ((BaseVehicle)v).getPartByIndex(index);}
    public Object partContainer(Object p){return ((VehiclePart)p).getItemContainer();}
    public void retainWindow(ItemScanWindow w){
        var it=columns.keySet().iterator();while(it.hasNext()){
            long key=it.next();int x=(int)(key>>32),y=(int)key;
            if(x<w.minChunkX()||x>w.maxChunkX()||y<w.minChunkY()||y>w.maxChunkY())it.remove();
        }
    }
    public void clear(){columns.clear();}
}
