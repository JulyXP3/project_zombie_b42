package modcore;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Exercises the production panel and production scan service together in Kahlua. */
class RadarPanelStatusTest {
    // ---- 新的目标语义: 勾选任一显示 = 追踪清单全部物品, 不再有第三个开关 ----

    @Test
    void watchTrackingButtonIsRemoved() throws Exception {
        Object[] result = runtime().execute("""
                return panel.watchCb == nil, EtherRadarPanel.onWatchToggled == nil,
                    EtherRadarPanel.startWatchTracking == nil, EtherRadarPanel.stopWatchTracking == nil
                """);
        assertArrayEquals(new Object[]{true,true,true,true},result);
    }

    @Test
    void listDrivesTargetsWhenOneDisplayIsEnabled() throws Exception {
        Object[] result = runtime().execute("""
                square(100,100,0).world={floorObject(100,100,0,item("Base.Axe")),
                                         floorObject(100,100,0,item("Base.Plank"))}
                UIMap.drawItems=false; UIMap.drawItemEsp=false
                panel:onEspToggled(true); finish()
                local axe,plank=0,0
                for _,p in ipairs(EtherItemSearch.results or {}) do
                    if p.name=="Base.Axe" then axe=p.count end
                    if p.name=="Base.Plank" then plank=p.count end
                end
                return axe,plank,UIMap.drawItems,UIMap.drawItemEsp,#EtherItemSearch.results
                """);
        assertArrayEquals(new Object[]{1.0,0.0,false,true,1.0},result);
    }

    @Test
    void selectedRowNoLongerOverridesNonEmptyList() throws Exception {
        Object[] result = runtime().execute("""
                square(100,100,0).world={floorObject(100,100,0,item("Base.Axe")),
                                         floorObject(100,100,0,item("Base.Plank"))}
                panel.datas.items={ {item={getFullName=function() return "Base.Axe" end}},
                                    {item={getFullName=function() return "Base.Plank" end}} }
                panel.datas.selected=2
                panel:onShowMapToggled(true); finish()
                local axe,plank=0,0
                for _,p in ipairs(EtherItemSearch.results or {}) do
                    if p.name=="Base.Axe" then axe=p.count end
                    if p.name=="Base.Plank" then plank=p.count end
                end
                return axe,plank
                """);
        assertArrayEquals(new Object[]{1.0,0.0},result);
    }

    @Test
    void emptyListFallsBackToFilteredTargets() throws Exception {
        Object[] result = runtime().execute("""
                EtherWatchlist.load("")
                square(100,100,0).world={floorObject(100,100,0,item("Base.Axe"))}
                panel.datas.items={ {item={getFullName=function() return "Base.Axe" end}} }
                panel.datas.selected=0
                panel:onShowMapToggled(true); finish()
                return #EtherItemSearch.results,UIMap.drawItems
                """);
        assertArrayEquals(new Object[]{1.0,true},result);
    }

    @Test
    void addingToListRefreshesTargetsWhileDisplayStaysOn() throws Exception {
        Object[] result = runtime().execute("""
                square(100,100,0).world={floorObject(100,100,0,item("Base.Axe")),
                                         floorObject(100,100,0,item("Base.Plank"))}
                UIMap.drawItems=true; UIMap.drawItemEsp=false
                panel:onShowMapToggled(true); finish()
                -- Axe and Plank share one square, so aggregation keeps ONE row there; the proof that
                -- targets were refreshed is the per-position count going 1 -> 2.
                local firstCount=EtherItemSearch.results[1].count
                EtherWatchlist.toggle("Base.Plank")
                panel:onWatchlistChanged(); finish()
                return firstCount,#EtherItemSearch.results,EtherItemSearch.results[1].count,UIMap.drawItems
                """);
        assertArrayEquals(new Object[]{1.0,1.0,2.0,true},result);
    }

    @Test
    void clearingListStopsTargetingOldItemsButKeepsDisplayOn() throws Exception {
        Object[] result = runtime().execute("""
                UIMap.drawItems=true; UIMap.drawItemEsp=false
                panel:onShowMapToggled(true); finish()
                EtherWatchlist.clear()
                panel:onWatchlistChanged(); finish()
                return panel.statusText,UIMap.drawItems
                """);
        assertArrayEquals(new Object[]{"清单为空: 先勾选物品并加入清单",true},result);
    }

