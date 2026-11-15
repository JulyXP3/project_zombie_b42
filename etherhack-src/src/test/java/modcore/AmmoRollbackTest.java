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
    void fastReloadIsAbsentFromTheRequiredModulesAndResources() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        String removed = "modcore/lua/components/fixes/FastReload.lua";
        try (var stream = loader.getResourceAsStream(removed)) {
            assertNull(stream, "removed reload module must not ship");
        }
        try (var stream = loader.getResourceAsStream("modcore/lua/modcoreMenu.lua")) {
            assertNotNull(stream);
            assertFalse(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains(removed),
                    "startup must not require the removed module");
        }
    }

    @Test
    void fastReloadHasNoRuntimeApiOrConfigurationReadWritePaths() throws Exception {
        var api = BytecodeFixture.read("modcore/core/CoreAPI");
        assertTrue(api.fields.stream().noneMatch(field -> field.name.equals("isFastReload")));
        for (var method : api.methods) {
            if (!java.util.Set.of("<clinit>", "saveConfig", "applyConfig").contains(method.name)) continue;
            for (var instruction : method.instructions) {
                if (instruction instanceof org.objectweb.asm.tree.LdcInsnNode constant) {
                    assertNotEquals("isFastReload", constant.cst, method.name);
                    assertNotEquals("k88", constant.cst, method.name);
                }
            }
        }
        var removed = java.util.Set.of("isFastReload", "toggleFastReload", "fastReloadReady",
                "syncHandWeaponFieldsToServer", "syncItemFieldsToServer", "removeItemFromContainerToServer");
        var methods = BytecodeFixture.read("modcore/core/LuaMethods");
        assertTrue(methods.methods.stream().noneMatch(method -> removed.contains(method.name)));
    }

    @Test
    void nestedBagFarmQueuesVanillaInsertAndEjectWithoutReloadGlobalsOrTransfer() throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                isClient = function() return true end
                getTranslate = function(key) return key end
                tr = function(key) return key end
                Events = { OnTick = { Add = function() end } }
                getTimestampMs = function() return 0 end
                local source = {
                    getID = function() return 11 end,
                    getFullType = function() return "Base.Magazine" end,
                    getCurrentAmmoCount = function() return 30 end,
                }
                local function list(items)
                    return {size=function() return #items end, get=function(self,i) return items[i+1] end}
                end
                bagItems = {source}
                local bag = {getInventory=function() return {getItems=function() return list(bagItems) end} end}
                gun = {
                    clip=false, ammo=0,
                    getID=function() return 10 end,
                    getStringItemType=function() return "RangedWeapon" end,
                    getMagazineType=function() return "Base.Magazine" end,
                    isContainsClip=function(self) return self.clip end,
                    getCurrentAmmoCount=function(self) return self.ammo end,
                }
                local inventory = {
                    getItems=function() return list({gun,bag}) end,
                    getItemById=function(self,id) return id==11 and bagItems[1] or nil end,
                    isFull=function() return false end,
                }
                player = {getInventory=function() return inventory end, getPrimaryHandItem=function() return gun end}
                getPlayer = function() return player end
                instanceof = function(item,kind) return item==bag and kind=="InventoryContainer" end
                actions = {}
                ISTimedActionQueue = {add=function(action) table.insert(actions,action) end}
                ISInsertMagazine = {new=function(self,p,g,m) return {kind="insert",character=p,gun=g,magazine=m} end}
                ISEjectMagazine = {new=function(self,p,g) return {kind="eject",character=p,gun=g} end}
                """);
        lua.resource("modcore/lua/components/ui/EtherAmmoFarm.lua");
        assertArrayEquals(new Object[]{true, true, true, 1.0, "insert", 0.0, 1.0, true, true, true}, lua.execute("""
                local started = EtherAmmoFarm.startBagFarm()
                local first = actions[1]
                EtherAmmoFarm.pollBagFarm()
                local waiting = #actions
                gun.clip=true; gun.ammo=30
                EtherAmmoFarm.pollBagFarm()
                local countBeforeEject = EtherAmmoFarm.bagFarm.count
                gun.clip=false; gun.ammo=0
                EtherAmmoFarm.pollBagFarm()
                return started,first.magazine==bagItems[1],first.fastReloadSkip==nil,waiting,
                    actions[1].kind,countBeforeEject,EtherAmmoFarm.bagFarm.count,
                    actions[2].kind=="eject" and actions[2].gun==gun,
                    actions[3].kind=="insert" and actions[3].magazine==bagItems[1] and actions[3].fastReloadSkip==nil,
                    #bagItems==1 and first.character==player
                """));
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
