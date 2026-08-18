# Changelog

## [3.1.8] - Current

- Feature: Headshot only for firearms — every hit is a headshot (3x damage).
- UI: closing the menu now hides it — reopening restores the previous tab and scroll position.
- Fixed: language switch takes effect immediately (texts no longer require a page switch to refresh).
- UI: "Headshot only" and "Auto-repair inventory items" checkboxes swapped positions.
- Fixed: NoClip / Invisible / God mode / Zombies don't attack now work in single-player again (previously blocked by the vanilla permission system); ineffective in multiplayer.
- Feature: Vehicle unconditional hotwire — instantly hotwires and starts the vehicle locally; auto-disables after 30s. Toggle moved to the Character tab, above "Auto-repair inventory items".
- Build: version bumped to 3.1.8.

## [3.1.7]

- Feature: Infinite ammo + ammo spawning — ammo refills automatically when empty (magazine weapons generate per magazine).
- Feature: CritMax — main-hand weapons always crit and always knock down.
- Feature: Custom attack speed multiplier (1.0-3.0) with input box and Apply/Reset buttons.
- Removed: crit damage multiplier — ineffective in multiplayer (server-authoritative damage).
- UI: button text vertically centered and auto-width (no overflow after language switch); loot page spacing increased.
- Fixed: installer now cleans leftover game files before installing (install no longer rejected by stale leftovers).
- Fixed: infinite durability for held items in multiplayer.
- Build: slimmer dependencies (3 small jars); JDK25 and Gradle 9.1.0 required.
- Fixed: server sync protection overhaul — module now actually active, no more stat/skill rollback, anti-cheat toggle works, logs no longer spam.

## [3.1.6]

- Feature: "Loot reroll" tab — resets loot records for all containers in a radius (default 10), reopened containers get re-rolled by the server, gun cabinets/ammo boxes can yield weapons and ammo; F9 hotkey; multiplayer only.
- Feature: "Fishing rod spawn" section (self-hosted servers only) — inject any item via fishing rod, ~5-8 s per item.
- Fixed: "server sync protection" toggle not working.
- UI: full panel restyle — RE2 remake style + frosted glass (main window, sidebar, buttons, tables, floating windows, popups); fixed glass dimming and text overlap.

## [3.1.5]

- Feature: "Trap" tab — search and spawn food (must stand next to a placed trap).
- Feature: F10 hotkey for container loot reset.
- Changed: multi-hit, 360° vision and three anti-detection toggles now enabled by default.
- Removed: item swapping — no viable server-side channel.

## [3.1.4]

- Fixed: 3D avatar in the player editor.
- Changed: minimap "items" layer now off by default.

## [3.1.3]

- Fixed: item radar missing items on corpses.
- Fixed: minimap quick-toggle bar and main panel checkbox now two-way synced.
- Fixed: minimap could not open.

## [3.1.2]

- Feature: item search + minimap markers — scans a 48-tile radius (floor ±1) across furniture/containers, ground items, bags and vehicle containers.
- Feature: minimap quick-toggle bar (me / players / vehicles / zombies / items).
- Feature: markers refresh automatically as the character moves.
- Fixed: replaced APIs unavailable in the B42 Lua VM; added a static check that blocks disallowed syntax before builds.
- Changed: minimap default size 256→300.
- Docs: README added; install flow unified to build.bat → install.bat.

## [3.1.0]

- Initial release: Project Zomboid B42 community port based on dei0/EtherHack.