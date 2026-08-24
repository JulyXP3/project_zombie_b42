package EtherHack.Ether;

import EtherHack.utils.FieldCache;
import EtherHack.utils.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import zombie.characters.SurvivorDesc;
import zombie.characters.skills.PerkFactory;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.scripting.objects.CharacterTrait;

/*
 * 建号增强 (多人建号包注入的载荷改写器)。
 *
 * GamePatcher 在 CreatePlayerPacket.set(byte) 末尾注入对 apply(Object) 的调用:
 * set 是客户端发包前的最终组装点 (survivorDescriptor/characterTraits 都在此填充),
 * 此处改写的数据随原版建号包上传, 服务端 processServer/applyTraits/SurvivorDesc.load
 * 零校验照单全收 (特性无点数上限, xpBoostMap 直采后按 Math.min(10, level) 封顶)。
 *
 * 通道限制: 注入的"全服装"只出服装类物品 —— 服务端 WornItems.load 会对非服装
 * (无 ClothingItem 资产, getVisual() 为 null) 在 tint 赋值处 NPE, 炸掉自己的
 * 建号包, 因此服装清单必须全部是 ClothingItem 类型。
 */
public class CharacterCreationBoost {
    private static final String[] DEFAULT_OUTFIT = new String[]{
        "Base.Hat_BaseballCap", "Base.Jacket_Leather", "Base.Tshirt_ArmyGreen",
        "Base.Trousers", "Base.Socks_Ankle", "Base.Shoes_ArmyBoots"
    };

    public static void apply(Object packet) {
        try {
            EtherAPI api = EtherMain.getInstance().etherAPI;
            if (api == null) {
                return;
            }
            if (!(api.isCharCreateAllTraits || api.isCharCreateMaxSkills || api.isCharCreateClothing)) {
                return;
            }
            Field descField = FieldCache.getField(packet.getClass(), "survivorDescriptor");
            SurvivorDesc desc = (SurvivorDesc)FieldCache.getFieldValue(packet, descField);
            if (desc == null) {
                return;
            }
            if (api.isCharCreateMaxSkills) {
                boostSkills(desc);
            }
            if (api.isCharCreateAllTraits) {
                boostTraits(packet);
            }
            if (api.isCharCreateClothing) {
                boostClothing(desc);
            }
        }
        catch (Throwable t) {
            Logger.printLog("CharacterCreationBoost failed: " + t);
        }
    }

    private static void boostSkills(SurvivorDesc desc) {
        int maxIndex = PerkFactory.Perks.getMaxIndex();
        for (int i = 0; i < maxIndex; ++i) {
            PerkFactory.Perk perk = PerkFactory.Perks.fromIndex(i);
            if (perk == null || perk == PerkFactory.Perks.None || perk == PerkFactory.Perks.MAX) continue;
            // 跳过纯分类节点(Agility/Melee/... parent 为 None), 只保留真实技能;
            // Fitness/Strength 虽也挂在 None 下但applyTraits 会叠加 base 5, 保留
            if (perk.parent == PerkFactory.Perks.None && perk != PerkFactory.Perks.Fitness && perk != PerkFactory.Perks.Strength) continue;
            desc.getXPBoostMap().put(perk, 10);
        }
    }

    private static void boostTraits(Object packet) {
        Field traitsField = FieldCache.getField(packet.getClass(), "characterTraits");
        List<Object> traits = (List<Object>)FieldCache.getFieldValue(packet, traitsField);
        if (traits == null) {
            return;
        }
        for (Object definition : getTraitDefinitions()) {
            try {
                Method getType = definition.getClass().getMethod("getType");
                Method getCost = definition.getClass().getMethod("getCost");
                Method isFree = definition.getClass().getMethod("isFree");
                CharacterTrait trait = (CharacterTrait)getType.invoke(definition);
                if (trait == null || (Integer)getCost.invoke(definition) <= 0 || (Boolean)isFree.invoke(definition)) continue;
                if (!traits.contains(trait)) {
                    traits.add(trait);
                }
            }
            catch (Throwable ignored) {
            }
        }
    }

    private static void boostClothing(SurvivorDesc desc) {
        for (String type : DEFAULT_OUTFIT) {
            InventoryItem item = InventoryItemFactory.CreateItem(type);
            // 只接受带 ClothingItem 资产且有穿戴位的物品, 防止 NPE 炸包
            if (item == null || item.getVisual() == null || item.getBodyLocation() == null) continue;
            desc.getWornItems().setItem(item.getBodyLocation(), item);
        }
    }

    /*
     * CharacterTraitDefinition 的包名在版本间发生过迁移 (42.20.3 在
     * zombie.scripting.objects, 旧快照在 zombie.characters.traits),
     * 双包名反射取 getTraits() 静态方法, 两版 API 同形。
     */
    private static List<Object> getTraitDefinitions() {
        for (String className : new String[]{"zombie.scripting.objects.CharacterTraitDefinition", "zombie.characters.traits.CharacterTraitDefinition"}) {
            try {
                Class<?> cls = Class.forName(className);
                Method getTraits = cls.getMethod("getTraits");
                return (List<Object>)getTraits.invoke(null);
            }
            catch (Throwable ignored) {
            }
        }
        return java.util.Collections.emptyList();
    }
}
