# Changelog

## [3.1.7] - Current

- Feature: unlimited ammo synced to server + ammo farming — refills ammo and triggers unload/eject (magazine weapons spawn magazines) when depleted, synced to server-side fields.
- Feature: CritMax — primary weapon always crits (`CriticalChance=100`) and always knocks down (`AlwaysKnockdown=true`), applied every frame on `OnRenderTick`, restored from cache when toggled off.
- Feature: attack speed multiplier — `IsoGameCharacter.calculateCombatSpeed()` return value is `FMUL`-multiplied by the multiplier (1.0-3.0, >2.0 carries a server kick risk).
- Removed: critical damage multiplier customization — damage is server-authoritative in multiplayer so the client-side multiplier was ineffective; field/Lua methods/config persistence/per-frame cache fully removed; CritMax now only keeps always-crit + always-knockdown.
- UI: unified layout constants (`EtherTheme.rowH/btnH/labelPadY`) giving checkboxes/sliders/buttons consistent row heights; button text vertically centered by font height and buttons auto-widen to their text (no more overflow on language switch).
- UI: loot page got larger section spacing; ammo farm row and reset button are left-aligned and stack dynamically when width is tight.
- Fixed: `install.bat` now deletes leftover `zombie/EtherHack` before installing, preventing install rejection caused by leftovers from the packaged-game uninstaller.
- Build: slimmed lib — removed `zombie.jar` and LFS markers, only 3 small dependency jars are tracked; README documents JDK17+ and Gradle 9.1.0 and adds the `projectzomboid.jar → lib/zombie.jar` copy step.

## [3.1.6]

- Fixed: "Server Sync Protection" toggle was a no-op — `exposeServerSyncBlocker` misused Kahlua's `exposeMethod` (attaches to the class metatable only, never became a Lua global); switched to `exposeGlobalClassFunction`, and `ServerSyncBlocker.lua` now resolves globals at call time instead of caching them at load.
- UI: entire panel restyled in **RE2 Remake + simulated glassmorphism** (Resident Evil style) — new `EtherTheme.lua` theme module (palette / glass backdrop / blood-red title bars / unified list & table styling), new `noise.png` grain texture and blood-red `close_re.png` icon:
  - Main window: near-black glass background + blood-red border + blood-red title bar (`E T H E R  H A C K  //  B42`) with a new red X close button; content area shifted down below the title bar.
  - Left tab rail: dark glass, active tab = blood-red 5px bar + accent-tinted icon, red hover glow.
  - Buttons / checkboxes / sliders: glass buttons + blood-red outline + accent left bar; checkbox labels light-ivory and vertically centered on the icon; slider track outlined in blood red.
  - Tables (skills / items / traits / trap / vehicle / medic): alternating glass rows, accent selection, blood-red column dividers, dark-red headers.
  - Floating windows (minimap / mechanics / medic) and modals: glass background + blood-red title bar + new close icon.
- Fixed: skill table column widths and alignment — level/XP/boost columns are fixed-width and centered; XP text no longer overflows into the boost column.
- Fixed: info page text overlap — line spacing is now computed from actual font heights and status lines (enabled/disabled) are split into two rows with color coding.
- Visual-only change; no functionality or logic touched.

## [3.1.5]

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
