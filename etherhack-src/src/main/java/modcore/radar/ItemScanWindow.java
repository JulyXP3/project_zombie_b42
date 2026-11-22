package modcore.radar;

import java.util.Objects;

/** Immutable horizontal window; player floor is intentionally not part of coverage. */
public final class ItemScanWindow {
    private final int cx,cy,radius,x1,x2,y1,y2;
    private final long area;
    private ItemScanWindow(int cx,int cy,int radius){
        if(radius<0)throw new IllegalArgumentException("negative scan radius");
        long loX=(long)cx-radius,hiX=(long)cx+radius,loY=(long)cy-radius,hiY=(long)cy+radius;
        if(loX<Integer.MIN_VALUE||hiX>Integer.MAX_VALUE||loY<Integer.MIN_VALUE||hiY>Integer.MAX_VALUE)
            throw new IllegalArgumentException("scan window overflow");
        long side=2L*radius+1;
        try{area=Math.multiplyExact(side,side);Math.multiplyExact(area,64L);}
        catch(ArithmeticException ex){throw new IllegalArgumentException("scan area overflow",ex);}
        this.cx=cx;this.cy=cy;this.radius=radius;x1=(int)loX;x2=(int)hiX;y1=(int)loY;y2=(int)hiY;
    }
    public static ItemScanWindow around(double x,double y,int radius){
        double fx=Math.floor(x),fy=Math.floor(y);
        if(!Double.isFinite(fx)||!Double.isFinite(fy)||fx<Integer.MIN_VALUE||fx>Integer.MAX_VALUE||
                fy<Integer.MIN_VALUE||fy>Integer.MAX_VALUE)throw new IllegalArgumentException("invalid player position");
        return new ItemScanWindow((int)fx,(int)fy,radius);
    }
    public int cx(){return cx;} public int cy(){return cy;} public int radius(){return radius;}
    public int x1(){return x1;} public int x2(){return x2;} public int y1(){return y1;} public int y2(){return y2;}
    public int minChunkX(){return Math.floorDiv(x1,8);} public int maxChunkX(){return Math.floorDiv(x2,8);}
    public int minChunkY(){return Math.floorDiv(y1,8);} public int maxChunkY(){return Math.floorDiv(y2,8);}
    public boolean contains(int x,int y){return x>=x1&&x<=x2&&y>=y1&&y<=y2;}
    public long area(){return area;}
    public long fullOrder(ItemScanCoverage.Pos p){
        return (p.z()+32L)*area+((long)p.x()-x1)*(y2-y1+1L)+((long)p.y()-y1);
    }
    @Override public boolean equals(Object other){return other instanceof ItemScanWindow w&&cx==w.cx&&cy==w.cy&&radius==w.radius;}
    @Override public int hashCode(){return Objects.hash(cx,cy,radius);}
}
