# Changelog

## [3.1.5] - Current

- Feature: new "Trap" tab (custom `trap.png` icon, next to the Item Creator page) — search box + list of all spawnable foods (auto-filtered by `hungerChange < 0`); select and click "Spawn" to run the trap chain (stand next to a placed trap).
- Feature: **F10** hotkey triggers "Reset loot" (same entry point as the button, multiplayer only).
- Changed: removed the "Trap-spawn item" button from the Item Creator page; "Reset loot" moved into its slot (label now shows F10).
- Changed: "Multi-Hit on zombies" and "360-degree vision" now default to **on** (field initializers and config defaults).
- Changed: the three anti-detection toggles now default to **on** — block default loggers, block files mentioning cheats, block files with suspicious words.
- Removed: the item swap feature — verified `AddItemInInventoryPacket` only has `processClient` (server→client direction), so client sends are silently ignored by the server, while `RemoveInventoryItemFromContainerPacket.processServer` genuinely deletes items and writes a server-side "item" log (username + coordinates + item); no viable swap channel exists, so the whole feature (incl. translations and icon) was removed.

### Planned / Not yet implemented

- Deep dive on container-item / corpse-container loot re-roll (static analysis done, not implemented):
  - Confirmed open-box mechanic: `RequestItemsForContainerPacket` → server runs `ItemPickerJava.fillContainer` on unexplored containers, rolls per container-type table, then pushes items; container items such as gun cases / military bags roll weapon tables.
  - To test 1: leave container items (sewing kits, gun cases, etc.) **inside** room containers un-emptied, repeatedly reset the room container, and check whether `fillContainerInternal` keeps topping up nested containers via `fillRand`.
  - To test 2: reset `explored` on indoor corpse containers (`inventorymale/female`) and search corpses repeatedly to re-roll zombie loot tables (weapons/ammo/clothing); outdoor corpses fail server-side (`getRoom()` nil deref in the command) and are out of scope.
  - Extension point: add a corpse-object branch to the `EtherContainerPOC` reset loop and document the "don't empty" usage.

## [3.1.4]

- Fixed: Player Editor 3D avatar had no character set, causing a per-frame `UI3DModel` NPE in render (now uses the vanilla `setCharacter` approach).
- Changed: the minimap "Items" layer toggle now defaults to **off** (was on).

## [3.1.3]

- Fixed: item radar missed items on corpses — dead bodies (`IsoDeadBody`) are not in `getObjects()`; they live in the separate `sq:getDeadBodys()` list and must be read via their `getContainer()`.
- Fixed: minimap quick-toggle bar and main-panel checkboxes are now **two-way synced** (single source of truth: the Java config flags); the main panel gained a "Show items" checkbox to match the minimap.
- Fixed: the Kahlua parser does not support indexing a table constructor literal (`{...}[k]`), which broke `UIMovableMiniMap.lua` loading and prevented the minimap from opening; the compat checker blacklist now includes the `}[` pattern.

## [3.1.2]

- Feature: **Item search + minimap markers** — pick an item in the "Item Creator" page and click "Show on map"; scans loaded tiles within radius 48 (player floor ±1):
  - Furniture/container contents (`IsoObject` containers)
  - Floor items and bags on the ground (including bag contents)
  - Vehicle part containers (trunk, seats, etc.)
- Feature: minimap quick-toggle bar (top of the window: `Me / Players / Vehicles / Zombies / Items`, white = on / gray = off); turning "Items" off clears markers and drops all per-tick cost, turning it back on silently re-scans the last target.
- Feature: markers follow the player (re-scan after walking 5+ tiles with a 2s cooldown).
- Improved: marker refresh is now event-driven with debounce (inventory count change or inventory window change → one re-scan after 1s of quiet); timer removed, zero idle cost.
- Fixed: replaced APIs unavailable in the B42 Lua VM (Kahlua) — `next()`, `ISUIElement.getVisible()`, `VehicleParts.getParts()`, `ItemContainer.size()`.
- Hardened: added `tests/check_kahlua_compat.lua` static checker to block banned patterns before building.
- Changed: minimap default size 256→300; credits text updated.
- Docs: added README / README_zh; unified installation to build.bat → install.bat.

## [3.1.0]

- Initial: community port of [dei0/EtherHack](https://github.com/dei0/EtherHack) for Project Zomboid Build 42.
