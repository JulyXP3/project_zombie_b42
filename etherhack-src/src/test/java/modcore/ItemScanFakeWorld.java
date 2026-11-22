package modcore;

import modcore.radar.ItemScanWorld;
import modcore.radar.ItemScanWindow;
import modcore.radar.ItemScanCoverage;
import modcore.radar.ItemScanCoverage.Column;
import modcore.radar.ItemScanCoverage.Pos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** External game-object boundary only: the scanner, index and publication are production code. */
final class ItemScanFakeWorld implements ItemScanWorld {
    static final class Node {
        final String kind;double x,y,z;String full,name;boolean broken;
        Node inventory,item;
        final List<Node> objects=new ArrayList<>(),floor=new ArrayList<>(),moving=new ArrayList<>(),
                containers=new ArrayList<>(),items=new ArrayList<>(),parts=new ArrayList<>();
        Node(String kind,double x,double y,double z){this.kind=kind;this.x=x;this.y=y;this.z=z;}
    }
    Node player=new Node("player",100,100,0);Object cell=new Object();
    final Map<Pos,Node> squares=new LinkedHashMap<>();
    final Map<Long,Column> columns=new LinkedHashMap<>();
    final List<Node> vehicles=new ArrayList<>();
    final Map<Pos,Integer> squareReads=new LinkedHashMap<>();
    int typeReads,bodyChecks;
    ItemScanFakeWorld(){columns.put(ItemScanCoverage.columnKey(12,12),new Column(ItemScanCoverage.mask(0,0),1));}
    Node putSquare(int x,int y,int z){var n=new Node("square",x,y,z);squares.put(new Pos(x,y,z),n);return n;}
    static Node item(String full){var n=new Node("item",0,0,0);n.full=full;n.name=full.substring(full.lastIndexOf('.')+1);return n;}
    static Node inventory(Node... items){var n=new Node("inventory",0,0,0);n.items.addAll(List.of(items));return n;}
    static Node bag(Node... items){var n=item("Base.Bag");n.inventory=inventory(items);return n;}
    static Node object(double x,double y,double z,Node... items){var n=new Node("object",x,y,z);n.containers.add(inventory(items));return n;}
    static Node floor(double x,double y,double z,Node item){var n=new Node("floor",x,y,z);n.item=item;return n;}
    static Node body(double x,double y,double z,Node... items){var n=new Node("corpse",x,y,z);n.inventory=inventory(items);return n;}
    static Node vehicle(double x,double y,double z,Node... items){
        var n=new Node("vehicle",x,y,z);var p=new Node("part",x,y,z);p.inventory=inventory(items);n.parts.add(p);return n;
    }
    public Object player(){return player;} public Object cell(){return cell;}
    public double x(Object n){return ((Node)n).x;} public double y(Object n){return ((Node)n).y;} public double z(Object n){return ((Node)n).z;}
    public Column probeColumn(int x,int y){return columns.getOrDefault(ItemScanCoverage.columnKey(x,y),new Column(0,0));}
    public Object square(int x,int y,int z){var p=new Pos(x,y,z);squareReads.merge(p,1,Integer::sum);return squares.get(p);}
    public List<?> objects(Object n){return ((Node)n).objects;}
    public List<?> floor(Object n){return ((Node)n).floor;}
    public List<?> moving(Object n){return ((Node)n).moving;}
    public boolean isCorpse(Object n){bodyChecks++;return ((Node)n).kind.equals("corpse");}
    public Object corpseContainer(Object n){if(!((Node)n).kind.equals("corpse"))throw new IllegalStateException("not a corpse");return ((Node)n).inventory;}
    public int containerCount(Object n){return ((Node)n).containers.size();}
    public Object container(Object n,int index){return ((Node)n).containers.get(index);}
    public Object floorItem(Object n){return ((Node)n).item;}
    public List<?> items(Object n){return ((Node)n).items;}
    public String fullType(Object n){typeReads++;if(((Node)n).broken)throw new IllegalStateException("broken item");return ((Node)n).full;}
    public String shortType(Object n){String full=((Node)n).full;return full.substring(full.lastIndexOf('.')+1);}
    public String name(Object n){return ((Node)n).name;}
    public boolean isBag(Object n){return ((Node)n).inventory!=null;}
    public Object bag(Object n){return ((Node)n).inventory;}
    public List<?> vehicles(){return vehicles;}
    public int partCount(Object n){return ((Node)n).parts.size();}
    public Object part(Object n,int index){return ((Node)n).parts.get(index);}
    public Object partContainer(Object n){return ((Node)n).inventory;}
    public void retainWindow(ItemScanWindow window){} public void clear(){}
}
