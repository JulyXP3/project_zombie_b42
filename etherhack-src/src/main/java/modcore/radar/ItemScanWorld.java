package modcore.radar;

import modcore.radar.ItemScanCoverage.Column;
import java.util.List;

/** Read-only live-world boundary. Implementations run on the request's owning thread. */
public interface ItemScanWorld {
    Object player(); Object cell();
    double x(Object position); double y(Object position); double z(Object position);
    Column probeColumn(int chunkX,int chunkY);
    Object square(int x,int y,int z);
    List<?> objects(Object square); List<?> floor(Object square); List<?> moving(Object square);
    boolean isCorpse(Object object); Object corpseContainer(Object corpse);
    int containerCount(Object object); Object container(Object object,int index);
    Object floorItem(Object floorObject);
    List<?> items(Object container);
    String fullType(Object item); String shortType(Object item); String name(Object item);
    boolean isBag(Object item); Object bag(Object item);
    List<?> vehicles(); int partCount(Object vehicle); Object part(Object vehicle,int index); Object partContainer(Object part);
    void retainWindow(ItemScanWindow window); void clear();
}
