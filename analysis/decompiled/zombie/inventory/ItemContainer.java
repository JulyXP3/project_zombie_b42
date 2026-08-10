/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  se.krka.kahlua.vm.LuaClosure
 *  zombie.GameTime
 *  zombie.GameWindow
 *  zombie.Lua.LuaManager
 *  zombie.SandboxOptions
 *  zombie.SystemDisabler
 *  zombie.UsedFromLua
 *  zombie.ZomboidGlobals
 *  zombie.characters.IsoGameCharacter
 *  zombie.characters.IsoPlayer
 *  zombie.characters.SurvivorDesc
 *  zombie.characters.animals.IsoAnimal
 *  zombie.core.Core
 *  zombie.core.properties.IsoPropertyType
 *  zombie.core.random.Rand
 *  zombie.debug.DebugLog
 *  zombie.debug.DebugType
 *  zombie.entity.ComponentType
 *  zombie.entity.components.fluids.Fluid
 *  zombie.entity.components.fluids.FluidContainer
 *  zombie.inventory.CompressIdenticalItems
 *  zombie.inventory.InventoryItemFactory
 *  zombie.inventory.ItemContainer$CategoryPredicate
 *  zombie.inventory.ItemContainer$Comparators
 *  zombie.inventory.ItemContainer$ConditionComparator
 *  zombie.inventory.ItemContainer$EvalArgComparator
 *  zombie.inventory.ItemContainer$EvalArgPredicate
 *  zombie.inventory.ItemContainer$EvalComparator
 *  zombie.inventory.ItemContainer$EvalPredicate
 *  zombie.inventory.ItemContainer$InventoryItemList
 *  zombie.inventory.ItemContainer$InventoryItemListPool
 *  zombie.inventory.ItemContainer$Predicates
 *  zombie.inventory.ItemContainer$TagEvalArgPredicate
 *  zombie.inventory.ItemContainer$TagEvalPredicate
 *  zombie.inventory.ItemContainer$TagPredicate
 *  zombie.inventory.ItemContainer$TypeEvalArgPredicate
 *  zombie.inventory.ItemContainer$TypeEvalPredicate
 *  zombie.inventory.ItemContainer$TypePredicate
 *  zombie.inventory.ItemPickerJava
 *  zombie.inventory.ItemPickerJava$ItemPickerContainer
 *  zombie.inventory.ItemPickerJava$ItemPickerRoom
 *  zombie.inventory.ItemSoundManager
 *  zombie.inventory.types.AlarmClock
 *  zombie.inventory.types.AlarmClockClothing
 *  zombie.inventory.types.AnimalInventoryItem
 *  zombie.inventory.types.Clothing
 *  zombie.inventory.types.Drainable
 *  zombie.inventory.types.DrainableComboItem
 *  zombie.inventory.types.Food
 *  zombie.inventory.types.HandWeapon
 *  zombie.inventory.types.InventoryContainer
 *  zombie.inventory.types.Key
 *  zombie.inventory.types.Literature
 *  zombie.iso.IsoDirections
 *  zombie.iso.IsoWorld
 *  zombie.iso.Vector2
 *  zombie.iso.objects.IsoBarbecue
 *  zombie.iso.objects.IsoCompost
 *  zombie.iso.objects.IsoDeadBody
 *  zombie.iso.objects.IsoFeedingTrough
 *  zombie.iso.objects.IsoFireplace
 *  zombie.iso.objects.IsoLightSwitch
 *  zombie.iso.objects.IsoMannequin
 *  zombie.iso.objects.IsoStove
 *  zombie.network.GameClient
 *  zombie.network.GameServer
 *  zombie.network.PacketTypes$PacketType
 *  zombie.network.packets.INetworkPacket
 *  zombie.scripting.ScriptManager
 *  zombie.scripting.objects.CharacterTrait
 *  zombie.scripting.objects.ContainerType
 *  zombie.scripting.objects.Item
 *  zombie.scripting.objects.ItemKey
 *  zombie.scripting.objects.ItemKey$Drainable
 *  zombie.scripting.objects.ItemTag
 *  zombie.scripting.objects.ItemType
 *  zombie.util.StringUtils
 *  zombie.util.lambda.Invokers$Params2$Boolean$ICallback
 *  zombie.util.lambda.Invokers$Params2$Boolean$IParam2
 *  zombie.util.list.PZArrayList
 *  zombie.util.list.PZArrayUtil
 *  zombie.vehicles.BaseVehicle
 *  zombie.vehicles.VehicleDoor
 *  zombie.vehicles.VehiclePart
 *  zombie.vehicles.VehiclePartOwner
 */
package zombie.inventory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import se.krka.kahlua.vm.LuaClosure;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.properties.IsoPropertyType;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSoundManager;
import zombie.inventory.types.AlarmClock;
import zombie.inventory.types.AlarmClockClothing;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Drainable;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.inventory.types.Literature;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ContainerType;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.util.StringUtils;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleDoor;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehiclePartOwner;

@UsedFromLua
public final class ItemContainer {
    private static final ArrayList<InventoryItem> tempList = new ArrayList();
    private static final ThreadLocal<ArrayList<IsoObject>> s_tempObjects = ThreadLocal.withInitial(ArrayList::new);
    public boolean active;
    private boolean dirty = true;
    public boolean isdevice;
    public float ageFactor = 1.0f;
    public float cookingFactor = 1.0f;
    public int capacity = 50;
    public InventoryItem containingItem;
    public ArrayList<InventoryItem> items = new ArrayList();
    public ArrayList<InventoryItem> includingObsoleteItems = new ArrayList();
    public IsoObject parent;
    public IsoGridSquare sourceGrid;
    public VehiclePart vehiclePart;
    public InventoryContainer inventoryContainer;
    public boolean explored;
    public String type = "none";
    public int id;
    private boolean drawDirty = true;
    private float customTemperature;
    private boolean hasBeenLooted;
    private String openSound;
    private String closeSound;
    private String putSound;
    private String takeSound;
    private String onlyAcceptCategory;
    private String acceptItemFunction;
    private int weightReduction;
    private String containerPosition;
    private String freezerPosition;
    private static final int MAX_CAPACITY = 100;
    private static final int MAX_CAPACITY_BAG = 50;
    private static final int MAX_CAPACITY_VEHICLE = 1000;
    private static final String TYPE_FRIDGE = "fridge";
    private static final String TYPE_FREEZER = "freezer";
    private static final ThreadLocal<Comparators> TL_comparators = ThreadLocal.withInitial(Comparators::new);
    private static final ThreadLocal<InventoryItemListPool> TL_itemListPool = ThreadLocal.withInitial(InventoryItemListPool::new);
    private static final ThreadLocal<Predicates> TL_predicates = ThreadLocal.withInitial(Predicates::new);

    public ItemContainer(int id, String containerName, IsoGridSquare square, IsoObject parent) {
        this.id = id;
        this.parent = parent;
        this.type = containerName;
        this.sourceGrid = square;
        if (this.isFridge()) {
            this.ageFactor = 0.02f;
            this.cookingFactor = 0.0f;
        }
    }

    public ItemContainer(String containerName, IsoGridSquare square, IsoObject parent) {
        this.id = -1;
        this.parent = parent;
        this.type = containerName;
        this.sourceGrid = square;
        if (this.isFridge()) {
            this.ageFactor = 0.02f;
            this.cookingFactor = 0.0f;
        }
    }

    public ItemContainer(int id) {
        this.id = id;
    }

    public ItemContainer() {
        this.id = -1;
    }

    public static float floatingPointCorrection(float val) {
        int pow = 100;
        float tmp = val * 100.0f;
        return (float)((int)(tmp - (float)((int)tmp) >= 0.5f ? tmp + 1.0f : tmp)) / 100.0f;
    }

    public int getCapacity() {
        int capacity = this.capacity;
        if (this.isOccupiedVehicleSeat()) {
            capacity /= 4;
        }
        if (this.getVehiclePart() != null) {
            return Math.min(capacity, 1000);
        }
        if (this.containingItem != null && this.containingItem instanceof InventoryItem) {
            return Math.min(capacity, 50);
        }
        return Math.min(capacity, 100);
    }

    public void setCapacity(int capacity) {
        if (this.getVehiclePart() != null && this.capacity > 1000) {
            DebugType.General.warn((Object)("Attempting to set capacity of " + String.valueOf(this) + "over maximum capacity of 1000"));
        } else if (this.containingItem != null && this.containingItem instanceof InventoryItem && this.capacity > 50) {
            DebugType.General.warn((Object)("Attempting to set capacity of " + String.valueOf((Object)this.containingItem) + "over maximum capacity of 50"));
        } else if (capacity > 100) {
            DebugType.General.warn((Object)("Attempting to set capacity of " + String.valueOf(this) + "over maximum capacity of 100"));
        }
        this.capacity = capacity;
    }

