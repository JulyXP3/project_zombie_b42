package modcore;

import modcore.core.GeneratedBundleRopes;
import org.junit.jupiter.api.Test;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BoxPoisonLuaTest {
    @Test void generatedBundleHasRopesAndOnlyThenTheRequestedTargetInTheSentPayload() throws Exception {
        Fixture f = new Fixture(); KahluaTable item = f.add("Base.LogStacks2", 42); f.remember(item);
        f.lua.execute("EtherBoxPoison.trigger('Base.Target',2)");
        assertEquals(List.of("Base.Rope", "Base.Rope", "Base.Target", "Base.Target"), f.lastRopes());
    }
    @Test void oneBasedSparseRopeRecordsArePreservedExactly() throws Exception {
        Fixture f = new Fixture(); KahluaTable item = f.add("Base.LogStacks2", 42);
        KahluaTable ropes = GeneratedBundleRopesTest.table(); ropes.rawset(1,"Base.SheetRope"); ropes.rawset(3,"Base.Rope");
        f.data(item).rawset("ropeItems",ropes); f.lua.execute("EtherBoxPoison.trigger('Base.Target',1)");
        assertEquals(List.of("Base.SheetRope","Base.Rope","Base.Target"),f.lastRopes());
        assertEquals(List.of("Base.SheetRope","Base.Rope"),GeneratedBundleRopesTest.values(ropes));
    }
    @Test void anOrdinaryBundleWithMissingClientRecordsStillPreparesTheTargetAndDefaultRopes() throws Exception {
        Fixture f = new Fixture(); f.add("Base.LogStacks2",42);
        f.lua.execute("EtherBoxPoison.trigger('Base.Target',1)");
        assertEquals(List.of("Base.Rope","Base.Rope","Base.Target"),f.lastRopes());
        assertTrue(f.initialized.isEmpty(),"Unbox preparation must not depend on a generation record");
    }
    @Test void aFailedBoxUploadRestoresTheOriginalRopeTable() throws Exception {
        Fixture f=new Fixture(); KahluaTable item=f.add("Base.LogStacks2",42); KahluaTable ropes=GeneratedBundleRopesTest.table();
        ropes.rawset(0,"Base.Rope"); ropes.rawset(1,"Base.SheetRope"); f.data(item).rawset("ropeItems",ropes); f.failBox=true;
        f.lua.execute("EtherBoxPoison.trigger('Base.Target',1)");
        assertSame(ropes,f.data(item).rawget("ropeItems")); assertTrue(f.boxPackets.isEmpty());
        assertFalse(f.lua.execute("return EtherBoxPoison.message")[0].toString().startsWith("ready:"));
    }
    @Test void unavailableGenerationInitializationDoesNotBlockOrdinaryUnboxPreparation() throws Exception {
        Fixture f=new Fixture(); KahluaTable item=f.add("Base.LogStacks2",42); f.remember(item); f.failInit=true;
        f.lua.execute("EtherBoxPoison.trigger('Base.Target',1)");
        assertEquals(List.of("Base.Rope","Base.Rope","Base.Target"),f.lastRopes());
        assertTrue(f.initialized.isEmpty());
    }
    @Test void failedBoxUploadKeepsTheAlreadySynchronizedGeneratedRopesForRetry() throws Exception {
        Fixture f=new Fixture(); KahluaTable item=f.add("Base.LogStacks2",42);f.remember(item);f.loadWorn();
        f.lua.execute("Events.OnContainerUpdate.handler()");f.failBox=true;
        f.lua.execute("EtherBoxPoison.trigger('Base.Target',1)");
        assertEquals(List.of("Base.Rope","Base.Rope"),GeneratedBundleRopesTest.values(f.data(item).rawget("ropeItems")));
        assertEquals(1,f.initialized.size());assertTrue(f.boxPackets.isEmpty());
        f.failBox=false;f.lua.execute("EtherBoxPoison.trigger('Base.Target',1)");
        assertEquals(1,f.initialized.size());assertEquals(List.of("Base.Rope","Base.Rope","Base.Target"),f.lastRopes());
    }
    @Test void jarBoxPreparationIsUnchangedAndDoesNotInventRopes() throws Exception {
        Fixture f=new Fixture(); f.add("Base.BoxOfJars",42); f.lua.execute("EtherBoxPoison.trigger('Base.Target',2)");
        KahluaTable payload=f.boxPackets.getFirst(); assertEquals(2.0,payload.rawget("numberOfPackedItems"));
        assertEquals("Base.Target",payload.rawget("packedItem0")); assertEquals("Base.Target",payload.rawget("packedItem1"));
        assertNull(payload.rawget("ropeItems"));
    }
    @Test void anInventoryUpdateInitializesAPickedUpGeneratedBundleWithoutOpeningTheBoxPage() throws Exception {
        Fixture f=new Fixture(); KahluaTable item=f.add("Base.LogStacks4",42); f.remember(item); f.loadWorn();
        f.lua.execute("if Events.OnContainerUpdate.handler then Events.OnContainerUpdate.handler() end");
        assertEquals(List.of("Base.Rope","Base.Rope"),GeneratedBundleRopesTest.values(f.data(item).rawget("ropeItems")));
        assertEquals(1,f.initialized.size());
        f.lua.execute("Events.OnContainerUpdate.handler()"); assertEquals(1,f.initialized.size());
    }
    @Test void disconnectClearsGeneratedRecordsInsteadOfReusingThemInAnotherWorld() throws Exception {
        Fixture f=new Fixture(); KahluaTable item=f.add("Base.LogStacks2",42); f.remember(item); f.loadWorn();
        f.lua.execute("for _,handler in ipairs(Events.OnDisconnect.handlers) do handler() end; if Events.OnContainerUpdate.handler then Events.OnContainerUpdate.handler() end");
        assertNull(f.data(item).rawget("ropeItems")); assertTrue(f.initialized.isEmpty());
    }
    @Test void malformedOrOversizedUnpackRecordsAreRejectedWithoutTruncatingThem() throws Exception {
        Fixture f=new Fixture();KahluaTable item=f.add("Base.LogStacks2",42),records=GeneratedBundleRopesTest.table();
        records.rawset(0,"Base.Rope");records.rawset(1,(Object)7.0);f.data(item).rawset("ropeItems",records);
        f.lua.execute("EtherBoxPoison.trigger('Base.Target',1)");assertSame(records,f.data(item).rawget("ropeItems"));assertTrue(f.boxPackets.isEmpty());
        records.rawset(1,"Base.Rope");for(int i=2;i<=256;i++)records.rawset(i,"Base.Rope");
        f.lua.execute("EtherBoxPoison.trigger('Base.Target',1)");assertSame(records,f.data(item).rawget("ropeItems"));assertEquals(257,records.size());assertTrue(f.boxPackets.isEmpty());
    }
    @Test void reloadingBundleHooksDoesNotRemoveForeignHandlersOrDuplicateOurHandler() throws Exception {
        Fixture f=new Fixture();KahluaTable item=f.add("Base.LogStacks2",42);f.remember(item);
        f.lua.execute("foreignCalls=0;Events.OnContainerUpdate.Add(function()foreignCalls=foreignCalls+1 end)");
        f.loadWorn();f.loadWorn();assertEquals(2.0,f.lua.execute("return #Events.OnContainerUpdate.handlers")[0]);
        f.lua.execute("for _,handler in ipairs(Events.OnContainerUpdate.handlers)do handler()end");
        assertEquals(1.0,f.lua.execute("return foreignCalls")[0]);assertEquals(1,f.initialized.size());
        assertEquals(List.of("Base.Rope","Base.Rope"),GeneratedBundleRopesTest.values(f.data(item).rawget("ropeItems")));
    }
    @Test void emptyClientTablesUseTheDefaultMaterialQuantityForAllBundleTypes() throws Exception {
        for(String type:List.of("Base.LogStacks2","Base.LogStacks3","Base.LogStacks4","Base.FirewoodBundle")){
            Fixture f=new Fixture();KahluaTable item=f.add(type,42);f.data(item).rawset("ropeItems",GeneratedBundleRopesTest.table());
            f.lua.execute("EtherBoxPoison.trigger('Base.Target',2)");
            List<String> expected=new ArrayList<>(type.equals("Base.FirewoodBundle")?List.of("Base.Rope"):List.of("Base.Rope","Base.Rope"));expected.add("Base.Target");expected.add("Base.Target");
            assertEquals(expected,f.lastRopes());
        }
    }
    @Test void ordinaryPreparationWorksWithNoGenerationBridgeAndNoTablePairsExtension() throws Exception {
        Fixture f=new Fixture();f.add("Base.LogStacks2",42);
        f.lua.execute("syncGeneratedBundleRopes=nil;table.pairs=nil;EtherBoxPoison.trigger('Base.Target',1)");
        assertEquals(List.of("Base.Rope","Base.Rope","Base.Target"),f.lastRopes());
    }
    @Test void failedDefaultPreparationRestoresTheExactMissingOrEmptyOriginal() throws Exception {
        for(boolean emptyTable:List.of(false,true)){
            Fixture f=new Fixture();KahluaTable item=f.add("Base.LogStacks2",42),original=emptyTable?GeneratedBundleRopesTest.table():null;
            f.data(item).rawset("ropeItems",original);f.failBox=true;f.lua.execute("EtherBoxPoison.trigger('Base.Target',1)");
            assertSame(original,f.data(item).rawget("ropeItems"));assertTrue(f.boxPackets.isEmpty());
            f.failBox=false;f.lua.execute("EtherBoxPoison.trigger('Base.Target',1)");
            assertEquals(List.of("Base.Rope","Base.Rope","Base.Target"),f.lastRopes());
        }
    }
    @Test void existingMaterialsUsePublicPairsEvenWhenTablePairsIsAbsent() throws Exception {
        Fixture f=new Fixture();KahluaTable item=f.add("Base.LogStacks2",42),ropes=GeneratedBundleRopesTest.table();
        ropes.rawset(0,"Base.SheetRope");ropes.rawset(1,"Base.Rope");f.data(item).rawset("ropeItems",ropes);
        f.lua.execute("table.pairs=nil;defaultBundleRopeItems=nil;syncGeneratedBundleRopes=nil;EtherBoxPoison.trigger('Base.Target',1)");
        assertEquals(List.of("Base.SheetRope","Base.Rope","Base.Target"),f.lastRopes());
    }
    @Test void falseValuedMetadataIsNotMistakenForMissingRecordsOrOverwritten() throws Exception {
        Fixture f=new Fixture();KahluaTable item=f.add("Base.LogStacks2",42);f.data(item).rawset("ropeItems",Boolean.FALSE);
        f.lua.execute("EtherBoxPoison.trigger('Base.Target',1)");
        assertTrue(f.boxPackets.isEmpty());assertEquals(Boolean.FALSE,f.data(item).rawget("ropeItems"));
    }
    static class Fixture {
        final LuaTestRuntime lua=new LuaTestRuntime(); final GeneratedBundleRopes registry=new GeneratedBundleRopes();
        final Object connection=new Object(),owner=new Object();
        final List<KahluaTable> items=new ArrayList<>(),boxPackets=new ArrayList<>(),initialized=new ArrayList<>();
        boolean failBox,failInit;
        Fixture() throws Exception {
            lua.execute("""
                    function print() end
                    function getTranslate(key) return key end
                    function isMultiplayer() return true end
                    carriers={}
                    function makeCarrier(fullType,id)
                        local item={fullType=fullType,id=id,data={}}
                        function item:getFullType() return self.fullType end
                        function item:getID() return self.id end
                        function item:getModData() return self.data end
                        table.insert(carriers,item); return item
                    end
                    inventory={}
                    function inventory:getItemsFromType(fullType)
                        local found={}
                        for _,item in ipairs(carriers) do if item.fullType==fullType then table.insert(found,item) end end
                        return {size=function()return #found end,get=function(self,index)return found[index+1]end}
                    end
                    player={getInventory=function()return inventory end}
                    function getPlayer() return player end
                    function event()
                        local e={handlers={}}
                        function e.Add(handler) table.insert(e.handlers,handler);e.handler=handler end
                        function e.Remove(handler) for i,h in ipairs(e.handlers) do if h==handler then table.remove(e.handlers,i);break end end end
                        return e
                    end
                    Events={OnTick=event(),OnContainerUpdate=event(),OnDisconnect=event(),OnMainMenuEnter=event()}
                    function wornSpawnStart()return 'started'end
                    function wornSpawnPoll()return 'waiting'end
                    function wornSpawnRestore()return 'restored'end
                    """);
            lua.environment.rawset("syncGeneratedBundleRopes",(JavaFunction)(frame,n)->{
                if(n>0&&Boolean.TRUE.equals(frame.get(0))){registry.clear();return frame.push(0.0);}
                int result=registry.synchronize(connection,owner,(id,type)->{
                    KahluaTable item=find(id);return item!=null&&type.equals(item.rawget("fullType"))?data(item):null;
                },(id,metadata)->{if(failInit)throw new IllegalStateException("init failure");initialized.add(copy(metadata));});
                return frame.push((double)result);
            });
            lua.environment.rawset("defaultBundleRopeItems",(JavaFunction)(frame,n)->{
                try{return frame.push(GeneratedBundleRopes.class.getMethod("defaultRopeItems",String.class).invoke(null,(String)frame.get(0)));}
                catch(ReflectiveOperationException missing){throw new AssertionError("Shared default rope policy is not implemented",missing);}
            });
            lua.environment.rawset("syncItemFieldsNow",(JavaFunction)(frame,n)->{
                int id=((Number)frame.get(0)).intValue();KahluaTable item=find(id);assertNotNull(item);
                if(failBox)return frame.push("error: write failed");
                boxPackets.add(copy(data(item)));return frame.push("sent fields: "+item.rawget("fullType")+" (#"+id+")");
            });
            lua.resource("modcore/lua/components/ui/EtherBoxPoison.lua");
        }
        KahluaTable add(String type,int id)throws Exception{
            KahluaTable item=(KahluaTable)lua.execute("return makeCarrier('"+type+"',"+id+")")[0];items.add(item);return item;
        }
        void remember(KahluaTable item){registry.remember(connection,owner,((Number)item.rawget("id")).intValue(),(String)item.rawget("fullType"));}
        KahluaTable data(KahluaTable item){return (KahluaTable)item.rawget("data");}
        KahluaTable find(int id){return items.stream().filter(item->((Number)item.rawget("id")).intValue()==id).findFirst().orElse(null);}
        List<String> lastRopes(){assertFalse(boxPackets.isEmpty());return GeneratedBundleRopesTest.values(boxPackets.getLast().rawget("ropeItems"));}
        void loadWorn()throws Exception{lua.resource("modcore/lua/components/ui/EtherWornSpawn.lua");}
        static KahluaTable copy(KahluaTable source){try{ByteBuffer b=ByteBuffer.allocate(32768);source.save(b);b.flip();KahluaTable data=GeneratedBundleRopesTest.table();data.load(b,251);return data;}catch(java.io.IOException e){throw new AssertionError(e);}}
    }
}
