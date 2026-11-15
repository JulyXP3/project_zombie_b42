--*********************************************************
--* MapLayers: 世界快照共享, 投影/颜色/剔除属于每个地图视口。
--* 保留各图层尺寸、标签缩放门限与原版 stencil 绘制。
--*********************************************************
MapLayers = {};

local function marker(view, c, wx, wy, color, label, minScale)
    local x, y = MapViewport.project(c, wx, wy);
    if x == nil then return; end
    local size = c.size;
    view:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1,
        color.a, color.r, color.g, color.b);
    view:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
    if label ~= nil and label ~= "" and c.scale > minScale then
        view:drawTextCentre(label, x + 1, y + 6, 0, 0, 0, 1, UIFont.Small);
        view:drawTextCentre(label, x, y + 5, 1, 1, 1, 1, UIFont.Small);
    end
    return x, y;
end

local ITEM_COLOR = { r = 0.75, g = 0.75, b = 0.75, a = 1 };

function MapLayers.render(view, c, frame, available)
    if available then
        if UIMap.drawZombies then
            for i = 1, #frame.zombies, 2 do
                marker(view, c, frame.zombies[i], frame.zombies[i + 1], view.zombieColor);
            end
        end
        if UIMap.drawVehicles then
            for i = 1, #frame.vehicles, 3 do
                marker(view, c, frame.vehicles[i], frame.vehicles[i + 1], view.vehicleColor,
                    frame.vehicles[i + 2], 5);
            end
        end
        if UIMap.drawAllPlayers then
            for i = 1, #frame.remote, 3 do
                marker(view, c, frame.remote[i], frame.remote[i + 1], view.playerColor,
                    frame.remote[i + 2], 1);
            end
        end
        if UIMap.drawLocalPlayer and frame.localX ~= nil then
            marker(view, c, frame.localX, frame.localY, view.localPlayerColor, frame.localName, 1);
        end
    end
    if UIMap.drawItems and EtherItemSearch ~= nil and EtherItemSearch.results ~= nil then
        for _, item in pairs(EtherItemSearch.results) do
            local x, y = marker(view, c, item.x, item.y, ITEM_COLOR);
            if x ~= nil and item.count > 1 then
                view:drawTextCentre(tostring(item.count), x, y + c.size + 2, 0.75, 0.75, 0.75, 1, UIFont.Small);
            end
        end
    end
end
