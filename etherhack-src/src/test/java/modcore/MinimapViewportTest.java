package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MinimapViewportTest {
    @Test
    void rectangularViewDoesNotDiscardVisibleMarkersPastTheShortEdgeRadius() throws Exception {
        Object[] result = runtime().execute("""
                local view=UIMap:new(0,0,600,200)
                zombies={actor(330,100)}
                UIMap.drawZombies=true
                Events.OnPreUIDraw.fire()
                view:render()
                return #view.rects,view.rects[1] and view.rects[1].x or -1
                """);
        assertArrayEquals(new Object[]{1.0, 525.0}, result);
    }

    @Test
    void draggingAwayFromThePlayerUsesTheCameraViewportForCulling() throws Exception {
        Object[] result = runtime().execute("""
                local view=UIMap:new(0,0,400,200)
                view.mapAPI:centerOn(1000,1000)
                zombies={actor(1000,1000),actor(100,100)}
                UIMap.drawZombies=true
                Events.OnPreUIDraw.fire()
                view:render()
                return #view.rects,view.rects[1] and view.rects[1].x or -1,
                    view.rects[1] and view.rects[1].y or -1
                """);
        assertArrayEquals(new Object[]{1.0, 195.0, 95.0}, result);
    }

    @Test
    void isometricCornersAreCoveredButOffscreenPointsAreStillRejected() throws Exception {
        Object[] result = runtime().execute("""
                local view=UIMap:new(0,0,600,200)
                view.mapAPI.iso=true
                zombies={actor(245, -45),actor(250,250)}
                UIMap.drawZombies=true
                Events.OnPreUIDraw.fire()
                view:render()
                return #view.rects,view.rects[1] and view.rects[1].x or -1,
                    view.rects[1] and view.rects[1].y or -1
                """);
        assertArrayEquals(new Object[]{1.0, 585.0, 95.0}, result);
    }

    @Test
    void cameraIsPreparedBeforeTheBaseMapAndDraggingKeepsTheOldRecenterRule() throws Exception {
        Object[] result = runtime().execute("""
                local view=UIMap:new(0,0,280,250)
                view.centerByPlayer=true
                UIMovableMiniMap={ instance={ map=view } }
                player.x=120
                player.y=130
                Events.OnPreUIDraw.fire()
                local x,y=view.mapAPI.cx,view.mapAPI.cy
                local calls=view.mapAPI.centers
                view:prerender()
                local late=view.mapAPI.centers
                view.dragging=true
                view.mapAPI:centerOn(1000,1000)
                Events.OnPreUIDraw.fire()
                local held=view.mapAPI.cx
                view:onMouseUp(0,0)
                Events.OnPreUIDraw.fire()
                return x,y,calls,late,held,view.mapAPI.cx,view.mapAPI.cy
                """);
        assertArrayEquals(new Object[]{120.0, 130.0, 1.0, 1.0, 1000.0, 120.0, 130.0}, result);
    }

    @Test
    void hiddenParentAndClosedMapDoNotPrepareTheirCameras() throws Exception {
        Object[] result = runtime().execute("""
                local parent=ISPanel:new(0,0,400,300)
                parent.visible=false
                local view=UIMap:new(0,0,280,250)
                view.centerByPlayer=true
                UIMovableMiniMap={ instance={ map=view } }
                parent:addChild(view)
                Events.OnPreUIDraw.fire()
                local hidden=view.mapAPI.centers
                parent.visible=true
                Events.OnPreUIDraw.fire()
                local shown=view.mapAPI.centers
                view.removed=true
                Events.OnPreUIDraw.fire()
                return hidden,shown,view.mapAPI.centers
                """);
        assertArrayEquals(new Object[]{0.0, 1.0, 1.0}, result);
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = MinimapTestRuntime.create();
        lua.execute("""
                local originalNew=UIMap.new
                function UIMap:new(x,y,w,h)
                    local view=originalNew(self,x,y,w,h)
                    UIMovableMiniMap={ instance={ map=view } }
                    return view
                end
                function mapCollectFrame(buffer,flags)
                    buffer.zombies={}
                    for _, z in ipairs(zombies) do
                        buffer.zombies[#buffer.zombies+1]=z.x
                        buffer.zombies[#buffer.zombies+1]=z.y
                    end
                    buffer.vehicles={}; buffer.players={}
                    return true
                end
                """);
        return lua;
    }
}