    @Test
    void oneDisplayDoesNotForceTheOther() throws Exception {
        Object[] result = runtime().execute("""
                UIMap.drawItems=false; UIMap.drawItemEsp=false
                panel:onShowMapToggled(true)
                local mapON, espOFF = UIMap.drawItems and true or false, UIMap.drawItemEsp and true or false
                panel:onShowMapToggled(false)
                panel:onEspToggled(true)
                return mapON, espOFF, UIMap.drawItems, UIMap.drawItemEsp
                """);
        assertArrayEquals(new Object[]{true,false,false,true},result);
    }

    // ---- 独立显示下的结果/失败/超时提示 (原意图保留, 驱动方式改为勾选显示) ----

    @Test
    void espOnlyCompletionIsReported() throws Exception {
        Object[] result = runtime().execute("""
                UIMap.drawItems=false; UIMap.drawItemEsp=false
                panel:onEspToggled(true); finish()
                return panel.statusText,UIMap.drawItems,UIMap.drawItemEsp,#EtherItemSearch.results
                """);
        assertArrayEquals(new Object[]{"未找到该物品",false,true,0.0},result);
    }

    @Test
    void mapOnlyCompletionIsReported() throws Exception {
        Object[] result = runtime().execute("""
                UIMap.drawItems=false; UIMap.drawItemEsp=false
                panel:onShowMapToggled(true); finish()
                return panel.statusText,UIMap.drawItems,UIMap.drawItemEsp,#EtherItemSearch.results
                """);
        assertArrayEquals(new Object[]{"未找到该物品",true,false,0.0},result);
    }

    @Test
    void hitsSurviveTurningOffTheOtherDisplay() throws Exception {
        Object[] result = runtime().execute("""
                square(100,100,0).world={floorObject(100,100,0,item("Base.Axe"))}
                UIMap.drawItems=false; UIMap.drawItemEsp=false
                panel:onShowMapToggled(true)
                panel:onEspToggled(true)
                finish()
                panel:onEspToggled(false)
                return panel.statusText,UIMap.drawItems,UIMap.drawItemEsp,#EtherItemSearch.results,
                    EtherItemSearch.results[1].count
                """);
        assertArrayEquals(new Object[]{"命中",true,false,1.0,1.0},result);
    }

    @Test
    void disablingTheOnlyDisplayBeforeCompletionStillReportsTheResult() throws Exception {
        Object[] result = runtime().execute("""
                UIMap.drawItems=false; UIMap.drawItemEsp=false; clockStep=1
                panel:onShowMapToggled(true)
                panel:onShowMapToggled(false)
                finish()
                return panel.statusText,UIMap.drawItems,UIMap.drawItemEsp
                """);
        assertArrayEquals(new Object[]{"未找到该物品",false,false},result);
    }

    @Test
    void bothDisplaysOffClearTheCompletedStatus() throws Exception {
        Object[] result = runtime().execute("""
                UIMap.drawItems=false; UIMap.drawItemEsp=false
                panel:onShowMapToggled(true)
                panel:onEspToggled(true)
                finish()
                panel:onShowMapToggled(false)
                panel:onEspToggled(false)
                return panel.statusText==nil,UIMap.drawItems,UIMap.drawItemEsp
                """);
        assertArrayEquals(new Object[]{true,false,false},result);
    }

    @Test
    void espOnlyReportsReadFailureInsteadOfNotFound() throws Exception {
        Object[] actual=runtime().execute("""
                local broken=item("Base.Axe");broken.getFullType=function() error("broken getter") end
                square(100,100,0).world={floorObject(100,100,0,broken)}
                UIMap.drawItems=false;UIMap.drawItemEsp=false
                panel:onEspToggled(true);finish()
                return panel.statusText,EtherItemSearch.progress.state,UIMap.drawItems,UIMap.drawItemEsp
                """);
        assertArrayEquals(new Object[]{"扫描失败，已停止；请重新搜索","error",false,true},actual);
    }

