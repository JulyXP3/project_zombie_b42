# Changelog

## [3.1.4] - Current

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
