package modcore;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** External action bodies mutate the tiny world; the installed observer and scanner are production Lua/Java. */
class ItemSearchSourceTest {
    private static final String TRANSFER="""
            transferCalls=0
            ISTransferAction={transferItem=function(self,character,value,source,target,dropSquare)
                transferCalls=transferCalls+1
                table.remove(source.values,1);table.insert(target.values,value)
                return value
            end}
            """;
    @Test void clientActionCompletionInvalidatesBothContainersWithoutTheSharedHelper() throws Exception {
        var lua=ItemSearchTestRuntime.create("""
                actionCalls=0
                ISInventoryTransferAction={perform=function(self)
                    actionCalls=actionCalls+1
                    table.remove(self.srcContainer.values,1)
                    table.insert(self.destContainer.values,self.item)
                    return "completed"
                end}
                """);
        Object[] got=lua.execute("""
                local value=item("Base.Axe");local from={value};local to={}
                local a=square(101,100,0);local b=square(102,100,0)
                a.objects={containerObject(101,100,0,from)};b.objects={containerObject(102,100,0,to)}
                local source={values=from,getParent=function() return {getSquare=function() return a end} end}
                local target={values=to,getParent=function() return {getSquare=function() return b end} end}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                local returned=ISInventoryTransferAction.perform({item=value,srcContainer=source,destContainer=target})
                now=now+251;tick();finish()
                return returned,actionCalls,hitCount(101,100),hitCount(102,100)
                """);
        assertArrayEquals(new Object[]{"completed",1.0,0.0,1.0},got);
    }
    @Test void aFailedTransferKeepsItsOriginalErrorAndDoesNotInventACompletedChange() throws Exception {
        var lua=ItemSearchTestRuntime.create("""
                actionCalls=0
                ISInventoryTransferAction={perform=function(self)
                    actionCalls=actionCalls+1;actionFailure()
                end}
                originalAction=ISInventoryTransferAction.perform
                """);
        lua.environment.rawset("actionFailure",(se.krka.kahlua.vm.JavaFunction)(frame,count)->{
            throw new RuntimeException("original failure");
        });
        Object[] got=lua.execute("""
                local sq=square(102,100,0);sq.world={floorObject(102,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                local originalOk,originalMessage=pcall(originalAction,{})
                actionCalls=0
                local ok,message=pcall(ISInventoryTransferAction.perform,{srcContainer={getSourceGrid=function() return sq end}})
                return ok,message==originalMessage,actionCalls,hitCount(102,100),EtherItemSearch.refreshPending,originalOk
                """);
        assertArrayEquals(new Object[]{false,true,1.0,1.0,false,false},got);
    }
    @Test void containerEventUsesItsParentWhenSourceGridIsMissing() throws Exception {
        Object[] got=ItemSearchTestRuntime.create().execute("""
                local sq=square(102,100,0)
                sq.world={floorObject(102,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                sq.world={}
                Events.OnContainerUpdate.fire({getSourceGrid=function() return nil end,
                    getParent=function() return {getSquare=function() return sq end} end})
                now=now+251;tick();finish()
                return hitCount(102,100)
                """);
        assertArrayEquals(new Object[]{0.0},got);
    }
    @Test void aGroundBagLootPageResolvesItsContainingWorldItem() throws Exception {
        Object[] got=ItemSearchTestRuntime.create().execute("""
                local values={item("Base.Axe")};local bag=item("Base.Bag",values)
                local sq=square(102,100,0);local placed=floorObject(102,100,0,bag)
                placed.getSquare=function() return sq end;bag.getWorldItem=function() return placed end;sq.world={placed}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                table.remove(values,1)
                Events.OnRefreshInventoryWindowContainers.fire({backpacks={{inventory={
                    getSourceGrid=function() return nil end,getContainingItem=function() return bag end}}}},"end")
                now=now+251;tick();finish()
                return hitCount(102,100)
                """);
        assertArrayEquals(new Object[]{0.0},got);
    }
    @Test void transferIntoAnEquippedBagInvalidatesTheActualOriginWithNoRootCountChange() throws Exception {
        Object[] got=ItemSearchTestRuntime.create(TRANSFER).execute("""
                local value=item("Base.Axe");local values={value};local sq=square(102,100,0)
                sq.objects={containerObject(102,100,0,values)}
                local source={values=values,getParent=function() return {getSquare=function() return sq end} end}
                local target={values={},getParent=function() return player end}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                local returned=ISTransferAction:transferItem(player,value,source,target,nil)
                now=now+251;tick();finish()
                return returned==value,transferCalls,hitCount(102,100),#target.values
                """);
        assertArrayEquals(new Object[]{true,1.0,0.0,1.0},got);
    }
    @Test void aContainerToContainerTransferInvalidatesBothEnds() throws Exception {
        Object[] got=ItemSearchTestRuntime.create(TRANSFER).execute("""
                local value=item("Base.Axe");local from={value};local to={}
                local a=square(101,100,0);local b=square(102,100,0)
                a.objects={containerObject(101,100,0,from)};b.objects={containerObject(102,100,0,to)}
                local source={values=from,getParent=function() return {getSquare=function() return a end} end}
                local target={values=to,getParent=function() return {getSquare=function() return b end} end}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                ISTransferAction:transferItem(player,value,source,target,nil)
                now=now+251;tick();finish()
                return hitCount(101,100),hitCount(102,100),transferCalls
                """);
        assertArrayEquals(new Object[]{0.0,1.0,1.0},got);
    }
    @Test void pickupCapturesTheGroundSquareBeforeTheWorldItemIsDetached() throws Exception {
        var lua=ItemSearchTestRuntime.create("""
                ISGrabItemAction={perform=function(self)
                    self.item.origin.world={};self.item.origin=nil
                    return "kept"
                end}
                """);
        Object[] got=lua.execute("""
                local sq=square(102,100,0);local placed=floorObject(102,100,0,item("Base.Axe"))
                placed.origin=sq;placed.getSquare=function(self) return self.origin end;sq.world={placed}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                local returned=ISGrabItemAction.perform({item=placed,destContainer={getParent=function() return player end}})
                now=now+251;tick();finish()
                return returned,hitCount(102,100)
                """);
        assertArrayEquals(new Object[]{"kept",0.0},got);
    }
    @Test void droppingToAnExplicitSquareInvalidatesThatSquareWithoutAFloorContainerOwner() throws Exception {
        var lua=ItemSearchTestRuntime.create("""
                ISTransferAction={transferItem=function(self,character,value,source,target,dropSquare)
                    table.remove(source.values,1)
                    dropSquare.world={floorObject(dropSquare.x,dropSquare.y,dropSquare.z,value)}
                    return value
                end}
                """);
        Object[] got=lua.execute("""
                local drop=square(102,100,0);local value=item("Base.Axe")
                local source={values={value},getParent=function() return player end}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                ISTransferAction:transferItem(player,value,source,{},drop)
                now=now+251;tick();finish()
                return hitCount(102,100)
                """);
        assertArrayEquals(new Object[]{1.0},got);
    }
    @Test void aCorpseContainerResolvesThroughItsParentWithoutSourceGrid() throws Exception {
        Object[] got=ItemSearchTestRuntime.create(TRANSFER).execute("""
                local value=item("Base.Axe");local values={value};local sq=square(102,100,0)
                local body=bodyObject(102,100,0,values);sq.bodies={body}
                local source={values=values,getParent=function() return body end}
                local target={values={},getParent=function() return player end}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                ISTransferAction:transferItem(player,value,source,target,nil)
                now=now+251;tick();finish()
                return hitCount(102,100),transferCalls
                """);
        assertArrayEquals(new Object[]{0.0,1.0},got);
    }
    @Test void aVehicleTransferNeedsNoStaticSourceSquare() throws Exception {
        Object[] got=ItemSearchTestRuntime.create(TRANSFER).execute("""
                local value=item("Base.Axe");local values={value};local car=vehicle(1,102,100,values)
                vehicles={car}
                local source={values=values,getVehiclePart=function() return car:getPartByIndex(0) end}
                local target={values={},getParent=function() return player end}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                local before=hitCount(102,100)
                ISTransferAction:transferItem(player,value,source,target,nil)
                now=now+251;tick();finish()
                return before,hitCount(102,100),transferCalls
                """);
        assertArrayEquals(new Object[]{1.0,0.0,1.0},got);
    }
    @Test void aBatchedGroundPickupCapturesEveryOriginBeforeDetachment() throws Exception {
        var lua=ItemSearchTestRuntime.create("""
                ISGrabItemAction={perform=function(self)
                    local queued=table.remove(self.queueList,1)
                    for _,placed in ipairs(queued.items) do placed.origin.world={};placed.origin=nil end
                end}
                """);
        Object[] got=lua.execute("""
                local a=square(101,100,0);local b=square(102,100,0)
                local p=floorObject(101,100,0,item("Base.Axe"));local q=floorObject(102,100,0,item("Base.Axe"))
                p.origin=a;q.origin=b
                p.getSquare=function(self) return self.origin end;q.getSquare=function(self) return self.origin end
                a.world={p};b.world={q}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                ISGrabItemAction.perform({item=p,queueList={{items={p,q}}}})
                now=now+251;tick();finish()
                return hitCount(101,100),hitCount(102,100)
                """);
        assertArrayEquals(new Object[]{0.0,0.0},got);
    }
    @Test void moduleReloadDoesNotStackTransferObservers() throws Exception {
        var lua=ItemSearchTestRuntime.create(TRANSFER);
        lua.resource("modcore/lua/components/ui/EtherItemSearch.lua");
        Object[] got=lua.execute("""
                sourceLookups=0
                local value=item("Base.Axe");local values={value};local sq=square(102,100,0)
                sq.objects={containerObject(102,100,0,values)}
                local source={values=values,getParent=function()
                    sourceLookups=sourceLookups+1;return {getSquare=function() return sq end} end}
                local target={values={},getParent=function() return player end}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                local returned=ISTransferAction:transferItem(player,value,source,target,nil)
                now=now+251;tick();finish()
                return returned==value,transferCalls,sourceLookups,hitCount(102,100)
                """);
        assertArrayEquals(new Object[]{true,1.0,2.0,0.0},got);
    }
}
