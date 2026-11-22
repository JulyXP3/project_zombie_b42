package modcore;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Every display entry must resume the current list/fallback, including while the panel was paused. */
class RadarTargetLifecycleTest {
    private static final String ITEMS="""
            square(100,100,0).world={floorObject(100,100,0,item("Base.Axe")),floorObject(100,100,0,item("Base.Plank"))}
            UIMap.drawItems=false;UIMap.drawItemEsp=false
            """;
    private static final String FALLBACK="""
            panel.datas={selected=1,items={{item={getFullName=function() return "Base.Plank" end}}}}
            """;
    @Test void changingTheListWhilePausedIsRespectedByTheMapShortcut() throws Exception {
        var lua=RadarPanelStatusTest.runtime();lua.execute(ITEMS);
        Object[] got=lua.execute("""
                panel:onEspToggled(true);finish();panel:onEspToggled(false)
                EtherWatchlist.load("Base.Plank");panel:onWatchlistChanged()
                EtherItemSearch.setMinimapEnabled(true);finish()
                return EtherItemSearch.results[1].name,UIMap.drawItems,UIMap.drawItemEsp
                """);
        assertArrayEquals(new Object[]{"Base.Plank",true,false},got);
    }
    @Test void clearingTheListWhilePausedDoesNotReviveOldTargetsOnAShortcut() throws Exception {
        var lua=RadarPanelStatusTest.runtime();lua.execute(ITEMS);
        Object[] got=lua.execute("""
                panel:onShowMapToggled(true);finish();panel:onShowMapToggled(false)
                EtherWatchlist.clear();panel:onWatchlistChanged()
                EtherItemSearch.setMinimapEnabled(true);finish()
                return EtherItemSearch.results==nil,EtherItemSearch.lastTargets==nil,UIMap.drawItems
                """);
        assertArrayEquals(new Object[]{true,true,true},got);
    }
    @Test void anEmptyPausedListResumesTheSelectedFallbackFromAnEspEntry() throws Exception {
        var lua=RadarPanelStatusTest.runtime();lua.execute(ITEMS);
        lua.execute("panel:onShowMapToggled(true);finish();panel:onShowMapToggled(false)");lua.execute(FALLBACK);
        Object[] got=lua.execute("""
                EtherWatchlist.clear();panel:onWatchlistChanged()
                tick() -- a paused player update must not lose the replacement target
                EtherItemSearch.setItemEspEnabled(true);finish()
                return EtherItemSearch.results[1].name,UIMap.drawItems,UIMap.drawItemEsp
                """);
        assertArrayEquals(new Object[]{"Base.Plank",false,true},got);
    }
    @Test void clearingTheActiveListImmediatelyRunsTheSelectedFallback() throws Exception {
        var lua=RadarPanelStatusTest.runtime();lua.execute(ITEMS);lua.execute("panel:onEspToggled(true);finish()");lua.execute(FALLBACK);
        Object[] got=lua.execute("""
                EtherWatchlist.clear();panel:onWatchlistChanged();finish()
                local row=(EtherItemSearch.results or {})[1]
                return row and row.name or false,UIMap.drawItemEsp
                """);
        assertArrayEquals(new Object[]{"Base.Plank",true},got);
    }
    @Test void aShortcutCanStartTheCurrentListWithoutAnEarlierPanelScan() throws Exception {
        var lua=RadarPanelStatusTest.runtime();lua.execute(ITEMS);
        Object[] got=lua.execute("""
                EtherItemSearch.setItemEspEnabled(true);finish()
                local row=(EtherItemSearch.results or {})[1]
                return row and row.name or false,UIMap.drawItemEsp
                """);
        assertArrayEquals(new Object[]{"Base.Axe",true},got);
    }
    @Test void explicitlyEnablingAnAlreadyEnabledDisplayRetriesAFailedRequest() throws Exception {
        var lua=RadarPanelStatusTest.runtime();
        Object[] got=lua.execute("""
                local value=item("Base.Axe");local original=value.getFullType
                value.getFullType=function() error("temporary unavailable source") end
                square(100,100,0).world={floorObject(100,100,0,value)}
                UIMap.drawItems=false;UIMap.drawItemEsp=false
                panel:onShowMapToggled(true);finish()
                local failed=EtherItemSearch.progress.state
                value.getFullType=original
                EtherItemSearch.setMinimapEnabled(true);finish()
                return failed,EtherItemSearch.progress.state,#(EtherItemSearch.results or {})
                """);
        assertArrayEquals(new Object[]{"error","ready",1.0},got);
    }
}