    public InventoryItem FindAndReturnWaterItem(int uses) {
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryItem item = this.getItems().get(i);
            if (!(item instanceof DrainableComboItem)) continue;
            DrainableComboItem drainableItem = (DrainableComboItem)item;
            if (!item.isWaterSource() || drainableItem.getCurrentUses() < uses) continue;
            return item;
        }
        return null;
    }

    public InventoryItem getItemFromTypeRecurse(String type) {
        return this.getFirstTypeRecurse(type);
    }

    public int getEffectiveCapacity(IsoGameCharacter chr) {
        int capacity = this.getCapacity();
        if (!(chr == null || this.parent instanceof IsoGameCharacter || this.parent instanceof IsoDeadBody || "floor".equals(this.getType()))) {
            if (chr.hasTrait(CharacterTrait.ORGANIZED)) {
                return (int)Math.max((float)capacity * 1.3f, (float)(capacity + 1));
            }
            if (chr.hasTrait(CharacterTrait.DISORGANIZED)) {
                return (int)Math.max((float)capacity * 0.7f, 1.0f);
            }
        }
        return capacity;
    }

    public boolean hasRoomFor(IsoGameCharacter chr, InventoryItem item) {
        if (chr != null && chr.getVehicle() != null && item.hasTag(ItemTag.HEAVY_ITEM) && this.parent instanceof IsoGameCharacter) {
            return false;
        }
        if (!this.isItemAllowed(item)) {
            return false;
        }
        if (this.containingItem != null && this.containingItem.getEquipParent() != null && this.containingItem.getEquipParent().getInventory() != null && !this.containingItem.getEquipParent().getInventory().contains(item)) {
            if (chr != null && ItemContainer.floatingPointCorrection(this.containingItem.getEquipParent().getInventory().getCapacityWeight()) + item.getUnequippedWeight() <= (float)this.containingItem.getEquipParent().getInventory().getEffectiveCapacity(chr)) {
                return this.hasRoomFor(chr, item.getUnequippedWeight());
            }
            return false;
        }
        if (this.hasWorldItem() && item.isOnGroundOnSquare(this.getWorldItem().getSquare())) {
            return this.hasRoomFor(chr, item.getUnequippedWeight(), 0.0f);
        }
        if (this.hasWorldItem() && item.isInsideBagOnSquare(this.getWorldItem().getSquare())) {
            return this.hasRoomFor(chr, item.getUnequippedWeight(), 0.0f);
        }
        return this.hasRoomFor(chr, item.getUnequippedWeight());
    }

    public boolean hasRoomFor(IsoGameCharacter chr, float weightVal) {
        return this.hasRoomFor(chr, weightVal, weightVal);
    }

    public boolean hasRoomFor(IsoGameCharacter chr, float weightVal, float weightAddedToFloor) {
        VehiclePart vehiclePart1;
        IsoGridSquare sq;
        float ground;
        IsoWorldInventoryObject worldItem;
        IsoWorldInventoryObject isoWorldInventoryObject;
        if (this.isVehicleSeat() && this.vehiclePart.getVehicle().getCharacter(this.vehiclePart.getContainerSeatNumber()) == null && this.items.isEmpty() && ItemContainer.floatingPointCorrection(weightVal) <= 50.0f) {
            return true;
        }
        if (this.inventoryContainer != null && this.inventoryContainer.getMaxItemSize() > 0.0f && weightVal > this.inventoryContainer.getMaxItemSize()) {
            return false;
        }
        if (chr != null && !this.isInCharacterInventory(chr) && (isoWorldInventoryObject = this.getWorldItem()) instanceof IsoWorldInventoryObject && (worldItem = isoWorldInventoryObject).getSquare() != null && (ground = (sq = worldItem.getSquare()).getTotalWeightOfItemsOnFloor()) + weightAddedToFloor > 50.0f) {
            return false;
        }
        VehiclePart vehiclePart = vehiclePart1 = this.containingItem != null && this.containingItem.getContainer() != null ? this.containingItem.getContainer().getVehiclePart() : null;
        if (vehiclePart1 != null && ItemContainer.floatingPointCorrection(this.containingItem.getContainer().getCapacityWeight() + weightVal) > (float)this.getContainingItem().getContainer().getEffectiveCapacity(chr)) {
            return false;
        }
        return ItemContainer.floatingPointCorrection(this.getCapacityWeight()) + weightVal <= (float)this.getEffectiveCapacity(chr);
    }

    public float getFreeCapacity(IsoGameCharacter chr) {
        return Math.max((float)this.getEffectiveCapacity(chr) - ItemContainer.floatingPointCorrection(this.getCapacityWeight()), 0.0f);
    }

    public boolean isFull(IsoGameCharacter chr) {
        return this.getFreeCapacity(chr) <= 0.0f;
    }

    private boolean isInvalidClothingRackItem(InventoryItem item) {
        return this.getType().equals("clothingrack") && !(item instanceof Clothing) && !item.hasTag(ItemTag.FITS_CLOTHING_RACK);
    }

    public boolean isItemAllowed(InventoryItem item) {
        Boolean accept;
        Object functionObj;
        if (item == null) {
            return false;
        }
        if (item instanceof AnimalInventoryItem && !"floor".equals(this.type)) {
            return false;
        }
        if (item.getType().contains("Corpse") && this.parent instanceof IsoDeadBody) {
            return false;
        }
        String category = this.getOnlyAcceptCategory();
        if (category != null && !category.equalsIgnoreCase(item.getCategory())) {
            return false;
        }
        String functionName = this.getAcceptItemFunction();
        if (functionName != null && (functionObj = LuaManager.getFunctionObject((String)functionName)) != null && (accept = LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObj, (Object)this, (Object)item)) != Boolean.TRUE) {
            return false;
        }
        if (this.parent != null && !this.parent.isItemAllowedInContainer(this, item)) {
            return false;
        }
        if (this.isInvalidClothingRackItem(item)) {
            return false;
        }
        if (this.getParent() != null && this.getParent().getProperties() != null && this.getParent().getProperties().get(IsoPropertyType.CUSTOM_NAME) != null && this.getParent().getProperties().get(IsoPropertyType.CUSTOM_NAME).equals("Toaster") && !item.hasTag(ItemTag.FITS_TOASTER)) {
            return false;
        }
        if (this.getParent() != null && this.getParent().getProperties() != null && this.getParent().getProperties().get(IsoPropertyType.GROUP_NAME) != null) {
            boolean coffeeMaker;
            boolean bl = coffeeMaker = this.getParent().getProperties().get(IsoPropertyType.GROUP_NAME).equals("Coffee") || this.getParent().getProperties().get(IsoPropertyType.GROUP_NAME).equals("Espresso");
            if (coffeeMaker && !item.hasTag(ItemTag.COFFEE_MAKER)) {
                return false;
            }
        }
        return true;
    }

    public boolean isRemoveItemAllowed(InventoryItem item) {
        if (item == null) {
            return false;
        }
        return this.parent == null || this.parent.isRemoveItemAllowedFromContainer(this, item);
    }

    public boolean isExplored() {
        return this.explored;
    }

    public void setExplored(boolean b) {
        this.explored = b;
    }

    public boolean isInCharacterInventory(IsoGameCharacter chr) {
        if (chr.getInventory() == this) {
            return true;
        }
        if (this.containingItem != null) {
            if (chr.getInventory().contains(this.containingItem, true)) {
                return true;
            }
            if (this.containingItem.getContainer() != null) {
                return this.containingItem.getContainer().isInCharacterInventory(chr);
            }
        }
        return false;
    }

    public boolean isInside(InventoryItem item) {
        if (this.containingItem == null) {
            return false;
        }
        if (this.containingItem == item) {
            return true;
        }
        return this.containingItem.getContainer() != null && this.containingItem.getContainer().isInside(item);
    }

    public InventoryItem getContainingItem() {
        return this.containingItem;
    }

    public InventoryItem DoAddItem(InventoryItem item) {
        return this.AddItem(item);
    }

    public InventoryItem DoAddItemBlind(InventoryItem item) {
        return this.AddItem(item);
    }

    public ArrayList<InventoryItem> AddItems(String type, int count) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (int i = 0; i < count; ++i) {
            InventoryItem item = this.AddItem(type);
            if (item == null) continue;
            result.add(item);
        }
        return result;
    }

    public <T extends InventoryItem> T addItem(ItemKey item) {
        return (T)((Object)this.AddItem(item.toString()));
    }

    public List<InventoryItem> addItems(ItemKey item, int count) {
        return this.AddItems(item.toString(), count);
    }

    public ArrayList<InventoryItem> AddItems(InventoryItem item, int count) {
        return this.AddItems(item.getFullType(), count);
    }

    public ArrayList<InventoryItem> AddItems(ArrayList<InventoryItem> items) {
        for (InventoryItem item : items) {
            this.AddItem(item);
        }
        return items;
    }

    public int getNumberOfItem(String findItem, boolean includeReplaceOnDeplete) {
        return this.getNumberOfItem(findItem, includeReplaceOnDeplete, false);
    }

    public int getNumberOfItem(String findItem) {
        return this.getNumberOfItem(findItem, false);
    }

    public int getNumberOfItem(String findItem, boolean includeReplaceOnDeplete, ArrayList<ItemContainer> containers) {
        int result = this.getNumberOfItem(findItem, includeReplaceOnDeplete);
        if (containers != null) {
            for (ItemContainer container : containers) {
                if (container == this) continue;
                result += container.getNumberOfItem(findItem, includeReplaceOnDeplete);
            }
        }
        return result;
    }

    public int getNumberOfItem(String findItem, boolean includeReplaceOnDeplete, boolean insideInv) {
        int result = 0;
        for (int i = 0; i < this.items.size(); ++i) {
            DrainableComboItem drainable;
            InventoryItem item = this.items.get(i);
            if (item.getFullType().equals(findItem) || item.getType().equals(findItem)) {
                ++result;
                continue;
            }
            if (insideInv && item instanceof InventoryContainer) {
                InventoryContainer container = (InventoryContainer)item;
                result += container.getItemContainer().getNumberOfItem(findItem);
                continue;
            }
            if (!includeReplaceOnDeplete || !(item instanceof DrainableComboItem) || (drainable = (DrainableComboItem)item).getReplaceOnDeplete() == null || !drainable.getReplaceOnDepleteFullType().equals(findItem) && !drainable.getReplaceOnDeplete().equals(findItem)) continue;
            ++result;
        }
        return result;
    }

    public InventoryItem addItem(InventoryItem item) {
        return this.AddItem(item);
    }

    public InventoryItem AddItem(InventoryItem item) {
        if (item == null) {
            return null;
        }
        if (this.containsID(item.id)) {
            DebugType.General.error((Object)"Error, container already has id");
            return this.getItemWithID(item.id);
        }
        this.drawDirty = true;
        if (this.parent != null) {
            this.dirty = true;
        }
        if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.parent.DirtySlice();
        }
        if (item.container != null) {
            item.container.Remove(item);
        }
        item.container = this;
        this.items.add(item);
        if (IsoWorld.instance.currentCell != null) {
            IsoWorld.instance.currentCell.addToProcessItems(item);
        }
        if (this.getParent() instanceof IsoFeedingTrough) {
            ((IsoFeedingTrough)this.getParent()).onFoodAdded();
        }
        item.OnAddedToContainer(this);
        if (this.getParent() != null) {
            this.getParent().flagForHotSave();
        }
        return item;
    }

    public void SpawnItem(InventoryItem item) {
        if (item == null) {
            this.AddItem(item);
        }
        item.SynchSpawn();
    }

    public InventoryItem AddItemBlind(InventoryItem item) {
        if (item == null) {
            return null;
        }
        if (item.getWeight() + this.getCapacityWeight() > (float)this.getCapacity()) {
            return null;
        }
        if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.parent.DirtySlice();
        }
        this.items.add(item);
        if (this.getParent() != null) {
            this.getParent().flagForHotSave();
        }
        return item;
    }

    public InventoryItem SpawnItem(String type) {
        InventoryItem item = this.AddItem(type);
        if (item == null) {
            return null;
        }
        item.SynchSpawn();
        return item;
    }

    public InventoryItem AddItem(String type) {
        Item scriptItem;
        this.drawDirty = true;
        if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.dirty = true;
        }
        if ((scriptItem = ScriptManager.instance.FindItem(type)) == null) {
            DebugLog.log((String)("ERROR: ItemContainer.AddItem: can't find " + type));
            return null;
        }
        if (scriptItem.obsolete) {
            return null;
        }
        InventoryItem item = InventoryItemFactory.CreateItem((String)type);
        if (item == null) {
            return null;
        }
        item.container = this;
        this.items.add(item);
        if (item instanceof Food) {
            Food food = (Food)item;
            food.setHeat(this.getTemprature());
        }
        if (item.hasComponent(ComponentType.FluidContainer) && item.isCookable()) {
            item.setItemHeat(this.getTemprature());
        }
        if (IsoWorld.instance.currentCell != null) {
            IsoWorld.instance.currentCell.addToProcessItems(item);
        }
        if (this.getParent() != null) {
            this.getParent().flagForHotSave();
        }
        return item;
    }

    public boolean SpawnItem(String type, float useDelta) {
        return this.AddItem(type, useDelta, true);
    }

    public boolean AddItem(String type, float useDelta) {
        InventoryItem item;
        this.drawDirty = true;
        if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.dirty = true;
        }
        if ((item = InventoryItemFactory.CreateItem((String)type)) == null) {
            return false;
        }
        if (item instanceof Drainable) {
            item.setCurrentUses((int)((float)item.getMaxUses() * useDelta));
        }
        item.container = this;
        this.items.add(item);
        if (this.getParent() != null) {
            this.getParent().flagForHotSave();
        }
        return true;
    }

    public boolean AddItem(String type, float useDelta, boolean synchSpawn) {
        InventoryItem item;
        this.drawDirty = true;
        if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.dirty = true;
        }
        if ((item = InventoryItemFactory.CreateItem((String)type)) == null) {
            return false;
        }
        if (item instanceof Drainable) {
            item.setCurrentUses((int)((float)item.getMaxUses() * useDelta));
        }
        item.container = this;
        this.items.add(item);
        if (this.getParent() != null) {
            this.getParent().flagForHotSave();
        }
        if (synchSpawn || item != null) {
            item.SynchSpawn();
        }
        return true;
    }

    public boolean contains(InventoryItem item) {
        return this.items.contains((Object)item);
    }

    public boolean containsWithModule(String moduleType) {
        return this.containsWithModule(moduleType, false);
    }

    public boolean containsWithModule(String moduleType, boolean withDeltaLeft) {
        String type = moduleType;
        String module = "Base";
        if (moduleType.contains(".")) {
            module = moduleType.split("\\.")[0];
            type = moduleType.split("\\.")[1];
        }
        for (int n = 0; n < this.items.size(); ++n) {
            InventoryItem item = this.items.get(n);
            if (item == null) {
                this.items.remove(n);
                --n;
                continue;
            }
            if (!item.type.equals(type.trim()) || !module.equals(item.getModule()) || withDeltaLeft && item instanceof DrainableComboItem && item.getCurrentUses() <= 0) continue;
            return true;
        }
        return false;
    }

    @Deprecated
    public void removeItemOnServer(InventoryItem item) {
        if (GameClient.client) {
            if (this.containingItem != null && this.containingItem.getWorldItem() != null) {
                GameClient.instance.addToItemRemoveSendBuffer((IsoObject)this.containingItem.getWorldItem(), this, item);
            } else {
                GameClient.instance.addToItemRemoveSendBuffer(this.parent, this, item);
            }
        }
    }

    public boolean contains(InventoryItem itemToFind, boolean doInv) {
        return this.contains(itemToFind, PZArrayList::referenceEqual, doInv);
    }

    public boolean contains(Invokers.Params2.Boolean.IParam2<InventoryItem> predicate, boolean doInv) {
        return this.contains(null, (Invokers.Params2.Boolean.ICallback)predicate, doInv);
    }

    public <T> boolean contains(T itemToCompare, Invokers.Params2.Boolean.ICallback<T, InventoryItem> predicate, boolean doInv) {
        InventoryItem foundItem = this.findItem(itemToCompare, predicate, doInv);
        return foundItem != null;
    }

    public InventoryItem findItem(Invokers.Params2.Boolean.IParam2<InventoryItem> predicate, boolean doInv) {
        return this.findItem(null, (Invokers.Params2.Boolean.ICallback)predicate, doInv);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public <T> InventoryItem findItem(T itemToCompare, Invokers.Params2.Boolean.ICallback<T, InventoryItem> predicate, boolean doInv) {
        InventoryItemList checkedBags = (InventoryItemList)TL_itemListPool.get().alloc();
        try {
            ArrayList<InventoryItem> items = this.items;
            for (int n = 0; n < items.size(); ++n) {
                InventoryContainer itemAsContainer;
                InventoryItem item = items.get(n);
                if (item == null) {
                    items.remove(n--);
                    continue;
                }
                if (predicate.accept(itemToCompare, (Object)item)) {
                    InventoryItem inventoryItem = item;
                    return inventoryItem;
                }
                if (!doInv || !(item instanceof InventoryContainer) || (itemAsContainer = (InventoryContainer)item).getInventory() == null || checkedBags.contains((Object)item)) continue;
                checkedBags.add((Object)itemAsContainer);
            }
            for (int i = 0; i < checkedBags.size(); ++i) {
                ItemContainer container = ((InventoryContainer)checkedBags.get(i)).getInventory();
                InventoryItem foundItem = container.findItem(itemToCompare, predicate, doInv);
                if (foundItem == null) continue;
                InventoryItem inventoryItem = foundItem;
                return inventoryItem;
            }
            InventoryItem inventoryItem = null;
            return inventoryItem;
        }
        finally {
            TL_itemListPool.get().release(checkedBags);
        }
    }

    public InventoryItem findItem(String type, boolean doInv, boolean ignoreBroken) {
        if (type.contains("/")) {
            String[] variants;
            for (String variant : variants = type.split("/")) {
                InventoryItem foundItem = this.findItem(variant.trim(), doInv, ignoreBroken);
                if (foundItem == null) continue;
                return foundItem;
            }
            return null;
        }
        if (type.contains("Type:")) {
            if (type.contains("Food")) {
                return this.findItem((Invokers.Params2.Boolean.IParam2<InventoryItem>)((Invokers.Params2.Boolean.IParam2)item -> item instanceof Food), doInv);
            }
            if (type.contains("Weapon")) {
                if (ignoreBroken) {
                    return this.findItem((Invokers.Params2.Boolean.IParam2<InventoryItem>)((Invokers.Params2.Boolean.IParam2)item -> item instanceof HandWeapon), doInv);
                }
                return this.findItem((Invokers.Params2.Boolean.IParam2<InventoryItem>)((Invokers.Params2.Boolean.IParam2)item -> item instanceof HandWeapon && !item.isBroken()), doInv);
            }
            if (type.contains("AlarmClock")) {
                return this.findItem((Invokers.Params2.Boolean.IParam2<InventoryItem>)((Invokers.Params2.Boolean.IParam2)item -> item instanceof AlarmClock), doInv);
            }
            if (type.contains("AlarmClockClothing")) {
                return this.findItem((Invokers.Params2.Boolean.IParam2<InventoryItem>)((Invokers.Params2.Boolean.IParam2)item -> item instanceof AlarmClockClothing), doInv);
            }
            return null;
        }
        if (ignoreBroken) {
            return this.findItem(type.trim(), ItemContainer::compareType, doInv);
        }
        return this.findItem(type.trim(), (lType, item) -> ItemContainer.compareType(lType, item) && !item.isBroken(), doInv);
    }

    public InventoryItem findHumanCorpseItem() {
        return this.findItem((Invokers.Params2.Boolean.IParam2<InventoryItem>)((Invokers.Params2.Boolean.IParam2)InventoryItem::isHumanCorpse), false);
    }

    public boolean containsHumanCorpse() {
        return this.contains((Invokers.Params2.Boolean.IParam2<InventoryItem>)((Invokers.Params2.Boolean.IParam2)InventoryItem::isHumanCorpse), false);
    }

    public boolean contains(String type, boolean doInv) {
        return this.contains(type, doInv, false);
    }

    public boolean containsType(String type) {
        return this.contains(type, false, false);
    }

    public boolean containsTypeRecurse(ItemKey type) {
        return this.containsTypeRecurse(type.toString());
    }

    public boolean containsTypeRecurse(String type) {
        return this.contains(type, true, false);
    }

    private boolean testBroken(boolean test, InventoryItem item) {
        if (!test) {
            return true;
        }
        return !item.isBroken();
    }

    public boolean contains(String type, boolean doInv, boolean ignoreBroken) {
        InventoryItem foundItem = this.findItem(type, doInv, ignoreBroken);
        return foundItem != null;
    }

    public boolean contains(String type) {
        return this.contains(type, false);
    }

    public AnimalInventoryItem getAnimalInventoryItem(IsoAnimal animal) {
        for (InventoryItem item : this.items) {
            if (!(item instanceof AnimalInventoryItem)) continue;
            AnimalInventoryItem animalInventoryItem = (AnimalInventoryItem)item;
            if (!(GameServer.server || GameClient.client ? animalInventoryItem.getAnimal().getOnlineID() == animal.getOnlineID() : animalInventoryItem.getAnimal().getAnimalID() == animal.getAnimalID())) continue;
            return animalInventoryItem;
        }
        return null;
    }

    public boolean canHumanCorpseFit(IsoGameCharacter chr) {
        float requiredSpace;
        boolean hasChr;
        boolean bl = hasChr = chr != null;
        if (this.isVehicleSeat()) {
            if (this.containsHumanCorpse()) {
                return false;
            }
            float occupiedSpace = this.getCapacityWeight();
            return !(occupiedSpace > 5.0f);
        }
        if (this.isVehiclePart()) {
            float requiredSpace2;
            float availableSpace = hasChr ? (float)this.getEffectiveCapacity(chr) - this.getContentsWeight() : this.getAvailableWeightCapacity();
            return !(availableSpace < (requiredSpace2 = (float)IsoGameCharacter.getWeightAsCorpse()));
        }
        float availableSpace = hasChr ? (float)this.getEffectiveCapacity(chr) - this.getContentsWeight() : this.getAvailableWeightCapacity();
        if (availableSpace < (requiredSpace = (float)IsoGameCharacter.getWeightAsCorpse())) {
            return false;
        }
        Object[] allowedContainers = new String[]{"bin", "cardboardbox", "crate", "militarycrate", "clothingdryer", "clothingdryerbasic", "clothingwasher", "coffin", "doghouse", "dumpster", "fireplace", TYPE_FRIDGE, TYPE_FREEZER, "locker", "militarylocker", "postbox", "shelter", "tent", "wardrobe"};
        String containerType = this.getType();
        return PZArrayUtil.contains((Object[])allowedContainers, (Object)containerType, StringUtils::equalsIgnoreCase);
    }

    public boolean canItemFit(InventoryItem item, IsoGameCharacter chr) {
        float requiredSpace;
        if (item.isHumanCorpse()) {
            return this.canHumanCorpseFit(chr);
        }
        float availableSpace = this.getAvailableWeightCapacity();
        return availableSpace >= (requiredSpace = item.getUnequippedWeight());
    }

    public boolean isVehiclePart() {
        return this.getVehiclePart() != null;
    }

    public boolean isVehicleSeat() {
        VehiclePart vehiclePart = this.getVehiclePart();
        if (vehiclePart == null) {
            return false;
        }
        return vehiclePart.isSeat();
    }

    public boolean isOccupiedVehicleSeat() {
        return this.isVehicleSeat() && this.vehiclePart.getVehicle().getCharacter(this.vehiclePart.getContainerSeatNumber()) != null;
    }

    private static InventoryItem getBestOf(InventoryItemList items, Comparator<InventoryItem> comparator) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        InventoryItem best = (InventoryItem)((Object)items.get(0));
        for (int i = 1; i < items.size(); ++i) {
            InventoryItem item = (InventoryItem)((Object)items.get(i));
            if (comparator.compare(item, best) <= 0) continue;
            best = item;
        }
        return best;
    }

    public InventoryItem getBest(Predicate<InventoryItem> predicate, Comparator<InventoryItem> comparator) {
        InventoryItemList items = (InventoryItemList)TL_itemListPool.get().alloc();
        this.getAll(predicate, (ArrayList<InventoryItem>)items);
        InventoryItem best = ItemContainer.getBestOf(items, comparator);
        TL_itemListPool.get().release(items);
        return best;
    }

    public InventoryItem getBestRecurse(Predicate<InventoryItem> predicate, Comparator<InventoryItem> comparator) {
        InventoryItemList items = (InventoryItemList)TL_itemListPool.get().alloc();
        this.getAllRecurse(predicate, (ArrayList<InventoryItem>)items);
        InventoryItem best = ItemContainer.getBestOf(items, comparator);
        TL_itemListPool.get().release(items);
        return best;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public InventoryItem getBestType(String type, Comparator<InventoryItem> comparator) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        try {
            InventoryItem inventoryItem = this.getBest((Predicate<InventoryItem>)predicate, comparator);
            return inventoryItem;
        }
        finally {
            ItemContainer.TL_predicates.get().type.release((Object)predicate);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public InventoryItem getBestTypeRecurse(String type, Comparator<InventoryItem> comparator) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        try {
            InventoryItem inventoryItem = this.getBestRecurse((Predicate<InventoryItem>)predicate, comparator);
            return inventoryItem;
        }
        finally {
            ItemContainer.TL_predicates.get().type.release((Object)predicate);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public InventoryItem getBestEval(LuaClosure predicateObj, LuaClosure comparatorObj) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(predicateObj);
        EvalComparator comparator = ((EvalComparator)ItemContainer.TL_comparators.get().eval.alloc()).init(comparatorObj);
        try {
            InventoryItem inventoryItem = this.getBest((Predicate<InventoryItem>)predicate, (Comparator<InventoryItem>)comparator);
            return inventoryItem;
        }
        finally {
            ItemContainer.TL_predicates.get().eval.release((Object)predicate);
            ItemContainer.TL_comparators.get().eval.release((Object)comparator);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public InventoryItem getBestEvalRecurse(LuaClosure predicateObj, LuaClosure comparatorObj) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(predicateObj);
        EvalComparator comparator = ((EvalComparator)ItemContainer.TL_comparators.get().eval.alloc()).init(comparatorObj);
        try {
            InventoryItem inventoryItem = this.getBestRecurse((Predicate<InventoryItem>)predicate, (Comparator<InventoryItem>)comparator);
            return inventoryItem;
        }
        finally {
            ItemContainer.TL_predicates.get().eval.release((Object)predicate);
            ItemContainer.TL_comparators.get().eval.release((Object)comparator);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public InventoryItem getBestEvalArg(LuaClosure predicateObj, LuaClosure comparatorObj, Object arg) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(predicateObj, arg);
        EvalArgComparator comparator = ((EvalArgComparator)ItemContainer.TL_comparators.get().evalArg.alloc()).init(comparatorObj, arg);
        try {
            InventoryItem inventoryItem = this.getBest((Predicate<InventoryItem>)predicate, (Comparator<InventoryItem>)comparator);
            return inventoryItem;
        }
        finally {
            ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
            ItemContainer.TL_comparators.get().evalArg.release((Object)comparator);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public InventoryItem getBestEvalArgRecurse(LuaClosure predicateObj, LuaClosure comparatorObj, Object arg) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(predicateObj, arg);
        EvalArgComparator comparator = ((EvalArgComparator)ItemContainer.TL_comparators.get().evalArg.alloc()).init(comparatorObj, arg);
        try {
            InventoryItem inventoryItem = this.getBestRecurse((Predicate<InventoryItem>)predicate, (Comparator<InventoryItem>)comparator);
            return inventoryItem;
        }
        finally {
            ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
            ItemContainer.TL_comparators.get().evalArg.release((Object)comparator);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public InventoryItem getBestTypeEval(String type, LuaClosure comparatorObj) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        EvalComparator comparator = ((EvalComparator)ItemContainer.TL_comparators.get().eval.alloc()).init(comparatorObj);
        try {
            InventoryItem inventoryItem = this.getBest((Predicate<InventoryItem>)predicate, (Comparator<InventoryItem>)comparator);
            return inventoryItem;
        }
        finally {
            ItemContainer.TL_predicates.get().type.release((Object)predicate);
            ItemContainer.TL_comparators.get().eval.release((Object)comparator);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public InventoryItem getBestTypeEvalRecurse(String type, LuaClosure comparatorObj) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        EvalComparator comparator = ((EvalComparator)ItemContainer.TL_comparators.get().eval.alloc()).init(comparatorObj);
        try {
            InventoryItem inventoryItem = this.getBestRecurse((Predicate<InventoryItem>)predicate, (Comparator<InventoryItem>)comparator);
            return inventoryItem;
        }
        finally {
            ItemContainer.TL_predicates.get().type.release((Object)predicate);
            ItemContainer.TL_comparators.get().eval.release((Object)comparator);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public InventoryItem getBestTypeEvalArg(String type, LuaClosure comparatorObj, Object arg) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        EvalArgComparator comparator = ((EvalArgComparator)ItemContainer.TL_comparators.get().evalArg.alloc()).init(comparatorObj, arg);
        try {
            InventoryItem inventoryItem = this.getBest((Predicate<InventoryItem>)predicate, (Comparator<InventoryItem>)comparator);
            return inventoryItem;
        }
        finally {
            ItemContainer.TL_predicates.get().type.release((Object)predicate);
            ItemContainer.TL_comparators.get().evalArg.release((Object)comparator);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public InventoryItem getBestTypeEvalArgRecurse(String type, LuaClosure comparatorObj, Object arg) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        EvalArgComparator comparator = ((EvalArgComparator)ItemContainer.TL_comparators.get().evalArg.alloc()).init(comparatorObj, arg);
        try {
            InventoryItem inventoryItem = this.getBestRecurse((Predicate<InventoryItem>)predicate, (Comparator<InventoryItem>)comparator);
            return inventoryItem;
        }
        finally {
            ItemContainer.TL_predicates.get().type.release((Object)predicate);
            ItemContainer.TL_comparators.get().evalArg.release((Object)comparator);
        }
    }

    public InventoryItem getBestCondition(Predicate<InventoryItem> predicate) {
        ConditionComparator comparator = (ConditionComparator)ItemContainer.TL_comparators.get().condition.alloc();
        InventoryItem best = this.getBest(predicate, (Comparator<InventoryItem>)comparator);
        ItemContainer.TL_comparators.get().condition.release((Object)comparator);
        if (best != null && best.getCondition() <= 0) {
            best = null;
        }
        return best;
    }

    public InventoryItem getBestConditionRecurse(Predicate<InventoryItem> predicate) {
        ConditionComparator comparator = (ConditionComparator)ItemContainer.TL_comparators.get().condition.alloc();
        InventoryItem best = this.getBestRecurse(predicate, (Comparator<InventoryItem>)comparator);
        ItemContainer.TL_comparators.get().condition.release((Object)comparator);
        if (best != null && best.getCondition() <= 0) {
            best = null;
        }
        return best;
    }

    public InventoryItem getBestCondition(String type) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        InventoryItem best = this.getBestCondition((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().type.release((Object)predicate);
        return best;
    }

    public InventoryItem getBestConditionRecurse(String type) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        InventoryItem best = this.getBestConditionRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().type.release((Object)predicate);
        return best;
    }

    public InventoryItem getBestConditionEval(LuaClosure functionObj) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(functionObj);
        InventoryItem best = this.getBestCondition((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().eval.release((Object)predicate);
        return best;
    }

    public InventoryItem getBestConditionEvalRecurse(LuaClosure functionObj) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(functionObj);
        InventoryItem best = this.getBestConditionRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().eval.release((Object)predicate);
        return best;
    }

    public InventoryItem getBestConditionEvalArg(LuaClosure functionObj, Object arg) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(functionObj, arg);
        InventoryItem best = this.getBestCondition((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
        return best;
    }

    public InventoryItem getBestConditionEvalArgRecurse(LuaClosure functionObj, Object arg) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(functionObj, arg);
        InventoryItem best = this.getBestConditionRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
        return best;
    }

    public InventoryItem getFirstEval(LuaClosure functionObj) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(functionObj);
        InventoryItem item = this.getFirst((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().eval.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstEvalArg(LuaClosure functionObj, Object arg) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(functionObj, arg);
        InventoryItem item = this.getFirst((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
        return item;
    }

    public boolean containsEval(LuaClosure functionObj) {
        return this.getFirstEval(functionObj) != null;
    }

    public boolean containsEvalArg(LuaClosure functionObj, Object arg) {
        return this.getFirstEvalArg(functionObj, arg) != null;
    }

    public boolean containsEvalRecurse(LuaClosure functionObj) {
        return this.getFirstEvalRecurse(functionObj) != null;
    }

    public boolean containsEvalArgRecurse(LuaClosure functionObj, Object arg) {
        return this.getFirstEvalArgRecurse(functionObj, arg) != null;
    }

    public boolean containsTag(ItemTag itemTag) {
        return this.getFirstTag(itemTag) != null;
    }

    public boolean containsTagEval(ItemTag itemTag, LuaClosure functionObj) {
        return this.getFirstTagEval(itemTag, functionObj) != null;
    }

    public boolean containsTagRecurse(ItemTag itemTag) {
        return this.getFirstTagRecurse(itemTag) != null;
    }

    public boolean containsTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj) {
        return this.getFirstTagEvalRecurse(itemTag, functionObj) != null;
    }

    public boolean containsTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg) {
        return this.getFirstTagEvalArgRecurse(itemTag, functionObj, arg) != null;
    }

    public boolean containsTypeEvalRecurse(String type, LuaClosure functionObj) {
        return this.getFirstTypeEvalRecurse(type, functionObj) != null;
    }

    public boolean containsTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg) {
        return this.getFirstTypeEvalArgRecurse(type, functionObj, arg) != null;
    }

    private static boolean compareType(String type1, String type2) {
        if (type1 != null && type1.contains("/")) {
            int p = type1.indexOf(type2);
            if (p == -1) {
                return false;
            }
            char chBefore = p > 0 ? type1.charAt(p - 1) : (char)'\u0000';
            char chAfter = p + type2.length() < type1.length() ? type1.charAt(p + type2.length()) : (char)'\u0000';
            return chBefore == '\u0000' && chAfter == '/' || chBefore == '/' && chAfter == '\u0000' || chBefore == '/' && chAfter == '/';
        }
        return type1.equals(type2);
    }

    private static boolean compareType(String type, InventoryItem item) {
        if (type != null && type.indexOf(46) == -1) {
            return ItemContainer.compareType(type, item.getType());
        }
        return ItemContainer.compareType(type, item.getFullType()) || ItemContainer.compareType(type, item.getType());
    }

    public InventoryItem getFirst(Predicate<InventoryItem> predicate) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                --i;
                continue;
            }
            if (!predicate.test(item)) continue;
            return item;
        }
        return null;
    }

    public InventoryItem getFirstRecurse(Predicate<InventoryItem> predicate) {
        int i;
        InventoryItemList bags = (InventoryItemList)TL_itemListPool.get().alloc();
        for (i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                --i;
                continue;
            }
            if (predicate.test(item)) {
                TL_itemListPool.get().release(bags);
                return item;
            }
            if (!(item instanceof InventoryContainer)) continue;
            bags.add((Object)item);
        }
        for (i = 0; i < bags.size(); ++i) {
            ItemContainer container = ((InventoryContainer)bags.get(i)).getInventory();
            InventoryItem item = container.getFirstRecurse(predicate);
            if (item == null) continue;
            TL_itemListPool.get().release(bags);
            return item;
        }
        TL_itemListPool.get().release(bags);
        return null;
    }

    public ArrayList<InventoryItem> getSome(Predicate<InventoryItem> predicate, int count, ArrayList<InventoryItem> result) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                --i;
                continue;
            }
            if (!predicate.test(item)) continue;
            result.add(item);
            if (result.size() >= count) break;
        }
        return result;
    }

    public ArrayList<InventoryItem> getSomeRecurse(Predicate<InventoryItem> predicate, int count, ArrayList<InventoryItem> result) {
        int i;
        InventoryItemList bags = (InventoryItemList)TL_itemListPool.get().alloc();
        for (i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                --i;
                continue;
            }
            if (predicate.test(item)) {
                result.add(item);
                if (result.size() >= count) {
                    TL_itemListPool.get().release(bags);
                    return result;
                }
            }
            if (!(item instanceof InventoryContainer)) continue;
            bags.add((Object)item);
        }
        for (i = 0; i < bags.size(); ++i) {
            ItemContainer container = ((InventoryContainer)bags.get(i)).getInventory();
            container.getSomeRecurse(predicate, count, result);
            if (result.size() >= count) break;
        }
        TL_itemListPool.get().release(bags);
        return result;
    }

    public ArrayList<InventoryItem> getAll(Predicate<InventoryItem> predicate, ArrayList<InventoryItem> result) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                --i;
                continue;
            }
            if (!predicate.test(item)) continue;
            result.add(item);
        }
        return result;
    }

    public ArrayList<InventoryItem> getAllRecurse(Predicate<InventoryItem> predicate, ArrayList<InventoryItem> result) {
        int i;
        InventoryItemList bags = (InventoryItemList)TL_itemListPool.get().alloc();
        for (i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                --i;
                continue;
            }
            if (predicate.test(item)) {
                result.add(item);
            }
            if (!(item instanceof InventoryContainer)) continue;
            bags.add((Object)item);
        }
        for (i = 0; i < bags.size(); ++i) {
            ItemContainer container = ((InventoryContainer)bags.get(i)).getInventory();
            container.getAllRecurse(predicate, result);
        }
        TL_itemListPool.get().release(bags);
        return result;
    }

    public ArrayList<InventoryItem> getAllRecurse(Predicate<InventoryItem> predicate) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        return this.getAllRecurse(predicate, result);
    }

    public int getCount(Predicate<InventoryItem> predicate) {
        InventoryItemList items = (InventoryItemList)TL_itemListPool.get().alloc();
        this.getAll(predicate, (ArrayList<InventoryItem>)items);
        int count = items.size();
        TL_itemListPool.get().release(items);
        return count;
    }

    public int getCountRecurse(Predicate<InventoryItem> predicate) {
        InventoryItemList items = (InventoryItemList)TL_itemListPool.get().alloc();
        this.getAllRecurse(predicate, (ArrayList<InventoryItem>)items);
        int count = items.size();
        TL_itemListPool.get().release(items);
        return count;
    }

    public int getCountTag(ItemTag itemTag) {
        TagPredicate predicate = ((TagPredicate)ItemContainer.TL_predicates.get().tag.alloc()).init(itemTag);
        int count = this.getCount((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().tag.release((Object)predicate);
        return count;
    }

    public int getCountTagEval(ItemTag itemTag, LuaClosure functionObj) {
        TagEvalPredicate predicate = ((TagEvalPredicate)ItemContainer.TL_predicates.get().tagEval.alloc()).init(itemTag, functionObj);
        int count = this.getCount((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().tagEval.release((Object)predicate);
        return count;
    }

    public int getCountTagEvalArg(ItemTag itemTag, LuaClosure functionObj, Object arg) {
        TagEvalArgPredicate predicate = ((TagEvalArgPredicate)ItemContainer.TL_predicates.get().tagEvalArg.alloc()).init(itemTag, functionObj, arg);
        int count = this.getCount((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().tagEvalArg.release((Object)predicate);
        return count;
    }

    public int getCountTagRecurse(ItemTag itemTag) {
        TagPredicate predicate = ((TagPredicate)ItemContainer.TL_predicates.get().tag.alloc()).init(itemTag);
        int count = this.getCountRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().tag.release((Object)predicate);
        return count;
    }

    public int getCountTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj) {
        TagEvalPredicate predicate = ((TagEvalPredicate)ItemContainer.TL_predicates.get().tagEval.alloc()).init(itemTag, functionObj);
        int count = this.getCountRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().tagEval.release((Object)predicate);
        return count;
    }

    public int getCountTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg) {
        TagEvalArgPredicate predicate = ((TagEvalArgPredicate)ItemContainer.TL_predicates.get().tagEvalArg.alloc()).init(itemTag, functionObj, arg);
        int count = this.getCountRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().tagEvalArg.release((Object)predicate);
        return count;
    }

    public int getCountType(String type) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        int count = this.getCount((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().type.release((Object)predicate);
        return count;
    }

    public int getCountTypeEval(String type, LuaClosure functionObj) {
        TypeEvalPredicate predicate = ((TypeEvalPredicate)ItemContainer.TL_predicates.get().typeEval.alloc()).init(type, functionObj);
        int count = this.getCount((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().typeEval.release((Object)predicate);
        return count;
    }

    public int getCountTypeEvalArg(String type, LuaClosure functionObj, Object arg) {
        TypeEvalArgPredicate predicate = ((TypeEvalArgPredicate)ItemContainer.TL_predicates.get().typeEvalArg.alloc()).init(type, functionObj, arg);
        int count = this.getCount((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().typeEvalArg.release((Object)predicate);
        return count;
    }

    public int getCountTypeRecurse(String type) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        int count = this.getCountRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().type.release((Object)predicate);
        return count;
    }

    public int getCountTypeEvalRecurse(String type, LuaClosure functionObj) {
        TypeEvalPredicate predicate = ((TypeEvalPredicate)ItemContainer.TL_predicates.get().typeEval.alloc()).init(type, functionObj);
        int count = this.getCountRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().typeEval.release((Object)predicate);
        return count;
    }

    public int getCountTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg) {
        TypeEvalArgPredicate predicate = ((TypeEvalArgPredicate)ItemContainer.TL_predicates.get().typeEvalArg.alloc()).init(type, functionObj, arg);
        int count = this.getCountRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().typeEvalArg.release((Object)predicate);
        return count;
    }

    public int getCountEval(LuaClosure functionObj) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(functionObj);
        int count = this.getCount((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().eval.release((Object)predicate);
        return count;
    }

    public int getCountEvalArg(LuaClosure functionObj, Object arg) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(functionObj, arg);
        int count = this.getCount((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
        return count;
    }

    public int getCountEvalRecurse(LuaClosure functionObj) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(functionObj);
        int count = this.getCountRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().eval.release((Object)predicate);
        return count;
    }

    public int getCountEvalArgRecurse(LuaClosure functionObj, Object arg) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(functionObj, arg);
        int count = this.getCountRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
        return count;
    }

    public InventoryItem getFirstCategory(String category) {
        CategoryPredicate predicate = ((CategoryPredicate)ItemContainer.TL_predicates.get().category.alloc()).init(category);
        InventoryItem item = this.getFirst((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().category.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstCategoryRecurse(String category) {
        CategoryPredicate predicate = ((CategoryPredicate)ItemContainer.TL_predicates.get().category.alloc()).init(category);
        InventoryItem item = this.getFirstRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().category.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstEvalRecurse(LuaClosure functionObj) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(functionObj);
        InventoryItem item = this.getFirstRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().eval.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstEvalArgRecurse(LuaClosure functionObj, Object arg) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(functionObj, arg);
        InventoryItem item = this.getFirstRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstTag(ItemTag itemTag) {
        TagPredicate predicate = ((TagPredicate)ItemContainer.TL_predicates.get().tag.alloc()).init(itemTag);
        InventoryItem item = this.getFirst((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().tag.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstTagRecurse(ItemTag itemTag) {
        TagPredicate predicate = ((TagPredicate)ItemContainer.TL_predicates.get().tag.alloc()).init(itemTag);
        InventoryItem item = this.getFirstRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().tag.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstTagEval(ItemTag itemTag, LuaClosure functionObj) {
        TagEvalPredicate predicate = ((TagEvalPredicate)ItemContainer.TL_predicates.get().tagEval.alloc()).init(itemTag, functionObj);
        InventoryItem item = this.getFirstRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().tagEval.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj) {
        TagEvalPredicate predicate = ((TagEvalPredicate)ItemContainer.TL_predicates.get().tagEval.alloc()).init(itemTag, functionObj);
        InventoryItem item = this.getFirstRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().tagEval.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg) {
        TagEvalArgPredicate predicate = ((TagEvalArgPredicate)ItemContainer.TL_predicates.get().tagEvalArg.alloc()).init(itemTag, functionObj, arg);
        InventoryItem item = this.getFirstRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().tagEvalArg.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstType(String type) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        InventoryItem item = this.getFirst((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().type.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstTypeRecurse(ItemKey key) {
        return this.getFirstTypeRecurse(key.toString());
    }

    public InventoryItem getFirstTypeRecurse(String type) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        InventoryItem item = this.getFirstRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().type.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstTypeEval(String type, LuaClosure functionObj) {
        TypeEvalPredicate predicate = ((TypeEvalPredicate)ItemContainer.TL_predicates.get().typeEval.alloc()).init(type, functionObj);
        InventoryItem item = this.getFirstRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().typeEval.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstTypeEvalRecurse(ItemKey key, LuaClosure functionObj) {
        return this.getFirstTypeEvalRecurse(key.toString(), functionObj);
    }

    public InventoryItem getFirstTypeEvalRecurse(String type, LuaClosure functionObj) {
        TypeEvalPredicate predicate = ((TypeEvalPredicate)ItemContainer.TL_predicates.get().typeEval.alloc()).init(type, functionObj);
        InventoryItem item = this.getFirstRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().typeEval.release((Object)predicate);
        return item;
    }

    public InventoryItem getFirstTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg) {
        TypeEvalArgPredicate predicate = ((TypeEvalArgPredicate)ItemContainer.TL_predicates.get().typeEvalArg.alloc()).init(type, functionObj, arg);
        InventoryItem item = this.getFirstRecurse((Predicate<InventoryItem>)predicate);
        ItemContainer.TL_predicates.get().typeEvalArg.release((Object)predicate);
        return item;
    }

    public ArrayList<InventoryItem> getSomeCategory(String category, int count, ArrayList<InventoryItem> result) {
        CategoryPredicate predicate = ((CategoryPredicate)ItemContainer.TL_predicates.get().category.alloc()).init(category);
        ArrayList<InventoryItem> items = this.getSome((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().category.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeCategoryRecurse(String category, int count, ArrayList<InventoryItem> result) {
        CategoryPredicate predicate = ((CategoryPredicate)ItemContainer.TL_predicates.get().category.alloc()).init(category);
        ArrayList<InventoryItem> items = this.getSomeRecurse((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().category.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTag(ItemTag itemTag, int count, ArrayList<InventoryItem> result) {
        TagPredicate predicate = ((TagPredicate)ItemContainer.TL_predicates.get().tag.alloc()).init(itemTag);
        ArrayList<InventoryItem> items = this.getSome((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().tag.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTagEval(ItemTag itemTag, LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        TagEvalPredicate predicate = ((TagEvalPredicate)ItemContainer.TL_predicates.get().tagEval.alloc()).init(itemTag, functionObj);
        ArrayList<InventoryItem> items = this.getSome((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().tagEval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTagEvalArg(ItemTag itemTag, LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        TagEvalArgPredicate predicate = ((TagEvalArgPredicate)ItemContainer.TL_predicates.get().tagEvalArg.alloc()).init(itemTag, functionObj, arg);
        ArrayList<InventoryItem> items = this.getSome((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().tagEvalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTagRecurse(ItemTag itemTag, int count, ArrayList<InventoryItem> result) {
        TagPredicate predicate = ((TagPredicate)ItemContainer.TL_predicates.get().tag.alloc()).init(itemTag);
        ArrayList<InventoryItem> items = this.getSomeRecurse((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().tag.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        TagEvalPredicate predicate = ((TagEvalPredicate)ItemContainer.TL_predicates.get().tagEval.alloc()).init(itemTag, functionObj);
        ArrayList<InventoryItem> items = this.getSomeRecurse((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().tagEval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        TagEvalArgPredicate predicate = ((TagEvalArgPredicate)ItemContainer.TL_predicates.get().tagEvalArg.alloc()).init(itemTag, functionObj, arg);
        ArrayList<InventoryItem> items = this.getSomeRecurse((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().tagEvalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeType(String type, int count, ArrayList<InventoryItem> result) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        ArrayList<InventoryItem> items = this.getSome((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().type.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTypeEval(String type, LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        TypeEvalPredicate predicate = ((TypeEvalPredicate)ItemContainer.TL_predicates.get().typeEval.alloc()).init(type, functionObj);
        ArrayList<InventoryItem> items = this.getSome((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().typeEval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTypeEvalArg(String type, LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        TypeEvalArgPredicate predicate = ((TypeEvalArgPredicate)ItemContainer.TL_predicates.get().typeEvalArg.alloc()).init(type, functionObj, arg);
        ArrayList<InventoryItem> items = this.getSome((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().typeEvalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTypeRecurse(String type, int count, ArrayList<InventoryItem> result) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        ArrayList<InventoryItem> items = this.getSomeRecurse((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().type.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTypeEvalRecurse(String type, LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        TypeEvalPredicate predicate = ((TypeEvalPredicate)ItemContainer.TL_predicates.get().typeEval.alloc()).init(type, functionObj);
        ArrayList<InventoryItem> items = this.getSomeRecurse((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().typeEval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        TypeEvalArgPredicate predicate = ((TypeEvalArgPredicate)ItemContainer.TL_predicates.get().typeEvalArg.alloc()).init(type, functionObj, arg);
        ArrayList<InventoryItem> items = this.getSomeRecurse((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().typeEvalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeEval(LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(functionObj);
        ArrayList<InventoryItem> items = this.getSome((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().eval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeEvalArg(LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(functionObj, arg);
        ArrayList<InventoryItem> items = this.getSome((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeEvalRecurse(LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(functionObj);
        ArrayList<InventoryItem> items = this.getSomeRecurse((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().eval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeEvalArgRecurse(LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(functionObj, arg);
        ArrayList<InventoryItem> items = this.getSomeRecurse((Predicate<InventoryItem>)predicate, count, result);
        ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllCategory(String category, ArrayList<InventoryItem> result) {
        CategoryPredicate predicate = ((CategoryPredicate)ItemContainer.TL_predicates.get().category.alloc()).init(category);
        ArrayList<InventoryItem> items = this.getAll((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().category.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllCategoryRecurse(String category, ArrayList<InventoryItem> result) {
        CategoryPredicate predicate = ((CategoryPredicate)ItemContainer.TL_predicates.get().category.alloc()).init(category);
        ArrayList<InventoryItem> items = this.getAllRecurse((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().category.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTag(ItemTag itemTag) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        return this.getAllTag(itemTag, result);
    }

    public ArrayList<InventoryItem> getAllTag(ItemTag itemTag, ArrayList<InventoryItem> result) {
        TagPredicate predicate = ((TagPredicate)ItemContainer.TL_predicates.get().tag.alloc()).init(itemTag);
        ArrayList<InventoryItem> items = this.getAll((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().tag.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTagEval(ItemTag itemTag, LuaClosure functionObj, ArrayList<InventoryItem> result) {
        if (result == null) {
            result = new ArrayList();
        }
        TagEvalPredicate predicate = ((TagEvalPredicate)ItemContainer.TL_predicates.get().tagEval.alloc()).init(itemTag, functionObj);
        ArrayList<InventoryItem> items = this.getAll((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().tagEval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTagEvalArg(ItemTag itemTag, LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        TagEvalArgPredicate predicate = ((TagEvalArgPredicate)ItemContainer.TL_predicates.get().tagEvalArg.alloc()).init(itemTag, functionObj, arg);
        ArrayList<InventoryItem> items = this.getAll((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().tagEvalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTagRecurse(ItemTag itemTag, ArrayList<InventoryItem> result) {
        TagPredicate predicate = ((TagPredicate)ItemContainer.TL_predicates.get().tag.alloc()).init(itemTag);
        ArrayList<InventoryItem> items = this.getAllRecurse((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().tag.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj, ArrayList<InventoryItem> result) {
        TagEvalPredicate predicate = ((TagEvalPredicate)ItemContainer.TL_predicates.get().tagEval.alloc()).init(itemTag, functionObj);
        ArrayList<InventoryItem> items = this.getAllRecurse((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().tagEval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        TagEvalArgPredicate predicate = ((TagEvalArgPredicate)ItemContainer.TL_predicates.get().tagEvalArg.alloc()).init(itemTag, functionObj, arg);
        ArrayList<InventoryItem> items = this.getAllRecurse((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().tagEvalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllType(String type, ArrayList<InventoryItem> result) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        ArrayList<InventoryItem> items = this.getAll((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().type.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTypeEval(String type, LuaClosure functionObj, ArrayList<InventoryItem> result) {
        TypeEvalPredicate predicate = ((TypeEvalPredicate)ItemContainer.TL_predicates.get().typeEval.alloc()).init(type, functionObj);
        ArrayList<InventoryItem> items = this.getAll((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().typeEval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTypeEvalArg(String type, LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        TypeEvalArgPredicate predicate = ((TypeEvalArgPredicate)ItemContainer.TL_predicates.get().typeEvalArg.alloc()).init(type, functionObj, arg);
        ArrayList<InventoryItem> items = this.getAll((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().typeEvalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTypeRecurse(String type, ArrayList<InventoryItem> result) {
        TypePredicate predicate = ((TypePredicate)ItemContainer.TL_predicates.get().type.alloc()).init(type);
        ArrayList<InventoryItem> items = this.getAllRecurse((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().type.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTypeEvalRecurse(String type, LuaClosure functionObj, ArrayList<InventoryItem> result) {
        TypeEvalPredicate predicate = ((TypeEvalPredicate)ItemContainer.TL_predicates.get().typeEval.alloc()).init(type, functionObj);
        ArrayList<InventoryItem> items = this.getAllRecurse((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().typeEval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        TypeEvalArgPredicate predicate = ((TypeEvalArgPredicate)ItemContainer.TL_predicates.get().typeEvalArg.alloc()).init(type, functionObj, arg);
        ArrayList<InventoryItem> items = this.getAllRecurse((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().typeEvalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllEval(LuaClosure functionObj, ArrayList<InventoryItem> result) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(functionObj);
        ArrayList<InventoryItem> items = this.getAll((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().eval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllEvalArg(LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(functionObj, arg);
        ArrayList<InventoryItem> items = this.getAll((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllEvalRecurse(LuaClosure functionObj, ArrayList<InventoryItem> result) {
        EvalPredicate predicate = ((EvalPredicate)ItemContainer.TL_predicates.get().eval.alloc()).init(functionObj);
        ArrayList<InventoryItem> items = this.getAllRecurse((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().eval.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllEvalArgRecurse(LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        EvalArgPredicate predicate = ((EvalArgPredicate)ItemContainer.TL_predicates.get().evalArg.alloc()).init(functionObj, arg);
        ArrayList<InventoryItem> items = this.getAllRecurse((Predicate<InventoryItem>)predicate, result);
        ItemContainer.TL_predicates.get().evalArg.release((Object)predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeCategory(String category, int count) {
        return this.getSomeCategory(category, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeEval(LuaClosure functionObj, int count) {
        return this.getSomeEval(functionObj, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeEvalArg(LuaClosure functionObj, Object arg, int count) {
        return this.getSomeEvalArg(functionObj, arg, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeTypeEval(String type, LuaClosure functionObj, int count) {
        return this.getSomeTypeEval(type, functionObj, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeTypeEvalArg(String type, LuaClosure functionObj, Object arg, int count) {
        return this.getSomeTypeEvalArg(type, functionObj, arg, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeEvalRecurse(LuaClosure functionObj, int count) {
        return this.getSomeEvalRecurse(functionObj, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeEvalArgRecurse(LuaClosure functionObj, Object arg, int count) {
        return this.getSomeEvalArgRecurse(functionObj, arg, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeTag(ItemTag itemTag, int count) {
        return this.getSomeTag(itemTag, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeTagRecurse(ItemTag itemTag, int count) {
        return this.getSomeTagRecurse(itemTag, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj, int count) {
        return this.getSomeTagEvalRecurse(itemTag, functionObj, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg, int count) {
        return this.getSomeTagEvalArgRecurse(itemTag, functionObj, arg, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeType(String type, int count) {
        return this.getSomeType(type, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeTypeRecurse(String type, int count) {
        return this.getSomeTypeRecurse(type, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeTypeEvalRecurse(String type, LuaClosure functionObj, int count) {
        return this.getSomeTypeEvalRecurse(type, functionObj, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getSomeTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg, int count) {
        return this.getSomeTypeEvalArgRecurse(type, functionObj, arg, count, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAll(Predicate<InventoryItem> predicate) {
        return this.getAll(predicate, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllCategory(String category) {
        return this.getAllCategory(category, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllEval(LuaClosure functionObj) {
        return this.getAllEval(functionObj, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllEvalArg(LuaClosure functionObj, Object arg) {
        return this.getAllEvalArg(functionObj, arg, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllTagEval(ItemTag itemTag, LuaClosure functionObj) {
        return this.getAllTagEval(itemTag, functionObj, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllTagEvalArg(ItemTag itemTag, LuaClosure functionObj, Object arg) {
        return this.getAllTagEvalArg(itemTag, functionObj, arg, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllTypeEval(String type, LuaClosure functionObj) {
        return this.getAllTypeEval(type, functionObj, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllTypeEvalArg(String type, LuaClosure functionObj, Object arg) {
        return this.getAllTypeEvalArg(type, functionObj, arg, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllEvalRecurse(LuaClosure functionObj) {
        return this.getAllEvalRecurse(functionObj, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllEvalArgRecurse(LuaClosure functionObj, Object arg) {
        return this.getAllEvalArgRecurse(functionObj, arg, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllType(String type) {
        return this.getAllType(type, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllTypeRecurse(String type) {
        return this.getAllTypeRecurse(type, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllTypeEvalRecurse(String type, LuaClosure functionObj) {
        return this.getAllTypeEvalRecurse(type, functionObj, new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg) {
        return this.getAllTypeEvalArgRecurse(type, functionObj, arg, new ArrayList<InventoryItem>());
    }

    public InventoryItem FindAndReturnCategory(String category) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!item.getCategory().equals(category)) continue;
            return item;
        }
        return null;
    }

    public ArrayList<InventoryItem> FindAndReturn(String type, int count) {
        return this.getSomeType(type, count);
    }

    public InventoryItem FindAndReturn(String type, ArrayList<InventoryItem> itemToCheck) {
        if (type == null) {
            return null;
        }
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item.type == null || !ItemContainer.compareType(type, item) || itemToCheck.contains((Object)item)) continue;
            return item;
        }
        return null;
    }

    public InventoryItem FindAndReturn(String type) {
        return this.getFirstType(type);
    }

    public ArrayList<InventoryItem> FindAll(String type) {
        return this.getAllType(type);
    }

    public InventoryItem FindAndReturnStack(String type) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem test;
            InventoryItem item = this.items.get(i);
            if (!ItemContainer.compareType(type, item) || !item.CanStack(test = InventoryItemFactory.CreateItem((String)(item.module + "." + type)))) continue;
            return item;
        }
        return null;
    }

    public InventoryItem FindAndReturnStack(InventoryItem itemlike) {
        String string = itemlike.type;
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!(item.type == null ? string == null : item.type.equals(string)) || !item.CanStack(itemlike)) continue;
            return item;
        }
        return null;
    }

    public boolean HasType(ItemType itemType) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item.itemType != itemType) continue;
            return true;
        }
        return false;
    }

    public void Remove(InventoryItem item) {
        if (this.getCharacter() != null) {
            this.getCharacter().removeFromHands(item);
        }
        for (int m = 0; m < this.items.size(); ++m) {
            IsoObject isoObject;
            InventoryItem item2 = this.items.get(m);
            if (item2 != item) continue;
            item.OnBeforeRemoveFromContainer(this);
            this.items.remove((Object)item);
            item.container = null;
            this.drawDirty = true;
            this.dirty = true;
            if (this.parent != null) {
                this.dirty = true;
            }
            if ((isoObject = this.parent) instanceof IsoDeadBody) {
                IsoDeadBody isoDeadBody = (IsoDeadBody)isoObject;
                isoDeadBody.checkClothing(item);
            }
            if ((isoObject = this.parent) instanceof IsoMannequin) {
                IsoMannequin isoMannequin = (IsoMannequin)isoObject;
                isoMannequin.checkClothing(item);
            }
            if ((isoObject = this.parent) instanceof IsoFeedingTrough) {
                IsoFeedingTrough isoFeedingTrough = (IsoFeedingTrough)isoObject;
                isoFeedingTrough.onRemoveFood();
            }
            if (this.getParent() != null) {
                this.getParent().flagForHotSave();
            }
            return;
        }
    }

    public void DoRemoveItem(InventoryItem item) {
        this.drawDirty = true;
        if (this.parent != null) {
            this.dirty = true;
        }
        this.items.remove((Object)item);
        item.container = null;
        IsoObject isoObject = this.parent;
        if (isoObject instanceof IsoDeadBody) {
            IsoDeadBody isoDeadBody = (IsoDeadBody)isoObject;
            isoDeadBody.checkClothing(item);
        }
        if ((isoObject = this.parent) instanceof IsoMannequin) {
            IsoMannequin isoMannequin = (IsoMannequin)isoObject;
            isoMannequin.checkClothing(item);
        }
        if ((isoObject = this.parent) instanceof IsoFeedingTrough) {
            IsoFeedingTrough isoFeedingTrough = (IsoFeedingTrough)isoObject;
            isoFeedingTrough.onRemoveFood();
        }
        if (this.getParent() != null) {
            this.getParent().flagForHotSave();
        }
    }

    public void Remove(String itemTypes) {
        for (int m = 0; m < this.items.size(); ++m) {
            InventoryItem item = this.items.get(m);
            if (!item.type.equals(itemTypes)) continue;
            if (item.getCurrentUses() > 1) {
                item.setCurrentUses(item.getCurrentUses() - 1);
            } else {
                this.items.remove((Object)item);
            }
            item.container = null;
            this.drawDirty = true;
            this.dirty = true;
            if (this.parent != null) {
                this.dirty = true;
            }
            if (this.getParent() != null) {
                this.getParent().flagForHotSave();
            }
            return;
        }
    }

    public InventoryItem Remove(ItemType itemType) {
        for (int m = 0; m < this.items.size(); ++m) {
            InventoryItem item = this.items.get(m);
            if (item.itemType != itemType) continue;
            this.items.remove((Object)item);
            item.container = null;
            this.drawDirty = true;
            this.dirty = true;
            if (this.parent != null) {
                this.dirty = true;
            }
            if (this.getParent() != null) {
                this.getParent().flagForHotSave();
            }
            return item;
        }
        return null;
    }

    public InventoryItem Find(String itemType) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!item.type.equals(itemType)) continue;
            return item;
        }
        return null;
    }

    public InventoryItem Find(ItemType itemType) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item.itemType != itemType) continue;
            return item;
        }
        return null;
    }

    public ArrayList<InventoryItem> RemoveAll(String itemType) {
        return this.RemoveAll(itemType, this.items.size());
    }

    public ArrayList<InventoryItem> RemoveAll(String itemType, int count) {
        this.drawDirty = true;
        if (this.parent != null) {
            this.dirty = true;
        }
        ArrayList<InventoryItem> toRemoveList = new ArrayList<InventoryItem>();
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!item.type.equals(itemType) && !item.fullType.equals(itemType)) continue;
            item.container = null;
            toRemoveList.add(item);
            this.dirty = true;
            if (toRemoveList.size() >= count) break;
        }
        for (InventoryItem toRemove : toRemoveList) {
            this.items.remove((Object)toRemove);
        }
        return toRemoveList;
    }

    public InventoryItem RemoveOneOf(String string, boolean insideInv) {
        InventoryItem item;
        this.drawDirty = true;
        if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.dirty = true;
        }
        for (int m = 0; m < this.items.size(); ++m) {
            item = this.items.get(m);
            if (!item.getFullType().equals(string) && !item.type.equals(string)) continue;
            if (item.getCurrentUses() > 1) {
                item.setCurrentUses(item.getCurrentUses() - 1);
            } else {
                item.container = null;
                this.items.remove((Object)item);
            }
            this.dirty = true;
            return item;
        }
        if (insideInv) {
            for (int i = 0; i < this.items.size(); ++i) {
                InventoryContainer container;
                item = this.items.get(i);
                if (!(item instanceof InventoryContainer) || (container = (InventoryContainer)item).getItemContainer() == null || container.getItemContainer().RemoveOneOf(string, insideInv) == null) continue;
                return item;
            }
        }
        return null;
    }

    public void RemoveOneOf(String string) {
        this.RemoveOneOf(string, true);
    }

    public float getContentsWeight() {
        float total = 0.0f;
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            total += item.getUnequippedWeight();
        }
        return total;
    }

    public float getMaxWeight() {
        IsoObject isoObject = this.parent;
        if (isoObject instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)isoObject;
            return isoGameCharacter.getMaxWeight();
        }
        return this.getCapacity();
    }

    public float getCapacityWeight() {
        IsoObject isoObject = this.parent;
        if (isoObject instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)isoObject;
            if (Core.debug && player.isGhostMode() || !player.isAccessLevel("None") && player.isUnlimitedCarry()) {
                return 0.0f;
            }
            if (player.isUnlimitedCarry()) {
                return 0.0f;
            }
        }
        if ((isoObject = this.parent) instanceof IsoGameCharacter) {
            IsoGameCharacter chr = (IsoGameCharacter)isoObject;
            return chr.getInventoryWeight();
        }
        isoObject = this.parent;
        if (isoObject instanceof IsoDeadBody) {
            IsoDeadBody deadBody = (IsoDeadBody)isoObject;
            return deadBody.getInventoryWeight();
        }
        return this.getContentsWeight();
    }

    public float getAvailableWeightCapacity() {
        float occupiedWeight = this.getCapacityWeight();
        float maxWeight = this.getMaxWeight();
        return maxWeight - occupiedWeight;
    }

    public boolean isEmpty() {
        return this.items == null || this.items.isEmpty();
    }

    public boolean isEmptyOrUnwanted(IsoPlayer player) {
        if (this.items == null || this.items.isEmpty()) {
            return true;
        }
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryItem item = this.getItems().get(i);
            if (item.isUnwanted(player)) continue;
            return false;
        }
        return true;
    }

    public boolean isMicrowave() {
        return "microwave".equals(this.getType());
    }

    private static boolean isSquareInRoom(IsoGridSquare square) {
        if (square == null) {
            return false;
        }
        return square.getRoom() != null;
    }

    private static boolean isSquarePowered(IsoGridSquare square, boolean includeGenerators) {
        if (square == null) {
            return false;
        }
        boolean bHydroPower = square.hasGridPower();
        if (bHydroPower && square.getRoom() != null) {
            return true;
        }
        if (includeGenerators && square.haveElectricity()) {
            return true;
        }
        if (bHydroPower && square.getRoom() == null) {
            IsoGridSquare sqN = square.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare sqS = square.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare sqW = square.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare sqE = square.getAdjacentSquare(IsoDirections.E);
            if (ItemContainer.isSquareInRoom(sqN) || ItemContainer.isSquareInRoom(sqS) || ItemContainer.isSquareInRoom(sqW) || ItemContainer.isSquareInRoom(sqE)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPowered() {
        return this.parent != null && this.parent.getObjectIndex() != -1 && this.parent.checkObjectPowered();
    }

    public static boolean isObjectPowered(IsoObject parent, boolean includeGenerators) {
        IsoObject object;
        int i;
        if (parent == null || parent.getObjectIndex() == -1) {
            return false;
        }
        ArrayList<IsoObject> tempObjects = s_tempObjects.get();
        parent.getSpriteGridObjects(tempObjects);
        if (tempObjects.isEmpty()) {
            if (parent.getProperties() != null && parent.getProperties().has(IsoPropertyType.STREETLIGHT)) {
                IsoLightSwitch lightSwitch;
                if (parent instanceof IsoLightSwitch && !(lightSwitch = (IsoLightSwitch)parent).isActivated()) {
                    return false;
                }
                return GameTime.getInstance().getNight() >= 0.5f && parent.hasGridPower();
            }
            IsoGridSquare sq = parent.getSquare();
            if (parent.getPipedFuelAmount() > 0) {
                return includeGenerators && sq.haveElectricity() || sq.hasGridPower();
            }
            return ItemContainer.isSquarePowered(sq, includeGenerators);
        }
        IsoLightSwitch lightSwitch = null;
        for (i = 0; i < tempObjects.size(); ++i) {
            IsoLightSwitch lightSwitchObject;
            object = tempObjects.get(i);
            if (!(object instanceof IsoLightSwitch)) continue;
            lightSwitch = lightSwitchObject = (IsoLightSwitch)object;
            break;
        }
        for (i = 0; i < tempObjects.size(); ++i) {
            object = tempObjects.get(i);
            if (object.getProperties() != null && object.getProperties().has(IsoPropertyType.STREETLIGHT)) {
                if (lightSwitch != null && !lightSwitch.isActivated()) {
                    return false;
                }
                return GameTime.getInstance().getNight() >= 0.5f && object.hasGridPower();
            }
            IsoGridSquare testSq = object.getSquare();
            if (object.getPipedFuelAmount() > 0) {
                return includeGenerators && testSq.haveElectricity() || testSq.hasGridPower();
            }
            if (!ItemContainer.isSquarePowered(testSq, includeGenerators)) continue;
            return true;
        }
        return false;
    }

    public float getTemprature() {
        IsoObject isoObject;
        if (this.customTemperature != 0.0f) {
            return this.customTemperature;
        }
        if (this.isPowered() && (this.isFridge() || this.isFreezer())) {
            return 0.2f;
        }
        if (this.isPowered() && (this.isStove() || "microwave".equals(this.type)) && (isoObject = this.parent) instanceof IsoStove) {
            IsoStove isoStove = (IsoStove)isoObject;
            return isoStove.getCurrentTemperature();
        }
        isoObject = this.parent;
        if (isoObject instanceof IsoBarbecue) {
            IsoBarbecue isoBarbecue = (IsoBarbecue)isoObject;
            return isoBarbecue.getTemperature();
        }
        isoObject = this.parent;
        if (isoObject instanceof IsoFireplace) {
            IsoFireplace isoFireplace = (IsoFireplace)isoObject;
            return isoFireplace.getTemperature();
        }
        if ((this.isFridge() || this.isFreezer()) && (float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30)) == (float)SandboxOptions.instance.getElecShutModifier() && GameTime.instance.getTimeOfDay() < 13.0f) {
            float delta = (GameTime.instance.getTimeOfDay() - 7.0f) / 6.0f;
            return GameTime.instance.Lerp(0.2f, 1.0f, delta);
        }
        return 1.0f;
    }

    public boolean isTemperatureChanging() {
        IsoObject isoObject = this.parent;
        if (isoObject instanceof IsoBarbecue) {
            IsoBarbecue bbq = (IsoBarbecue)isoObject;
            return bbq.isTemperatureChanging();
        }
        isoObject = this.parent;
        if (isoObject instanceof IsoFireplace) {
            IsoFireplace fireplace = (IsoFireplace)isoObject;
            return fireplace.isTemperatureChanging();
        }
        isoObject = this.parent;
        if (isoObject instanceof IsoStove) {
            IsoStove stove = (IsoStove)isoObject;
            return stove.isTemperatureChanging();
        }
        return false;
    }

    public ArrayList<InventoryItem> save(ByteBuffer output, IsoGameCharacter noCompress) throws IOException {
        GameWindow.WriteString((ByteBuffer)output, (String)this.type);
        output.put(this.explored ? (byte)1 : 0);
        ArrayList savedItems = CompressIdenticalItems.save((ByteBuffer)output, this.items, null);
        output.put(this.isHasBeenLooted() ? (byte)1 : 0);
        output.putInt(this.capacity);
        return savedItems;
    }

    public ArrayList<InventoryItem> save(ByteBuffer output) throws IOException {
        return this.save(output, null);
    }

    public ArrayList<InventoryItem> load(ByteBuffer input, int worldVersion) throws IOException {
        this.type = GameWindow.ReadString((ByteBuffer)input);
        this.explored = input.get() != 0;
        ArrayList savedItems = CompressIdenticalItems.load((ByteBuffer)input, (int)worldVersion, this.items, this.includingObsoleteItems);
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            item.container = this;
        }
        this.setHasBeenLooted(input.get() != 0);
        this.capacity = input.getInt();
        this.dirty = false;
        return savedItems;
    }

    public boolean isDrawDirty() {
        return this.drawDirty;
    }

    public void setDrawDirty(boolean b) {
        this.drawDirty = b;
    }

    public InventoryItem getBestWeapon(SurvivorDesc desc) {
        InventoryItem best = null;
        float bestscore = -1.0E7f;
        for (int i = 0; i < this.items.size(); ++i) {
            float score;
            InventoryItem item = this.items.get(i);
            if (!(item instanceof HandWeapon) || !((score = item.getScore(desc)) >= bestscore)) continue;
            bestscore = score;
            best = item;
        }
        return best;
    }

    public InventoryItem getBestWeapon() {
        InventoryItem best = null;
        float bestscore = 0.0f;
        for (int i = 0; i < this.items.size(); ++i) {
            float score;
            InventoryItem item = this.items.get(i);
            if (!(item instanceof HandWeapon) || !((score = item.getScore(null)) >= bestscore)) continue;
            bestscore = score;
            best = item;
        }
        return best;
    }

    public float getTotalFoodScore(SurvivorDesc desc) {
        float score = 0.0f;
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!(item instanceof Food)) continue;
            score += item.getScore(desc);
        }
        return score;
    }

    public float getTotalWeaponScore(SurvivorDesc desc) {
        float score = 0.0f;
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!(item instanceof HandWeapon)) continue;
            score += item.getScore(desc);
        }
        return score;
    }

    public InventoryItem getBestFood(SurvivorDesc descriptor) {
        InventoryItem best = null;
        float bestscore = 0.0f;
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!(item instanceof Food)) continue;
            Food food = (Food)item;
            float score = item.getScore(descriptor);
            if (food.isbDangerousUncooked() && !item.isCooked()) {
                score *= 0.2f;
            }
            if (item.age > (float)item.offAge) {
                score *= 0.2f;
            }
            if (!(score >= bestscore)) continue;
            bestscore = score;
            best = item;
        }
        return best;
    }

    public InventoryItem getBestBandage(SurvivorDesc descriptor) {
        InventoryItem best = null;
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!item.isCanBandage()) continue;
            best = item;
            break;
        }
        return best;
    }

    public int getNumItems(String itemLike) {
        int ncount = 0;
        if (itemLike.contains("Type:")) {
            for (int i = 0; i < this.items.size(); ++i) {
                InventoryItem item = this.items.get(i);
                if (item instanceof Food && itemLike.contains("Food")) {
                    ncount += item.getCurrentUses();
                }
                if (!(item instanceof HandWeapon) || !itemLike.contains("Weapon")) continue;
                ncount += item.getCurrentUses();
            }
        } else {
            for (int i = 0; i < this.items.size(); ++i) {
                InventoryItem item = this.items.get(i);
                if (!item.type.equals(itemLike)) continue;
                ncount += item.getCurrentUses();
            }
        }
        return ncount;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isIsDevice() {
        return this.isdevice;
    }

    public void setIsDevice(boolean isDevice) {
        this.isdevice = isDevice;
    }

    public float getAgeFactor() {
        return this.ageFactor;
    }

    public void setAgeFactor(float ageFactor) {
        this.ageFactor = ageFactor;
    }

    public float getCookingFactor() {
        return this.cookingFactor;
    }

    public void setCookingFactor(float cookingFactor) {
        this.cookingFactor = cookingFactor;
    }

    public ArrayList<InventoryItem> getItems() {
        return this.items;
    }

    public void setItems(ArrayList<InventoryItem> items) {
        this.items = items;
    }

    public void takeItemsFrom(ItemContainer other) {
        while (!other.isEmpty()) {
            InventoryItem item = other.getItems().get(0);
            other.Remove(item);
            this.AddItem(item);
        }
    }

    public IsoObject getParent() {
        return this.parent;
    }

    public void setParent(IsoObject parent) {
        this.parent = parent;
    }

    public IsoGridSquare getSourceGrid() {
        return this.sourceGrid;
    }

    public void setSourceGrid(IsoGridSquare sourceGrid) {
        this.sourceGrid = sourceGrid;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void clear() {
        this.items.clear();
        this.dirty = true;
        this.drawDirty = true;
    }

    public int getWaterContainerCount() {
        int c = 0;
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!item.hasComponent(ComponentType.FluidContainer)) continue;
            ++c;
        }
        return c;
    }

    public InventoryItem FindWaterSource() {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!item.isWaterSource()) continue;
            if (item instanceof Drainable) {
                if (item.getCurrentUses() <= 0) continue;
                return item;
            }
            return item;
        }
        return null;
    }

    public ArrayList<InventoryItem> getAllWaterFillables() {
        tempList.clear();
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!item.hasComponent(ComponentType.FluidContainer) || !item.getFluidContainer().isEmpty()) continue;
            tempList.add(item);
        }
        return tempList;
    }

    public InventoryItem getFirstWaterFluidSources(boolean includeTainted) {
        ArrayList<InventoryItem> result = this.getAllWaterFluidSources(includeTainted);
        return result.isEmpty() ? null : result.get(0);
    }

    public InventoryItem getFirstWaterFluidSources(boolean includeTainted, boolean taintedPriority) {
        ArrayList<InventoryItem> result = this.getAllWaterFluidSources(includeTainted);
        if (taintedPriority) {
            for (int i = 0; i < result.size(); ++i) {
                InventoryItem item = result.get(i);
                if (!item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("TaintedWater")) continue;
                return item;
            }
        }
        return result.isEmpty() ? null : result.get(0);
    }

    public InventoryItem getFirstFluidContainer(String type) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!item.hasComponent(ComponentType.FluidContainer)) continue;
            if (item.getFluidContainer().isEmpty()) {
                return item;
            }
            if (StringUtils.isNullOrEmpty((String)type) || item.getFluidContainer().getPrimaryFluid() == null || !item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equalsIgnoreCase(type)) continue;
            return item;
        }
        return null;
    }

    public ArrayList<InventoryItem> getAvailableFluidContainer(String type) {
        tempList.clear();
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!item.hasComponent(ComponentType.FluidContainer)) continue;
            if (item.getFluidContainer().isEmpty()) {
                tempList.add(item);
            }
            if (StringUtils.isNullOrEmpty((String)type) || item.getFluidContainer().getPrimaryFluid() == null || !item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equalsIgnoreCase(type) || item.getFluidContainer().isFull()) continue;
            tempList.add(item);
        }
        return tempList;
    }

    public float getAvailableFluidContainersCapacity(String type) {
        float volume = 0.0f;
        ArrayList<InventoryItem> result = this.getAvailableFluidContainer(type);
        for (InventoryItem item : result) {
            volume += item.getFluidContainer().getFreeCapacity();
        }
        return volume;
    }

    public InventoryItem getFirstAvailableFluidContainer(String type) {
        ArrayList<InventoryItem> result = this.getAvailableFluidContainer(type);
        return result.isEmpty() ? null : result.get(0);
    }

    public ArrayList<InventoryItem> getAllWaterFluidSources(boolean includeTainted) {
        ArrayList<InventoryItem> tempList = new ArrayList<InventoryItem>();
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!item.hasComponent(ComponentType.FluidContainer) || item.getFluidContainer().isEmpty() || !item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("Water") && !item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("CarbonatedWater") && (!includeTainted || !item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("TaintedWater"))) continue;
            tempList.add(item);
        }
        return tempList;
    }

    public InventoryItem getFirstCleaningFluidSources() {
        ArrayList<InventoryItem> result = this.getAllCleaningFluidSources();
        return result.isEmpty() ? null : result.get(0);
    }

    public ArrayList<InventoryItem> getAllCleaningFluidSources() {
        ArrayList<InventoryItem> tempList = new ArrayList<InventoryItem>();
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (!item.hasComponent(ComponentType.FluidContainer) || item.getFluidContainer().isEmpty() || !item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("Bleach") && !item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("CleaningLiquid")) continue;
            tempList.add(item);
        }
        return tempList;
    }

    public int getItemCount(String type) {
        return this.getCountType(type);
    }

    public int getItemCount(ItemKey type) {
        return this.getCountType(type.toString());
    }

    public int getItemCountRecurse(ItemKey type) {
        return this.getItemCountRecurse(type.toString());
    }

    public int getItemCountRecurse(String type) {
        return this.getCountTypeRecurse(type);
    }

    public int getItemCount(String type, boolean doBags) {
        return doBags ? this.getCountTypeRecurse(type) : this.getCountType(type);
    }

    private static int getUses(InventoryItemList items) {
        int count = 0;
        for (int i = 0; i < items.size(); ++i) {
            Object object = items.get(i);
            if (object instanceof DrainableComboItem) {
                DrainableComboItem drainable = (DrainableComboItem)object;
                count += drainable.getCurrentUses();
                continue;
            }
            ++count;
        }
        return count;
    }

    public int getUsesRecurse(Predicate<InventoryItem> predicate) {
        InventoryItemList items = (InventoryItemList)TL_itemListPool.get().alloc();
        this.getAllRecurse(predicate, (ArrayList<InventoryItem>)items);
        int count = ItemContainer.getUses(items);
        TL_itemListPool.get().release(items);
        return count;
    }

    public int getUsesType(String type) {
        InventoryItemList items = (InventoryItemList)TL_itemListPool.get().alloc();
        this.getAllType(type, (ArrayList<InventoryItem>)items);
        int count = ItemContainer.getUses(items);
        TL_itemListPool.get().release(items);
        return count;
    }

    public int getUsesTypeRecurse(String type) {
        InventoryItemList items = (InventoryItemList)TL_itemListPool.get().alloc();
        this.getAllTypeRecurse(type, (ArrayList<InventoryItem>)items);
        int count = ItemContainer.getUses(items);
        TL_itemListPool.get().release(items);
        return count;
    }

    public int getWeightReduction() {
        return this.weightReduction;
    }

    public void setWeightReduction(int weightReduction) {
        weightReduction = Math.min(weightReduction, 100);
        this.weightReduction = weightReduction = Math.max(weightReduction, 0);
    }

    public void removeAllItems() {
        this.drawDirty = true;
        if (this.parent != null) {
            this.dirty = true;
        }
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            item.container = null;
        }
        this.items.clear();
        IsoObject isoObject = this.parent;
        if (isoObject instanceof IsoDeadBody) {
            IsoDeadBody isoDeadBody = (IsoDeadBody)isoObject;
            isoDeadBody.checkClothing(null);
        }
        if ((isoObject = this.parent) instanceof IsoMannequin) {
            IsoMannequin isoMannequin = (IsoMannequin)isoObject;
            isoMannequin.checkClothing(null);
        }
    }

    public boolean containsRecursive(InventoryItem item) {
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryContainer container;
            InventoryItem inventoryItem = this.getItems().get(i);
            if (inventoryItem == item) {
                return true;
            }
            if (!(inventoryItem instanceof InventoryContainer) || !(container = (InventoryContainer)inventoryItem).getInventory().containsRecursive(item)) continue;
            return true;
        }
        return false;
    }

    public int getItemCountFromTypeRecurse(String type) {
        int count = 0;
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryItem inventoryItem = this.getItems().get(i);
            if (inventoryItem.getFullType().equals(type)) {
                ++count;
            }
            if (!(inventoryItem instanceof InventoryContainer)) continue;
            InventoryContainer container = (InventoryContainer)inventoryItem;
            int ocount = container.getInventory().getItemCountFromTypeRecurse(type);
            count += ocount;
        }
        return count;
    }

    public float getCustomTemperature() {
        return this.customTemperature;
    }

    public void setCustomTemperature(float newTemp) {
        this.customTemperature = newTemp;
    }

    public InventoryItem getItemFromType(String type, IsoGameCharacter chr, boolean notEquipped, boolean ignoreBroken, boolean includeInv) {
        InventoryItemList bags = (InventoryItemList)TL_itemListPool.get().alloc();
        if (type.contains(".")) {
            type = type.split("\\.")[1];
        }
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryContainer container;
            InventoryItem item = this.getItems().get(i);
            if (item.getFullType().equals(type) || item.getType().equals(type)) {
                if (notEquipped && chr != null && chr.isEquippedClothing(item) || !this.testBroken(ignoreBroken, item)) continue;
                TL_itemListPool.get().release(bags);
                return item;
            }
            if (!includeInv || !(item instanceof InventoryContainer) || (container = (InventoryContainer)item).getInventory() == null || bags.contains((Object)item)) continue;
            bags.add((Object)item);
        }
        for (int j = 0; j < bags.size(); ++j) {
            ItemContainer container = ((InventoryContainer)bags.get(j)).getInventory();
            InventoryItem item = container.getItemFromType(type, chr, notEquipped, ignoreBroken, includeInv);
            if (item == null) continue;
            TL_itemListPool.get().release(bags);
            return item;
        }
        TL_itemListPool.get().release(bags);
        return null;
    }

    public InventoryItem getItemFromTag(ItemTag itemTag, IsoGameCharacter chr, boolean notEquipped, boolean ignoreBroken, boolean includeInv) {
        InventoryItemList bags = (InventoryItemList)TL_itemListPool.get().alloc();
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryContainer container;
            InventoryItem item = this.getItems().get(i);
            if (item.hasTag(itemTag)) {
                if (notEquipped && chr != null && chr.isEquippedClothing(item) || !this.testBroken(ignoreBroken, item)) continue;
                TL_itemListPool.get().release(bags);
                return item;
            }
            if (!includeInv || !(item instanceof InventoryContainer) || (container = (InventoryContainer)item).getInventory() == null || bags.contains((Object)item)) continue;
            bags.add((Object)item);
        }
        for (int j = 0; j < bags.size(); ++j) {
            ItemContainer container = ((InventoryContainer)bags.get(j)).getInventory();
            InventoryItem item = container.getItemFromTag(itemTag, chr, notEquipped, ignoreBroken, includeInv);
            if (item == null) continue;
            TL_itemListPool.get().release(bags);
            return item;
        }
        TL_itemListPool.get().release(bags);
        return null;
    }

    public InventoryItem getItemFromType(String type, boolean ignoreBroken, boolean includeInv) {
        return this.getItemFromType(type, null, false, ignoreBroken, includeInv);
    }

    public InventoryItem getItemFromTag(ItemTag itemTag, boolean ignoreBroken, boolean includeInv) {
        return this.getItemFromTag(itemTag, null, false, ignoreBroken, includeInv);
    }

    public InventoryItem getItemFromType(String type) {
        return this.getFirstType(type);
    }

    public ArrayList<InventoryItem> getItemsFromType(String type) {
        return this.getAllType(type);
    }

    public ArrayList<InventoryItem> getItemsFromFullType(String type) {
        if (type == null || !type.contains(".")) {
            return new ArrayList<InventoryItem>();
        }
        return this.getAllType(type);
    }

    public ArrayList<InventoryItem> getItemsFromFullType(String type, boolean includeInv) {
        if (type == null || !type.contains(".")) {
            return new ArrayList<InventoryItem>();
        }
        return includeInv ? this.getAllTypeRecurse(type) : this.getAllType(type);
    }

    public ArrayList<InventoryItem> getItemsFromType(String type, boolean includeInv) {
        return includeInv ? this.getAllTypeRecurse(type) : this.getAllType(type);
    }

    public ArrayList<InventoryItem> getItemsFromCategory(String category) {
        return this.getAllCategory(category);
    }

    public void requestSync() {
        if (GameClient.client && (this.parent == null || this.parent.square == null || this.parent.square.chunk == null)) {
            return;
        }
    }

    public void requestServerItemsForContainer() {
        if (this.parent == null || this.parent.square == null) {
            return;
        }
        INetworkPacket.send((PacketTypes.PacketType)PacketTypes.PacketType.RequestItemsForContainer, (Object[])new Object[]{this});
    }

    public InventoryItem getItemWithIDRecursiv(int id) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryContainer container;
            InventoryItem item = this.items.get(i);
            if (item.id == id) {
                return item;
            }
            if (!(item instanceof InventoryContainer) || (container = (InventoryContainer)item).getItemContainer() == null || container.getItemContainer().getItems().isEmpty() || (item = container.getItemContainer().getItemWithIDRecursiv(id)) == null) continue;
            return item;
        }
        return null;
    }

    public InventoryItem getItemWithID(int id) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item.id != id) continue;
            return item;
        }
        return null;
    }

    public boolean removeItemWithID(int id) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item.id != id) continue;
            this.Remove(item);
            return true;
        }
        return false;
    }

    public boolean containsID(int id) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i);
            if (item.id != id) continue;
            return true;
        }
        return false;
    }

    public boolean removeItemWithIDRecurse(int id) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryContainer container;
            InventoryItem item = this.items.get(i);
            if (item.id == id) {
                this.Remove(item);
                return true;
            }
            if (!(item instanceof InventoryContainer) || !(container = (InventoryContainer)item).getInventory().removeItemWithIDRecurse(id)) continue;
            return true;
        }
        return false;
    }

    public boolean isHasBeenLooted() {
        return this.hasBeenLooted;
    }

    public void setHasBeenLooted(boolean hasBeenLooted) {
        this.hasBeenLooted = hasBeenLooted;
    }

    public String getOpenSound() {
        if (this.openSound == null) {
            VehiclePart vehiclePart = this.getVehiclePart();
            if (vehiclePart instanceof VehiclePart) {
                VehiclePart part = vehiclePart;
                return part.getContainerOpenSound();
            }
            IsoObject parent1 = this.getParent();
            if (parent1 == null || parent1.getSprite() == null) {
                return null;
            }
            return parent1.getProperties().get(IsoPropertyType.CONTAINER_OPEN_SOUND);
        }
        return this.openSound;
    }

    public void setOpenSound(String openSound) {
        this.openSound = openSound;
    }

    public String getCloseSound() {
        if (this.closeSound == null) {
            VehiclePart vehiclePart = this.getVehiclePart();
            if (vehiclePart instanceof VehiclePart) {
                VehiclePart part = vehiclePart;
                return part.getContainerCloseSound();
            }
            IsoObject parent1 = this.getParent();
            if (parent1 == null || parent1.getSprite() == null) {
                return null;
            }
            return parent1.getProperties().get(IsoPropertyType.CONTAINER_CLOSE_SOUND);
        }
        return this.closeSound;
    }

    public void setCloseSound(String closeSound) {
        this.closeSound = closeSound;
    }

    public String getPutSound() {
        if (this.putSound == null) {
            VehiclePart vehiclePart = this.getVehiclePart();
            if (vehiclePart instanceof VehiclePart) {
                VehiclePart part = vehiclePart;
                return part.getContainerPutSound();
            }
            IsoObject parent1 = this.getParent();
            if (parent1 == null || parent1.getSprite() == null) {
                return null;
            }
            return parent1.getProperties().get(IsoPropertyType.CONTAINER_PUT_SOUND);
        }
        return this.putSound;
    }

    public void setPutSound(String putSound) {
        this.putSound = putSound;
    }

    public String getTakeSound() {
        if (this.takeSound == null) {
            VehiclePart vehiclePart = this.getVehiclePart();
            if (vehiclePart instanceof VehiclePart) {
                VehiclePart part = vehiclePart;
                return part.getContainerTakeSound();
            }
            IsoObject parent1 = this.getParent();
            if (parent1 == null || parent1.getSprite() == null) {
                return null;
            }
            return parent1.getProperties().get(IsoPropertyType.CONTAINER_TAKE_SOUND);
        }
        return this.takeSound;
    }

    public void setTakeSound(String takeSound) {
        this.takeSound = takeSound;
    }

    public InventoryItem haveThisKeyId(int keyId) {
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryItem item = this.getItems().get(i);
            if (item instanceof Key) {
                Key key = (Key)item;
                if (key.getKeyId() != keyId) continue;
                return key;
            }
            if (!item.getType().equals("KeyRing") && !item.hasTag(ItemTag.KEY_RING) || ((InventoryContainer)item).getInventory().haveThisKeyId(keyId) == null) continue;
            return ((InventoryContainer)item).getInventory().haveThisKeyId(keyId);
        }
        return null;
    }

    public String getOnlyAcceptCategory() {
        return this.onlyAcceptCategory;
    }

    public void setOnlyAcceptCategory(String onlyAcceptCategory) {
        this.onlyAcceptCategory = StringUtils.discardNullOrWhitespace((String)onlyAcceptCategory);
    }

    public String getAcceptItemFunction() {
        return this.acceptItemFunction;
    }

    public void setAcceptItemFunction(String functionName) {
        this.acceptItemFunction = StringUtils.discardNullOrWhitespace((String)functionName);
    }

    public String toString() {
        return "ItemContainer:[type:" + this.getType() + ", parent:" + String.valueOf(this.getParent()) + "]";
    }

    public IsoGameCharacter getCharacter() {
        if (this.getParent() instanceof IsoGameCharacter) {
            return (IsoGameCharacter)this.getParent();
        }
        if (this.containingItem != null && this.containingItem.getContainer() != null) {
            return this.containingItem.getContainer().getCharacter();
        }
        return null;
    }

    public void emptyIt() {
        this.items = new ArrayList();
    }

    public LinkedHashMap<String, InventoryItem> getItems4Admin() {
        LinkedHashMap<String, InventoryItem> items = new LinkedHashMap<String, InventoryItem>();
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryItem item = this.getItems().get(i);
            item.setCount(1);
            if (!(item.isItemType(ItemType.DRAINABLE) || item.isItemType(ItemType.WEAPON) || items.get(item.getFullType()) == null || item instanceof InventoryContainer)) {
                items.get(item.getFullType()).setCount(items.get(item.getFullType()).getCount() + 1);
                continue;
            }
            if (items.get(item.getFullType()) != null) {
                items.put(item.getFullType() + Rand.Next((int)100000), item);
                continue;
            }
            items.put(item.getFullType(), item);
        }
        return items;
    }

    public ArrayList<InventoryItem> getAllFoodsForAnimals() {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (int i = 0; i < this.getItems().size(); ++i) {
            Food food;
            InventoryItem item = this.getItems().get(i);
            if (!item.isAnimalFeed() && (!(item instanceof Food) || ((food = (Food)item).getFoodType() == null || !food.getFoodType().equals("Fruits") && !food.getFoodType().equals("Vegetables")) && food.getMilkType() == null || !(food.getHungerChange() < 0.0f) || food.isRotten() || item.isSpice())) continue;
            result.add(item);
        }
        return result;
    }

    public LinkedHashMap<String, InventoryItem> getAllItems(LinkedHashMap<String, InventoryItem> items, boolean inInv) {
        if (items == null) {
            items = new LinkedHashMap();
        }
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryContainer container;
            InventoryItem item = this.getItems().get(i);
            if (inInv) {
                item.setWorker("inInv");
            }
            item.setCount(1);
            if (!item.isItemType(ItemType.DRAINABLE) && !item.isItemType(ItemType.WEAPON) && items.get(item.getFullType()) != null) {
                items.get(item.getFullType()).setCount(items.get(item.getFullType()).getCount() + 1);
            } else if (items.get(item.getFullType()) != null) {
                items.put(item.getFullType() + Rand.Next((int)100000), item);
            } else {
                items.put(item.getFullType(), item);
            }
            if (!(item instanceof InventoryContainer) || (container = (InventoryContainer)item).getItemContainer() == null || container.getItemContainer().getItems().isEmpty()) continue;
            items = container.getItemContainer().getAllItems(items, true);
        }
        return items;
    }

    @Deprecated
    public InventoryItem getItemById(long id) {
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryContainer container;
            InventoryItem item = this.getItems().get(i);
            if ((long)item.getID() == id) {
                return item;
            }
            if (!(item instanceof InventoryContainer) || (container = (InventoryContainer)item).getItemContainer() == null || container.getItemContainer().getItems().isEmpty() || (item = container.getItemContainer().getItemById(id)) == null) continue;
            return item;
        }
        return null;
    }

    public void addItemsToProcessItems() {
        IsoWorld.instance.currentCell.addToProcessItems(this.items);
    }

    public void removeItemsFromProcessItems() {
        IsoWorld.instance.currentCell.addToProcessItemsRemove(this.items);
        if (!"floor".equals(this.type)) {
            ItemSoundManager.removeItems(this.items);
        }
    }

    public boolean isExistYet() {
        if (!SystemDisabler.doWorldSyncEnable) {
            return true;
        }
        if (this.getCharacter() != null) {
            return true;
        }
        if (this.getVehiclePart() != null) {
            return true;
        }
        if (this.parent instanceof IsoDeadBody) {
            return this.parent.getStaticMovingObjectIndex() != -1;
        }
        if (this.parent instanceof IsoCompost) {
            return this.parent.getObjectIndex() != -1;
        }
        if (this.containingItem != null && this.containingItem.worldItem != null) {
            return this.containingItem.worldItem.getWorldObjectIndex() != -1;
        }
        if (this.getType().equals("floor")) {
            return true;
        }
        if (this.sourceGrid == null) {
            return false;
        }
        IsoGridSquare sq = this.sourceGrid;
        if (!sq.getObjects().contains((Object)this.parent)) {
            return false;
        }
        return this.parent.getContainerIndex(this) != -1;
    }

    public String getContainerPosition() {
        return this.containerPosition;
    }

    public void setContainerPosition(String containerPosition) {
        this.containerPosition = containerPosition;
    }

    public String getFreezerPosition() {
        return this.freezerPosition;
    }

    public void setFreezerPosition(String freezerPosition) {
        this.freezerPosition = freezerPosition;
    }

    public VehiclePart getVehiclePart() {
        return this.vehiclePart;
    }

    public VehiclePartOwner getVehiclePartOwner() {
        return this.isVehiclePart() ? this.getVehiclePart().getOwner() : null;
    }

    public BaseVehicle getVehicle() {
        if (!this.isVehiclePart()) {
            return null;
        }
        VehiclePart part = this.getVehiclePart();
        return part.getVehicle();
    }

    public VehiclePart getVehicleDoorPart() {
        VehicleDoor door;
        if (this.isVehicleSeat()) {
            return this.getVehicleSeatDoorPart();
        }
        VehiclePart part = this.getVehiclePart();
        if (part == null) {
            return null;
        }
        if (part.isVehicleTrunk()) {
            VehicleDoor trunkDoor;
            VehiclePartOwner vehicle = this.getVehiclePartOwner();
            if (vehicle == null) {
                return null;
            }
            VehiclePart trunkDoorPart = vehicle.getTrunkDoorPart();
            if (trunkDoorPart != null && (trunkDoor = trunkDoorPart.getDoor()) != null) {
                return trunkDoorPart;
            }
        }
        if ((door = part.getDoor()) == null) {
            return null;
        }
        return part;
    }

    public VehiclePart getVehicleSeatDoorPart() {
        if (!this.isVehicleSeat()) {
            return null;
        }
        VehiclePart part = this.getVehiclePart();
        int seatNum = part.getContainerSeatNumber();
        if (seatNum < 0) {
            return null;
        }
        BaseVehicle vehicle = this.getVehicle();
        if (vehicle == null) {
            return null;
        }
        return vehicle.getPassengerDoor(seatNum);
    }

    public VehicleDoor getVehicleSeatDoor() {
        VehiclePart seatDoorPart = this.getVehicleSeatDoorPart();
        if (seatDoorPart == null) {
            return null;
        }
        return seatDoorPart.getDoor();
    }

    public VehicleDoor getVehicleDoor() {
        if (this.isVehicleSeat()) {
            return this.getVehicleSeatDoor();
        }
        VehiclePart doorPart = this.getVehicleDoorPart();
        if (doorPart == null) {
            return null;
        }
        return doorPart.getDoor();
    }

    public boolean doesVehicleDoorNeedOpening() {
        VehiclePart doorPart = this.getVehicleDoorPart();
        if (doorPart == null) {
            return false;
        }
        VehicleDoor door = this.getVehicleDoor();
        if (door == null) {
            return false;
        }
        boolean isOpen = door.isOpen();
        return !isOpen;
    }

    public boolean canCharacterOpenVehicleDoor(IsoGameCharacter playerObj) {
        VehiclePart doorPart = this.getVehicleDoorPart();
        if (doorPart == null) {
            return false;
        }
        VehicleDoor door = this.getVehicleDoor();
        if (door == null) {
            return false;
        }
        BaseVehicle vehicle = this.getVehicle();
        if (vehicle == null) {
            return false;
        }
        return vehicle.canOpenDoor(doorPart, playerObj);
    }

    public boolean canCharacterUnlockVehicleDoor(IsoGameCharacter playerObj) {
        VehiclePart doorPart = this.getVehicleDoorPart();
        if (doorPart == null) {
            return false;
        }
        VehicleDoor door = this.getVehicleDoor();
        if (door == null) {
            return false;
        }
        BaseVehicle vehicle = this.getVehicle();
        if (vehicle == null) {
            return false;
        }
        return vehicle.canUnlockDoor(doorPart, playerObj);
    }

    public void reset() {
        for (int i = 0; i < this.getItems().size(); ++i) {
            this.getItems().get(i).reset();
        }
    }

    public ItemContainer getOutermostContainer() {
        InventoryItem containingItem = this.getContainingItem();
        if (containingItem == null) {
            return this;
        }
        ItemContainer outer = containingItem.getContainer();
        if (outer == null) {
            return this;
        }
        return outer.getOutermostContainer();
    }

    public IsoGridSquare getSquare() {
        IsoGridSquare parentSquare;
        IsoGridSquare vehicleSquare;
        BaseVehicle vehicle;
        ItemContainer outerContainer = this.getOutermostContainer();
        if (outerContainer != null && outerContainer != this) {
            return outerContainer.getSquare();
        }
        VehiclePart vehiclePart = this.getVehiclePart();
        if (vehiclePart != null && (vehicle = vehiclePart.getVehicle()) != null && (vehicleSquare = vehicle.getSquare()) != null) {
            return vehicleSquare;
        }
        IsoGridSquare gridSquare = this.getSourceGrid();
        if (gridSquare != null) {
            return gridSquare;
        }
        IsoObject parentObject = this.getParent();
        if (parentObject != null && (parentSquare = parentObject.getSquare()) != null) {
            return parentSquare;
        }
        IsoWorldInventoryObject isoWorldInventoryObject = this.getWorldItem();
        if (isoWorldInventoryObject instanceof IsoWorldInventoryObject) {
            IsoWorldInventoryObject worldItem = isoWorldInventoryObject;
            return worldItem.getSquare();
        }
        return null;
    }

    public IsoWorldInventoryObject getWorldItem() {
        IsoWorldInventoryObject isoWorldInventoryObject;
        if (this.getContainingItem() != null && (isoWorldInventoryObject = this.getContainingItem().getWorldItem()) instanceof IsoWorldInventoryObject) {
            IsoWorldInventoryObject worldItem = isoWorldInventoryObject;
            return worldItem;
        }
        return null;
    }

    public boolean hasWorldItem() {
        return this.getWorldItem() != null;
    }

    public Vector2 getWorldPosition(Vector2 result) {
        String vehiclePartArea;
        Vector2 partPos;
        ItemContainer outermostContainer = this.getOutermostContainer();
        if (outermostContainer != null && outermostContainer != this) {
            return outermostContainer.getWorldPosition(result);
        }
        BaseVehicle vehicle = this.getVehicle();
        VehiclePart vehiclePart = this.getVehiclePart();
        if (vehicle != null && vehiclePart != null && (partPos = vehicle.getAreaFacingPosition(vehiclePartArea = vehiclePart.getArea(), result)) != null) {
            return partPos;
        }
        IsoGridSquare gridSquare = this.getSquare();
        result.set((float)gridSquare.getX(), (float)gridSquare.getY());
        return result;
    }

    public boolean isStove() {
        return "stove".equals(this.type) || "toaster".equals(this.type) || "coffeemaker".equals(this.type);
    }

    public boolean isShop() {
        if (this.getSquare() == null) {
            return false;
        }
        if (this.getSquare().isShop()) {
            return true;
        }
        if (this.getParent() != null && this.getParent().getSquare() != null && this.getParent().getSquare().isShop()) {
            return true;
        }
        if (this.getSquare().getRoom() == null) {
            return false;
        }
        if (this.getSquare().getRoom().isShop()) {
            return true;
        }
        String roomName = this.getSquare().getRoom().getName();
        if (ItemPickerJava.rooms.containsKey((Object)roomName)) {
            ItemPickerJava.ItemPickerRoom roomDist = (ItemPickerJava.ItemPickerRoom)ItemPickerJava.rooms.get((Object)roomName);
            if (roomDist != null && roomDist.isShop) {
                return true;
            }
            ItemPickerJava.ItemPickerContainer containerDist = null;
            if (roomDist.containers.containsKey((Object)this.getType())) {
                containerDist = (ItemPickerJava.ItemPickerContainer)roomDist.containers.get((Object)this.getType());
            }
            if (containerDist == null && roomDist.containers.containsKey((Object)"other")) {
                containerDist = (ItemPickerJava.ItemPickerContainer)roomDist.containers.get((Object)"other");
            }
            if (containerDist == null && roomDist.containers.containsKey((Object)"all")) {
                containerDist = (ItemPickerJava.ItemPickerContainer)roomDist.containers.get((Object)"all");
            }
            if (containerDist != null && containerDist.isShop) {
                return true;
            }
        }
        return false;
    }

    public boolean isCorpse() {
        return this.getType().equals("inventorymale") || this.getType().equals("inventoryfemale");
    }

    public boolean hasRecipe(String recipe, IsoGameCharacter chr) {
        return this.hasRecipe(recipe, chr, true);
    }

    public boolean hasRecipe(String recipe, IsoGameCharacter chr, boolean recursive) {
        IsoPlayer isoPlayer;
        if (chr instanceof IsoPlayer && (isoPlayer = (IsoPlayer)chr).tooDarkToRead()) {
            return false;
        }
        boolean illiterate = chr != null && chr.hasTrait(CharacterTrait.ILLITERATE);
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryContainer cont;
            Literature literature;
            boolean canRead;
            InventoryItem item = this.getItems().get(i);
            boolean bl = canRead = !illiterate || item.hasTag(ItemTag.PICTUREBOOK);
            if (canRead && item instanceof Literature && (literature = (Literature)item).hasRecipe(recipe)) {
                return true;
            }
            if (!recursive || !(item instanceof InventoryContainer) || !(cont = (InventoryContainer)item).getInventory().hasRecipe(recipe, chr, true)) continue;
            return true;
        }
        return false;
    }

    public InventoryItem getRecipeItem(String recipe, IsoGameCharacter chr, boolean recursive) {
        IsoPlayer isoPlayer;
        if (chr instanceof IsoPlayer && (isoPlayer = (IsoPlayer)chr).tooDarkToRead()) {
            return null;
        }
        boolean illiterate = chr != null && chr.hasTrait(CharacterTrait.ILLITERATE);
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryContainer cont;
            Literature literature;
            boolean canRead;
            InventoryItem item = this.getItems().get(i);
            boolean bl = canRead = !illiterate || item.hasTag(ItemTag.PICTUREBOOK);
            if (canRead && item instanceof Literature && (literature = (Literature)item).hasRecipe(recipe)) {
                return item;
            }
            if (!recursive || !(item instanceof InventoryContainer) || !(cont = (InventoryContainer)item).getInventory().hasRecipe(recipe, chr, recursive)) continue;
            return cont.getInventory().getRecipeItem(recipe, chr, recursive);
        }
        return null;
    }

    public void dumpContentsInSquare(IsoGridSquare sq) {
        for (int i = 0; i < this.getItems().size(); ++i) {
            InventoryItem item = this.getItems().get(i);
            this.getItems().remove((Object)item);
            sq.AddWorldInventoryItem(item, 0.0f, 0.0f, 0.0f);
        }
    }

    public String getCustomName() {
        String key = this.getType() + "_customContainerName";
        if (this.getParent().getModData().rawget((Object)key) != null) {
            return (String)this.getParent().getModData().rawget((Object)key);
        }
        return null;
    }

    public void setCustomName(String name) {
        String key = this.getType() + "_customContainerName";
        this.getParent().getModData().rawset((Object)key, (Object)String.valueOf(name));
        INetworkPacket.send((PacketTypes.PacketType)PacketTypes.PacketType.ReceiveContainerModData, (Object[])new Object[]{this});
    }

    public List<InventoryItem> getSoapList(List<InventoryItem> result, boolean includeLiquidSoap) {
        if (result == null) {
            result = new ArrayList<InventoryItem>();
        }
        for (InventoryItem item : this.items) {
            ItemContainer bag;
            float cleanRatio;
            FluidContainer fc;
            if (item.is(ItemKey.Drainable.SOAP_2)) {
                result.add(item);
            } else if (includeLiquidSoap && (fc = item.getFluidContainer()) != null && (cleanRatio = fc.getRatioForFluid(Fluid.CleaningLiquid)) >= 0.499f && (double)fc.getAmount() >= ZomboidGlobals.cleanStainCleaningFluidAmount) {
                result.add(item);
            }
            if (!(item instanceof InventoryContainer)) continue;
            InventoryContainer container = (InventoryContainer)item;
            if (!item.isEquipped() || (bag = container.getInventory()) == null) continue;
            bag.getSoapList(result, includeLiquidSoap);
        }
        return result;
    }

    public boolean isFreezer() {
        if (this.type == null || "none".equals(this.type)) {
            return false;
        }
        return this.type.equals(ContainerType.FREEZER.toString()) || this.type.equals(TYPE_FREEZER);
    }

    public boolean isFridge() {
        if (this.type == null || "none".equals(this.type)) {
            return false;
        }
        if (this.isFreezer()) {
            return false;
        }
        if (this.type.equals(ContainerType.FRIDGE.toString()) || this.type.equals(TYPE_FRIDGE)) {
            return true;
        }
        if (this.parent != null) {
            return this.parent.getProperties() != null && this.parent.getProperties().has(IsoPropertyType.IS_FRIDGE);
        }
        return false;
    }
}
