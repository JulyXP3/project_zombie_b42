package EtherHack.Ether;

import EtherHack.utils.FieldCache;
import EtherHack.utils.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import zombie.characters.SurvivorDesc;
import zombie.characters.skills.PerkFactory;
import zombie.scripting.objects.CharacterTrait;

/*
 * 建号增强 (多人建号包注入的载荷改写器)。
 *
 * GamePatcher 在 CreatePlayerPacket.set(byte) 末尾注入对 apply(Object) 的调用:
 * set 是客户端发包前的最终组装点 (survivorDescriptor/characterTraits 都在此填充),
 * 此处改写的数据随原版建号包上传, 服务端 processServer/applyTraits/SurvivorDesc.load
 * 零校验照单全收 (特性无点数上限, xpBoostMap 直采后按 Math.min(10, level) 封顶)。
 *
 * 2026-08-26 扩展 (创建角色选项卡):
 *  - 自定义特性名单 (charCreateCustomTraits): 逐项追加进包内 characterTraits;
 *  - 自定义技能等级 (charCreateCustomSkillLevels): xpBoostMap 逐项覆盖,
 *    应用在「建号技能满级」之后 (手工设定优先于一键全满);
 *  - 「解锁全部服装」不再走本类 —— 由 Lua 覆盖建号界面 shouldShowAllOutfits 实现。
 */
public class CharacterCreationBoost {
    public static void apply(Object packet) {
        try {
            EtherAPI api = EtherMain.getInstance().etherAPI;
            if (api == null) {
                return;
            }
            boolean hasCustom = !api.charCreateCustomTraits.isEmpty()
                || !api.charCreateCustomSkillLevels.isEmpty();
            if (!(api.isCharCreateAllTraits || api.isCharCreateMaxSkills || api.isCharCreateAllClothes || hasCustom)) {
                return;
            }
            Field descField = FieldCache.getField(packet.getClass(), "survivorDescriptor");
            SurvivorDesc desc = (SurvivorDesc)FieldCache.getFieldValue(packet, descField);
            if (desc == null) {
                return;
            }
            Logger.printLog("[CreateChar] apply fired: allTraits=" + api.isCharCreateAllTraits
                + " maxSkills=" + api.isCharCreateMaxSkills
                + " customTraits=" + api.charCreateCustomTraits.size()
                + " customSkills=" + api.charCreateCustomSkillLevels.size());
            if (api.isCharCreateMaxSkills) {
                boostSkills(desc);
            }
            if (api.isCharCreateAllTraits) {
                boostTraits(packet);
            }
            if (!api.charCreateCustomTraits.isEmpty()) {
                applyCustomTraits(packet);
                Logger.printLog("[CreateChar] custom traits now: " + fieldTraits(packet));
            }
            if (!api.charCreateCustomSkillLevels.isEmpty()) {
                applyCustomSkillLevels(desc);
                Logger.printLog("[CreateChar] xpBoostMap now: " + desc.getXPBoostMap());
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

    /*
     * 自定义特性名单: 按 CharacterTrait 类型名逐项追加 (contains 判重)。
     */
    private static void applyCustomTraits(Object packet) {
        Field traitsField = FieldCache.getField(packet.getClass(), "characterTraits");
        List<Object> traits = (List<Object>)FieldCache.getFieldValue(packet, traitsField);
        if (traits == null) {
            return;
        }
        EtherAPI api = EtherMain.getInstance().etherAPI;
        for (String typeName : api.charCreateCustomTraits) {
            CharacterTrait trait = findTraitByType(typeName);
            if (trait != null && !traits.contains(trait)) {
                traits.add(trait);
            }
        }
    }

    /*
     * 自定义技能等级: 按枚举名查 Perk (Perks 非枚举, 遍历 fromIndex 比较
     * toString), xpBoostMap 覆盖 (0..10, 服务端再钳)。
     * 在 boostSkills 之后应用 → 手工设定优先于一键全满。
     */
    private static void applyCustomSkillLevels(SurvivorDesc desc) {
        EtherAPI api = EtherMain.getInstance().etherAPI;
        if (api.charCreateCustomSkillLevels.isEmpty()) {
            return;
        }
        int maxIndex = PerkFactory.Perks.getMaxIndex();
        for (int i = 0; i < maxIndex; ++i) {
            PerkFactory.Perk perk = PerkFactory.Perks.fromIndex(i);
            if (perk == null) continue;
            Integer level = api.charCreateCustomSkillLevels.get(perk.getName());
            if (level == null) continue;
            int v = level.intValue();
            if (v < 0) v = 0;
            if (v > 10) v = 10;
            desc.getXPBoostMap().put(perk, v);
        }
    }

    private static String fieldTraits(Object packet) {
        try {
            Field traitsField = FieldCache.getField(packet.getClass(), "characterTraits");
            List<Object> traits = (List<Object>)FieldCache.getFieldValue(packet, traitsField);
            return traits == null ? "null" : String.valueOf(traits);
        }
        catch (Throwable t) {
            return "err:" + t;
        }
    }

    private static CharacterTrait findTraitByType(String typeName) {
        for (Object definition : getTraitDefinitions()) {
            try {
                Method getType = definition.getClass().getMethod("getType");
                CharacterTrait trait = (CharacterTrait)getType.invoke(definition);
                if (trait != null && typeName.equals(trait.getName())) {
                    return trait;
                }
            }
            catch (Throwable ignored) {
            }
        }
        return null;
    }

    /*
     * SP 建号钩子: 单人不走 CreatePlayerPacket (IsoWorld.init 直接
     * new IsoPlayer(luaDesc) + applyTraits(luaTraits)), 包注入在 SP 永远
     * 不会被调 —— 本方法由 GamePatcher 注入 IsoGameCharacter.applyTraits
     * 头部, 就地改写 luaTraits 名单与玩家 descriptor 的 xpBoostMap。
     * MP 下客户端不调 applyTraits (服务端才调, 而 dedicated 服无本 mod),
     * 故此钩子天然只影响 SP。
     */
    public static void applySP(zombie.characters.IsoGameCharacter player, List<CharacterTrait> luaTraits) {
        try {
            EtherAPI api = EtherMain.getInstance().etherAPI;
            if (api == null || luaTraits == null) {
                return;
            }
            if (!(api.isCharCreateAllTraits || api.isCharCreateMaxSkills || hasCustom(api))) {
                return;
            }
            if (!api.charCreateCustomTraits.isEmpty()) {
                for (String typeName : api.charCreateCustomTraits) {
                    CharacterTrait trait = findTraitByType(typeName);
                    if (trait != null && !luaTraits.contains(trait)) {
                        luaTraits.add(trait);
                    }
                }
            }
            SurvivorDesc desc = player.getDescriptor();
            Logger.printLog("[CreateChar][SP] applySP fired: allTraits=" + api.isCharCreateAllTraits
                + " maxSkills=" + api.isCharCreateMaxSkills
                + " customTraits=" + api.charCreateCustomTraits.size()
                + " customSkills=" + api.charCreateCustomSkillLevels.size()
                + " luaTraits=" + luaTraits.size());
            if (desc == null) {
                return;
            }
            if (api.isCharCreateMaxSkills) {
                boostSkills(desc);
            }
            if (!api.charCreateCustomSkillLevels.isEmpty()) {
                applyCustomSkillLevels(desc);
            }
        }
        catch (Throwable t) {
            Logger.printLog("CharacterCreationBoost.applySP failed: " + t);
        }
    }

    private static boolean hasCustom(EtherAPI api) {
        return !api.charCreateCustomTraits.isEmpty() || !api.charCreateCustomSkillLevels.isEmpty();
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
