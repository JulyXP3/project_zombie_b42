package modcore.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoverySelectionTest {
    @Test void eligiblePrimaryHandAvoidsAnyInventoryWalk() {
        Source p=new Source();p.primary="food:hand";FoodRecoverySelection<String,List<String>> s=new FoodRecoverySelection<>(p);
        s.start();assertEquals("food:hand",s.selected());assertEquals(0,p.rootReads);assertEquals(0,p.itemReads);
    }
    @Test void eligibleSecondaryHandWinsOverInventory() {
        Source p=new Source();p.primary="unsafe";p.secondary="food:offhand";FoodRecoverySelection<String,List<String>> s=new FoodRecoverySelection<>(p);
        s.start();assertEquals("food:offhand",s.selected());assertEquals(0,p.rootReads);
    }
    @Test void inventoryIsOnlyWalkedByExplicitBoundedSteps() {
        Source p=new Source();for(int i=0;i<1000;i++)p.root.add("junk:"+i);p.root.add("food:last");
        FoodRecoverySelection<String,List<String>> s=new FoodRecoverySelection<>(p);s.start();assertEquals(0,p.itemReads);
        assertEquals(FoodRecoverySelection.State.SEARCHING,s.state());s.step(128);assertTrue(p.itemReads<=128);assertNull(s.selected());
        while(s.state()==FoodRecoverySelection.State.SEARCHING){int before=p.itemReads;s.step(128);assertTrue(p.itemReads-before<=128);}
        assertEquals("food:last",s.selected());
    }
    @Test void firstEligibleFoodStopsWithoutSortingOrReadingTheRest() {
        Source p=new Source();p.root.addAll(List.of("food:first","food:second","junk"));
        FoodRecoverySelection<String,List<String>> s=new FoodRecoverySelection<>(p);s.start();s.step(128);
        assertEquals("food:first",s.selected());assertEquals(1,p.itemReads);
        s.step(128);assertEquals(1,p.itemReads);
    }
    @Test void nestedInventoryAndCyclesRemainFinite() {
        Source p=new Source();List<String> bag=new ArrayList<>(List.of("back","food:inside"));p.root.add("bag");p.children.put("bag",bag);p.children.put("back",p.root);
        FoodRecoverySelection<String,List<String>> s=new FoodRecoverySelection<>(p);s.start();for(int i=0;i<20&&s.state()==FoodRecoverySelection.State.SEARCHING;i++)s.step(1);
        assertEquals("food:inside",s.selected());assertEquals(3,p.itemReads);
    }
    @Test void emptyContainersAlsoConsumeWorkBudget() {
        Source p=new Source();for(int i=0;i<300;i++){String k="bag:"+i;p.root.add(k);p.children.put(k,new ArrayList<>());}
        FoodRecoverySelection<String,List<String>> s=new FoodRecoverySelection<>(p);s.start();s.step(128);
        int before=p.itemReads;s.step(1);assertTrue(p.itemReads-before<=1);assertEquals(FoodRecoverySelection.State.SEARCHING,s.state());
    }
    @Test void cancellationAndExhaustionHaveNoFutureScans() {
        Source p=new Source();p.root.add("junk");FoodRecoverySelection<String,List<String>> s=new FoodRecoverySelection<>(p);s.start();s.cancel();s.step(128);assertEquals(0,p.itemReads);
        s.start();s.step(128);assertEquals(FoodRecoverySelection.State.EMPTY,s.state());int n=p.itemReads;s.step(128);assertEquals(n,p.itemReads);
    }
    static final class Source implements FoodRecoverySelection.Source<String,List<String>> {
        String primary,secondary;List<String> root=new ArrayList<>();Map<String,List<String>> children=new HashMap<>();int rootReads,itemReads;
        public String primary(){return primary;}public String secondary(){return secondary;}public List<String> root(){rootReads++;return root;}
        public int size(List<String> c){return c.size();}public String item(List<String> c,int i){itemReads++;return c.get(i);}
        public List<String> child(String i){return children.get(i);}public boolean eligible(String i){return i!=null&&i.startsWith("food:");}
    }
}
