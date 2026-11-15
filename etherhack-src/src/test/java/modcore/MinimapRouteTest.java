package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MinimapRouteTest {
    @Test
    void routeUsesOneSegmentBatchPerMapAndOneSharedReadPerUiFrame() throws Exception {
        Object[] result = runtime().execute("""
                local small=UIMap:new(10,20,200,100)
                local large=UIMap:new(30,40,200,100)
                UIMovableMiniMap={ instance={ map=small } }
                EtherMain={ instance={ buttonsPanel={ currentPanel={ map=large } } } }
                Events.OnPreUIDraw.fire()
                small:render()
                large:render()
                return batches,routeReads,#small.rects,#large.rects,
                    lastBatch and lastBatch[1] or -1,lastBatch and lastBatch[2] or -1,
                    lastBatch and lastBatch[3] or -1,lastBatch and lastBatch[4] or -1,
                    lastR or -1,lastG or -1,lastB or -1
                """);
        assertArrayEquals(new Object[]{2.0, 6.0, 0.0, 0.0,
                -100.0, 50.0, 300.0, 50.0, 0.35, 0.55, 1.0}, result);
    }

    @Test
    void vanillaWorldMapStillDrawsTheRouteAndReloadDoesNotDoubleDraw() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.resource("modcore/lua/components/drive/AutoDriveMap.lua");
        Object[] result = lua.execute("""
                local world=ISWorldMap:new(0,0,200,100)
                Events.OnPreUIDraw.fire()
                world:render()
                return batches,routeReads,#world.rects
                """);
        assertArrayEquals(new Object[]{1.0, 6.0, 0.0}, result);
    }

    @Test
    void destinationMarkerAndRouteShareTheMapStencil() throws Exception {
        Object[] result = runtime().execute("""
                local world=ISWorldMap:new(0,0,200,100)
                function autoDriveGetTargetX() return 100 end
                function autoDriveGetTargetY() return 100 end
                stencilDepth=0
                outside=false
                function ISPanel:clampStencilRectToParent() stencilDepth=stencilDepth+1 end
                function ISPanel:clearStencilRect() stencilDepth=stencilDepth-1 end
                local drawRect=ISPanel.drawRect
                function ISPanel:drawRect(...)
                    if stencilDepth < 1 then outside=true end
                    drawRect(self,...)
                end
                local drawSegments=mapDrawSegments
                function mapDrawSegments(...)
                    if stencilDepth < 1 then outside=true end
                    drawSegments(...)
                end
                Events.OnPreUIDraw.fire()
                world:render()
                return outside,stencilDepth,#world.rects,batches
                """);
        assertArrayEquals(new Object[]{false, 0.0, 2.0, 1.0}, result);
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = MinimapTestRuntime.create();
        lua.execute("""
                routeCount=3
                local points={{-100,100},{300,100},{500,100}}
                routeReads=0; batches=0
                function autoDriveGetRouteX(i) routeReads=routeReads+1; return points[i+1][1] end
                function autoDriveGetRouteY(i) routeReads=routeReads+1; return points[i+1][2] end
                function autoDriveGetRouteKind() return "roadwm" end
                function autoDriveGetTargetX() return 500 end
                function autoDriveGetTargetY() return 100 end
                function mapDrawSegments(data,n,ox,oy,w,h,r,g,b,a)
                    batches=batches+1
                    lastBatch=data
                    lastR,lastG,lastB=r,g,b
                end
                """);
        return lua;
    }
}