    @Test
    void mapOnlyReportsTimeoutInsteadOfNotFound() throws Exception {
        Object[] actual=runtime().execute("""
                clockStep=1;UIMap.drawItems=false;UIMap.drawItemEsp=false
                panel:onShowMapToggled(true);now=now+120001;finish()
                return panel.statusText,EtherItemSearch.progress.state,UIMap.drawItems,UIMap.drawItemEsp
                """);
        assertArrayEquals(new Object[]{"扫描超时，已停止；请重新搜索","timeout",true,false},actual);
    }

    @Test
    void cachedRunningProgressDoesNotReplaceFinalHitMessage() throws Exception {
        Object[] actual=runtime().execute("""
                local items={};for i=1,300 do items[i]=item("Base.Axe") end
                square(100,100,0).objects={containerObject(100,100,0,items)}
                clockStep=1;UIMap.drawItems=false;UIMap.drawItemEsp=false
                panel:onEspToggled(true);tick()
                if type(panel.updateScanStatus)=="function" then panel:updateScanStatus() end
                local state=EtherItemSearch.progress.state;local live=panel.statusText
                finish();if type(panel.updateScanStatus)=="function" then panel:updateScanStatus() end
                return state,string.find(live," | ",1,true)~=nil,panel.statusText
                """);
        assertArrayEquals(new Object[]{"running",true,"命中"},actual);
    }


    @Test
    void layerModeButtonCyclesAndPersists() throws Exception {
        Object[] result = runtime().execute("""
                local seen = {}
                local saved = {}
                setItemScanLayerMode = function(v) saved[#saved+1] = v end
                local startMode = EtherItemSearch.layerMode
                for i = 1, 4 do
                    panel:cycleLayerMode()
                    seen[i] = EtherItemSearch.layerMode
                end
                return startMode, seen[1], seen[2], seen[3], seen[4], #saved, saved[1], saved[2], saved[3]
                """);
        assertArrayEquals(new Object[]{0.0, 1.0, 2.0, 0.0, 1.0, 4.0, 1.0, 2.0, 0.0}, result);
    }

    @Test
    void layerModeStartsAtPersistedAllFloorsWhenNothingWasSaved() throws Exception {
        Object[] result = runtime().execute("""
                local title = EtherRadarPanel.layerModeTitle()
                return EtherItemSearch.layerMode, title, title ~= nil and #title > 0
                """);
        assertArrayEquals(new Object[]{0.0, "全部楼层", true}, result);
    }
    static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua=ItemSearchTestRuntime.create();
        var translations=(se.krka.kahlua.vm.KahluaTable)lua.environment.rawget("translations");
        // Native translations enter Lua as Java strings, not Lua source literals.
        translations.rawset("UI_ItemSearch_NoResults","未找到该物品");
        translations.rawset("UI_ItemSearch_Searching","搜索中…");
        translations.rawset("UI_RadarPanel_StatusHits","命中");
        translations.rawset("UI_RadarPanel_SelectFirst","请先搜索或选择物品");
        translations.rawset("UI_RadarPanel_WatchEmpty","清单为空: 先勾选物品并加入清单");
        translations.rawset("UI_RadarPanel_ScanError","扫描失败，已停止；请重新搜索");
        translations.rawset("UI_RadarPanel_ScanTimeout","扫描超时，已停止；请重新搜索");
        translations.rawset("UI_RadarPanel_ScanProgress","扫描 {phase} | 格 {cells} / 物品 {items} | {seconds}s");
        translations.rawset("UI_RadarPanel_LayerAll","全部楼层");
        translations.rawset("UI_RadarPanel_LayerPM2","本层±2");
        translations.rawset("UI_RadarPanel_LayerCurrent","仅本层");
        lua.resource("modcore/lua/components/ui/EtherWatchlist.lua");
        lua.resource("modcore/lua/components/panels/EtherRadarPanel.lua");
        lua.execute("""
                EtherWatchlist.load("Base.Axe")
                panel=setmetatable({datas={selected=0,items={}}},EtherRadarPanel)
                """);
        return lua;
    }
}
