--*********************************************************
--* ItemScanIndex: 标量贡献索引与矩形差集游标, 不持有世界对象。
--* 新盒减旧盒分为至多六个不重叠轴向片, 避免重扫重叠区域。
--*********************************************************
ItemScanIndex = {};

function ItemScanIndex.bounds(x, y, z, radius)
    local cx, cy, cz = math.floor(x), math.floor(y), math.floor(z);
    return { x1 = cx - radius, x2 = cx + radius, y1 = cy - radius, y2 = cy + radius,
        z1 = cz - 1, z2 = cz + 1, cx = cx, cy = cy, cz = cz, radius = radius };
end

function ItemScanIndex.key(x, y, z)
    return x .. "," .. y .. "," .. z;
end

function ItemScanIndex.contains(b, x, y, z)
    return b ~= nil and x >= b.x1 and x <= b.x2 and y >= b.y1 and y <= b.y2
        and z >= b.z1 and z <= b.z2;
end

local function range(list, x1, x2, y1, y2, z1, z2)
    if x1 <= x2 and y1 <= y2 and z1 <= z2 then
        list[#list + 1] = { x1 = x1, x2 = x2, y1 = y1, y2 = y2, z1 = z1, z2 = z2 };
    end
end

function ItemScanIndex.plan(old, b, dirty, full)
    local cur = { ranges = {}, band = 1, dirty = dirty or {}, dirtyAt = 1, seen = {}, bounds = b };
    local ranges = cur.ranges;
    if old == nil or full or old.radius ~= b.radius or math.abs(b.cx - old.cx) > b.radius
        or math.abs(b.cy - old.cy) > b.radius or b.cz ~= old.cz then
        range(ranges, b.x1, b.x2, b.y1, b.y2, b.z1, b.z2);
        return cur;
    end
    local x1, x2 = math.max(b.x1, old.x1), math.min(b.x2, old.x2);
    local y1, y2 = math.max(b.y1, old.y1), math.min(b.y2, old.y2);
    local z1, z2 = math.max(b.z1, old.z1), math.min(b.z2, old.z2);
    if x1 > x2 or y1 > y2 or z1 > z2 then
        range(ranges, b.x1, b.x2, b.y1, b.y2, b.z1, b.z2);
    else
        range(ranges, b.x1, x1 - 1, b.y1, b.y2, b.z1, b.z2);
        range(ranges, x2 + 1, b.x2, b.y1, b.y2, b.z1, b.z2);
        range(ranges, x1, x2, b.y1, y1 - 1, b.z1, b.z2);
        range(ranges, x1, x2, y2 + 1, b.y2, b.z1, b.z2);
        range(ranges, x1, x2, y1, y2, b.z1, z1 - 1);
        range(ranges, x1, x2, y1, y2, z2 + 1, b.z2);
    end
    return cur;
end

function ItemScanIndex.take(cur)
    while cur.dirtyAt <= #cur.dirty do
        local p = cur.dirty[cur.dirtyAt];
        cur.dirtyAt = cur.dirtyAt + 1;
        local key = ItemScanIndex.key(p.x, p.y, p.z);
        if not cur.seen[key] and ItemScanIndex.contains(cur.bounds, p.x, p.y, p.z) then
            cur.seen[key] = true;
            return p.x, p.y, p.z, key;
        end
    end
    while cur.band <= #cur.ranges do
        local b = cur.ranges[cur.band];
        local x, y, z = cur.x or b.x1, cur.y or b.y1, cur.z or b.z1;
        cur.x, cur.y, cur.z = x, y + 1, z;
        if cur.y > b.y2 then cur.y = b.y1; cur.x = x + 1; end
        if cur.x > b.x2 then cur.x = b.x1; cur.z = z + 1; end
        if cur.z > b.z2 then
            cur.band = cur.band + 1;
            cur.x, cur.y, cur.z = nil, nil, nil;
        end
        local key = ItemScanIndex.key(x, y, z);
        if not cur.seen[key] then return x, y, z, key; end
    end
end

function ItemScanIndex.patrol(b, offset, count)
    local width, height = b.x2 - b.x1 + 1, b.y2 - b.y1 + 1;
    local area = width * height;
    local total = area * (b.z2 - b.z1 + 1);
    local out = {};
    for i = 0, math.min(count, total) - 1 do
        local n = (offset + i) % total;
        out[#out + 1] = { x = b.x1 + math.floor((n % area) / height),
            y = b.y1 + n % height, z = b.z1 + math.floor(n / area) };
    end
    return out, (offset + #out) % total;
end

function ItemScanIndex.add(out, keys, x, y, z, name, count)
    if x ~= x or y ~= y or z ~= z or math.abs(x) == math.huge
        or math.abs(y) == math.huge or math.abs(z) == math.huge then return; end
    x, y, z = math.floor(x), math.floor(y), math.floor(z);
    local key = ItemScanIndex.key(x, y, z);
    local n = keys[key];
    if n ~= nil then
        local hit = out[n];
        hit.count = hit.count + (count or 1);
        hit.label = hit.name .. " x" .. hit.count;
    else
        n = #out + 1;
        keys[key] = n;
        out[n] = { x = x, y = y, z = z, name = name, count = count or 1,
            label = name .. " x" .. (count or 1) };
    end
end
