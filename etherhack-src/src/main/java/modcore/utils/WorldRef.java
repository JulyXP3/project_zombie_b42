package modcore.utils;

import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;

/**
 * D4 (2026-09-13, 见 analysis/DLL分析/D-容器与物品-设计方案(待实施).md):
 * 世界对象/容器的**引用稳定性校验** —— 批量操作(或被索引寻址的网络命令)发出前,
 * 确认 (格坐标 + 对象 index + 容器 index) 仍指向同一个对象/容器。
 *
 * 语义照同事 DLL 的 ContainerJobs.resolve(): 格一致 -> 对象 index 有效 ->
 * 对象确实在该格上 -> 容器 index 有效 -> 容器确实属于该对象且反查 index 一致。
 * index 型操作(如 object.clearContainerExplore 的 index/containerIndex)在目标
 * 加载/卸载之间会错位, 校验失败时调用方应跳过该项而不是照发。
 */
public final class WorldRef {

    private WorldRef() {
    }

    /** 对象引用是否有效: (x,y,z) 上的第 objectIndex 个对象仍在, 且属于该格。 */
    @LuaMethod(name = "refIsValidObject", global = true)
    public static boolean isValidObjectRef(int x, int y, int z, int objectIndex) {
        return pickObject(x, y, z, objectIndex) != null;
    }

    /** 容器引用是否有效: 在上面的基础上, 再确认 containerIndex 指向该对象的同一容器。 */
    @LuaMethod(name = "refIsValidContainer", global = true)
    public static boolean isValidContainerRef(int x, int y, int z, int objectIndex, int containerIndex) {
        IsoObject obj = pickObject(x, y, z, objectIndex);
        if (obj == null) {
            return false;
        }
        if (containerIndex < 0 || containerIndex > Short.MAX_VALUE) {
            return false;
        }
        if (obj.getContainerCount() <= containerIndex) {
            return false;
        }
        ItemContainer container = obj.getContainerByIndex(containerIndex);
        if (container == null || container.getParent() != obj) {
            return false;
        }
        return obj.getContainerIndex(container) == containerIndex;
    }

    private static IsoObject pickObject(int x, int y, int z, int objectIndex) {
        if (IsoWorld.instance == null || IsoWorld.instance.getCell() == null) {
            return null;
        }
        if (objectIndex < 0 || objectIndex > Short.MAX_VALUE) {
            return null;
        }
        IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare(x, y, z);
        if (square == null) {
            return null;
        }
        if (objectIndex >= square.getObjects().size()) {
            return null;
        }
        IsoObject object = (IsoObject) square.getObjects().get(objectIndex);
        if (object == null || object.getSquare() != square) {
            return null;
        }
        return object;
    }
}
