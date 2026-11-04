package modcore.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 三百零三 尸体生成多选: 批量参数串解析 (纯逻辑, 不建尸体/不碰游戏世界)。
 * 解析错的表现是"整具尸体失败或数量不对", 且错误原因会直接显示给用户,
 * 所以每条拒绝分支都锁死。
 */
public class CorpseSpawnBatchTest {

    private static void parse(String pairs, List<String> types, List<Integer> counts) {
        CorpseSpawnAPI.parseItemPairs(pairs, types, counts);
    }

    @Test
    void parsesMultipleTypesWithCounts() {
        List<String> types = new ArrayList<String>();
        List<Integer> counts = new ArrayList<Integer>();
        parse("Base.Axe=2,Base.Nails=50", types, counts);
        assertEquals(2, types.size());
        assertEquals("Base.Axe", types.get(0));
        assertEquals("Base.Nails", types.get(1));
        assertEquals(Integer.valueOf(2), counts.get(0));
        assertEquals(Integer.valueOf(50), counts.get(1));
    }

    @Test
    void toleratesSpacesAndEmptySegments() {
        List<String> types = new ArrayList<String>();
        List<Integer> counts = new ArrayList<Integer>();
        parse(" Base.Axe = 2 , ,Base.Nails=3,", types, counts);
        assertEquals(2, types.size());
        assertEquals("Base.Axe", types.get(0));
        assertEquals(Integer.valueOf(2), counts.get(0));
        assertEquals(Integer.valueOf(3), counts.get(1));
    }

    @Test
    void acceptsItemNamesWithDots() {
        List<String> types = new ArrayList<String>();
        List<Integer> counts = new ArrayList<Integer>();
        parse("Base.223Bullet=1", types, counts);
        assertEquals("Base.223Bullet", types.get(0));
    }

    @Test
    void rejectsMissingSeparator() {
        assertThrows(RuntimeException.class, () -> parse("Base.Axe", new ArrayList<String>(), new ArrayList<Integer>()));
    }

    @Test
    void rejectsBadOrOutOfRangeCounts() {
        assertThrows(RuntimeException.class, () -> parse("Base.Axe=abc", new ArrayList<String>(), new ArrayList<Integer>()));
        assertThrows(RuntimeException.class, () -> parse("Base.Axe=0", new ArrayList<String>(), new ArrayList<Integer>()));
        assertThrows(RuntimeException.class, () -> parse("Base.Axe=101", new ArrayList<String>(), new ArrayList<Integer>()));
    }

    @Test
    void rejectsEmptyInput() {
        assertThrows(RuntimeException.class, () -> parse("", new ArrayList<String>(), new ArrayList<Integer>()));
        assertThrows(RuntimeException.class, () -> parse("  , ,", new ArrayList<String>(), new ArrayList<Integer>()));
        assertThrows(RuntimeException.class, () -> parse(null, new ArrayList<String>(), new ArrayList<Integer>()));
    }

    @Test
    void rejectsTotalOverLimit() {
        // 100 + 100 + 100 + 1 = 301 > 300
        assertThrows(RuntimeException.class, () -> parse(
                "Base.A=100,Base.B=100,Base.C=100,Base.D=1",
                new ArrayList<String>(), new ArrayList<Integer>()));
    }

    @Test
    void acceptsExactlyAtTotalLimit() {
        List<String> types = new ArrayList<String>();
        List<Integer> counts = new ArrayList<Integer>();
        parse("Base.A=100,Base.B=100,Base.C=100", types, counts);
        assertEquals(3, types.size());
    }
}
