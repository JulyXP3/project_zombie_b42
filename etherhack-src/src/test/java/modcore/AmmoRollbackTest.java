package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Offline checks for the remaining ammo module, without a world or native fixtures. */
class AmmoRollbackTest {
    @Test
    void requiredModulesNoLongerIncludeLocalShotAccounting() throws Exception {
        var bridge = BytecodeFixture.read("modcore/core/LuaBridge");
        var load = bridge.methods.stream().filter(method -> method.name.equals("loadLua"))
                .findFirst().orElseThrow();
        for (var instruction : load.instructions) {
            if (instruction instanceof org.objectweb.asm.tree.LdcInsnNode constant) {
                assertNotEquals("modcore/lua/fixes/WeaponAmmoAccounting.lua", constant.cst);
            }
        }
    }

    @Test
    void nestedBagFarmRefusesSinglePlayerBeforeAnyAction() throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                isClient = function() return false end
                getTranslate = function(key) return key end
                Events = { OnTick = { Add = function() end } }
                """);
        lua.resource("modcore/lua/components/ui/EtherAmmoFarm.lua");
        lua.execute("""
                local ok = EtherAmmoFarm.startBagFarm()
                assert(ok == false)
                assert(EtherAmmoFarm.message == "UI_AmmoFarm_MPOnly")
                assert(EtherAmmoFarm.bagFarm == nil)
                """);
    }
}
