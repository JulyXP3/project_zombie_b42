package modcore;

import modcore.core.CoreAPI;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 三百零一 物种多选: 规范化串与筛选判定的纯逻辑 (不依赖游戏世界)。
 * 配置读写由 ConfigUtils/applyConfig 负责, 这里只锁死「全部 / 自定义 / 空选择」三种
 * 语义与逐帧缓存失效 —— 这三处出错的表现分别是: 全部失效、改选不生效、改完仍按旧集合画。
 */
public class AnimalSpeciesFilterTest {

    private static Unsafe unsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    /** 绕开构造器 (CoreAPI 构造会碰游戏 Color/单例); 本测试只碰这两个筛选字段。 */
    private static CoreAPI newApi(boolean all, String csv) throws Exception {
        CoreAPI api = (CoreAPI) unsafe().allocateInstance(CoreAPI.class);
        api.isAnimalSpeciesAll = all;
        api.animalSpeciesFilter = csv;
        return api;
    }

    @Test
    void normalizeTrimsDropsBlanksAndDedupesKeepingOrder() {
        assertEquals("pig,deer", CoreAPI.normalizeAnimalSpeciesFilter(" pig , deer,,pig , "));
        assertEquals("deer", CoreAPI.normalizeAnimalSpeciesFilter("deer"));
        assertEquals("", CoreAPI.normalizeAnimalSpeciesFilter(null));
        assertEquals("", CoreAPI.normalizeAnimalSpeciesFilter(""));
        assertEquals("", CoreAPI.normalizeAnimalSpeciesFilter(" , ,, "));
    }

    @Test
    void allModeShowsEverySpeciesIncludingUnknownAndNull() throws Exception {
        CoreAPI api = newApi(true, "");
        assertTrue(api.isAnimalSpeciesVisible("deer"));
        assertTrue(api.isAnimalSpeciesVisible("some_mod_species"));
        assertTrue(api.isAnimalSpeciesVisible(null));
    }

    @Test
    void customModeOnlyShowsSelectedSpecies() throws Exception {
        CoreAPI api = newApi(false, "deer,pig");
        assertTrue(api.isAnimalSpeciesVisible("deer"));
        assertTrue(api.isAnimalSpeciesVisible("pig"));
        assertFalse(api.isAnimalSpeciesVisible("cow"));
        assertFalse(api.isAnimalSpeciesVisible(null));
    }

    @Test
    void customEmptySelectionShowsNothing() throws Exception {
        CoreAPI api = newApi(false, "");
        assertFalse(api.isAnimalSpeciesVisible("deer"));
        assertFalse(api.isAnimalSpeciesVisible("cow"));
    }

    @Test
    void storedCsvWithSpacesStillMatches() throws Exception {
        CoreAPI api = newApi(false, " deer , pig ");
        assertTrue(api.isAnimalSpeciesVisible("deer"));
        assertTrue(api.isAnimalSpeciesVisible("pig"));
        assertFalse(api.isAnimalSpeciesVisible("deer "));
    }

    @Test
    void selectionCacheFollowsFieldChanges() throws Exception {
        CoreAPI api = newApi(false, "deer");
        assertFalse(api.isAnimalSpeciesVisible("pig"));
        api.animalSpeciesFilter = "deer,pig";
        assertTrue(api.isAnimalSpeciesVisible("pig"));
        api.animalSpeciesFilter = "";
        assertFalse(api.isAnimalSpeciesVisible("deer"));
        api.animalSpeciesFilter = "deer";
        assertTrue(api.isAnimalSpeciesVisible("deer"));
    }

    @Test
    void switchingBackToAllModeIgnoresSelection() throws Exception {
        CoreAPI api = newApi(false, "deer");
        assertFalse(api.isAnimalSpeciesVisible("cow"));
        api.isAnimalSpeciesAll = true;
        assertTrue(api.isAnimalSpeciesVisible("cow"));
    }
}
