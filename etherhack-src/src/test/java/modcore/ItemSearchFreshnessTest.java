package modcore;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Production controller + production Java bridge. Only external world/time boundaries are supplied. */
class ItemSearchFreshnessTest {
    @Test void duePatrolMustCarryAChangeStillInsideItsDebounce() throws Exception {
        Object[] got=ItemSearchTestRuntime.create().execute("""
                local sq=square(102,100,0)
                sq.world={floorObject(102,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                EtherItemSearch.patrolMs=1000;EtherItemSearch.patrolCells=1
                now=now+1001;sq.world={};Events.OnContainerUpdate.fire(sq)
                tick();finish();now=now+251;tick();finish()
                return hitCount(102,100),EtherItemSearch.refreshPending
                """);
        assertArrayEquals(new Object[]{0.0,false},got);
    }
    @Test void replacingAnUncommittedMoveReplaysItsDetachedDirtyBatch() throws Exception {
        Object[] got=ItemSearchTestRuntime.create().execute("""
                local sq=square(100,100,0)
                sq.world={floorObject(100,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                now=now+1001;sq.world={};Events.OnContainerUpdate.fire(sq)
                player.x=96;now=now+251;clockStep=1;tick()
                local first=EtherItemSearch._cursor~=nil
                -- The uncommitted request is centred on 96; moving to 101 crosses its 2R
                -- limit, while 101 still overlaps the LAST COMMITTED window centred on 100.
                player.x=101;tick();clockStep=0;finish()
                return first,hitCount(100,100),EtherItemSearch._bounds.cx
                """);
        assertArrayEquals(new Object[]{true,0.0,101.0},got);
    }
    @Test void anotherEventForTheSameSquareDuringARequestSurvivesItsReadyCommit() throws Exception {
        Object[] got=ItemSearchTestRuntime.create().execute("""
                local sq=square(98,98,-1)
                sq.world={floorObject(98,98,-1,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                Events.OnContainerUpdate.fire(sq);now=now+251;clockStep=1;tick()
                sq.world={};Events.OnContainerUpdate.fire(sq)
                finish();clockStep=0;now=now+251;tick();finish()
                return hitCount(98,98),EtherItemSearch.refreshPending
                """);
        assertArrayEquals(new Object[]{0.0,false},got);
    }
    @Test void failedRefreshWithdrawsTheActiveMarkersButDoesNotMutateTheOldSnapshot() throws Exception {
        Object[] got=ItemSearchTestRuntime.create().execute("""
                local value=item("Base.Axe");local sq=square(100,100,0)
                sq.world={floorObject(100,100,0,value)}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                local old=EtherItemSearch.results
                value.getFullType=function() error("bad live item") end
                Events.OnContainerUpdate.fire(sq);now=now+251;tick();finish()
                return EtherItemSearch.results==nil,#old,old[1].count,EtherItemSearch.progress.state
                """);
        assertArrayEquals(new Object[]{true,1.0,1.0,"error"},got);
    }
    @Test void aDuePatrolIsCarriedByAContinuousStreamOfDirtyRequests() throws Exception {
        Object[] got=ItemSearchTestRuntime.create().execute("""
                EtherItemSearch.radius=12
                local known=square(102,102,0)
                known.world={floorObject(102,102,0,item("Base.Axe"))}
                square(100,100,0)
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                known.world={};EtherItemSearch.patrolMs=1000;EtherItemSearch.patrolCells=8
                for i=1,8 do
                    Events.OnContainerUpdate.fire(squares[key(100,100,0)])
                    now=now+251;tick();finish()
                end
                return hitCount(102,102)
                """);
        assertArrayEquals(new Object[]{0.0},got);
    }
    @Test void timedOutRefreshWithdrawsTheActiveMarkers() throws Exception {
        Object[] got=ItemSearchTestRuntime.create().execute("""
                square(100,100,0).world={floorObject(100,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                local old=EtherItemSearch.results
                clockStep=1;EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                now=now+120001;tick()
                return EtherItemSearch.results==nil,#old,EtherItemSearch.progress.state
                """);
        assertArrayEquals(new Object[]{true,1.0,"timeout"},got);
    }
}
