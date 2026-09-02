# Changelog

## [3.2.2] - Current

- Added: "Fun" tab — ① "Send as another player": post chat messages under any online player's name (server-wide or say/30-tile channel; the target player can't see the message themselves, multiplayer only); ② "Zombie skin": wear a rotting zombie skin in one click (light/heavy rot plus a rotten-face option, a new look each click, skin and face combine freely).
- Added: "Swap" tab — consume clothing from your inventory to obtain any clothing/bags/armor in the list (mod items included).
- Added: "Attack range bonus" (Character tab - Combat, adjustable 0-4 tiles).
- Changed: "Attack speed" cap tightened from 3.0 to 2.5 — at higher multipliers targets get knocked back and swings whiff, so effective hit rate drops.
- Improved: "Item Radar" searches now run in small background steps — no more periodic stutters while moving or picking up items; the frame stays smooth, and old markers keep showing until the refresh completes.
- Improved: minimap overall cost — the game runs smoother with the minimap always open, and pre-existing occasional hitches no longer get stretched out.
- Fixed: "Learn all available crafting recipes" not working in multiplayer — recipes now sync to the server, pass crafting validation, and persist through relogging.
- Changed: "Learn all available crafting recipes" moved to the Character tab.
- Fixed: buttons on the "Other" tab were clickable while unavailable — unavailable buttons are now greyed out and unclickable (e.g. vehicle mechanics when no vehicle nearby).
- Build: version bumped to 3.2.2.

## [3.2.1]

- Feature: "Unlimited condition (held item)" and "Auto-repair items" now also restore a weapon's "Head condition" — the second durability bar of axe/hammer-type weapons.
- Added: an "Item Radar" tab — search the whole item database, with freely combinable minimap-marker and tracking-line switches; the "Items" page list reworked, lighter and faster.
- Added: tracking lines are drawn even off-screen, labeled with name/count/floor/distance; tracking range extended to 56 tiles.
- Fixed: exact full-name item search and tracking-target selection; tracking lines pointing too high; tracking lost after toggling the minimap Item button; language switching not applying; Trap tab hint position and special-character name search.
- UI: tab switching keeps inputs and checkboxes; "Reset to defaults" button in Settings; tighter "Attack speed" row; nav "Items" labeled "Items(SP)"; shorter Russian nav labels; overall performance improved.
- Build: version bumped to 3.2.1.

## [3.2.0]

- Added: "Farming" tab — crop management (grow / ripen / water / cure / harvest / remove, adjustable range) + tool-free digging, seed-free sowing with auto-watering (supersedes the "Cheat farming mode" toggle).
- Added: "Create Char" tab — add/remove traits freely, set skill levels, unlock all clothing at creation.
- Added: "True Night Vision" (render-level full brightness); "Reveal Map" button on the Map tab; "Repair worn clothing" and "Pad worn clothing with leather strips" toggles.
- Features: map teleport reworked into "Pathfind & fast-move"; "Unlimited ammo" covers all backpack magazines; "Auto-repair items" works in multiplayer and maxes sharpness; "Zombies do not attack the player" works in multiplayer (no chasing, no biting).
- Features: "Trap" tab weapon mode — spawn any melee weapon or firearm (multiplayer), with batch count.
- Removed: "No corpse sickness" — testing showed it only hides the indicator.
- Renamed: "Marksman Mode" → "Increase Fire Rate"; "Full limb restore" → "Fast health regen (not godmode)"; "No muscle strain" → "Disable muscle strain".
- Fixed: night vision indoors; farming page blank and button errors; context-menu crash in some mods; "Repair worn clothing" not cleaning blood and invisible to others; vehicle hotwire now auto-disables; debug-mode toggle no longer restarts the game.
- UI: nav "Exploit" renamed "Other"; multiplayer sync-protection hint on the Players page; execution-block toggles remember settings.
- Build: version bumped to 3.2.0.


## [3.1.9]

- Features: 9 items from the 2026-08-19 plan — new Vehicles tab (unconditional engine start / repair / refuel; the engine still needs fuel and battery), "No jamming" and "Creation Boost" (all traits / max skills / starting outfit); Unlimited carry and "Zombies do not attack the player" now work in multiplayer (no chasing, no biting).
- UI: **full cyberpunk-style redesign** — icon+label nav tiles, adaptive multi-column layout, unified chamfered neon skin, aligned CN/EN/RU texts.
- UI: the "Visuals" tab is now "ESP", reorganized into four flat modules; the Character tab reorganized into five modules.
- Feature: zombie info is now an overhead health bar; new zombie radar; vehicles show name + power/top speed.
- Feature: Reset Loot (F9) refreshes instantly — no manual container reopening.
- Fixed: several rounds of ESP feedback — zoom misalignment, HP bar style, text sizes, vehicle power units.
- Fixed: playtest issues — Character tab failing to open, Marksman Mode model distortion, "Repair vehicle" error; Vehicles nav icon redrawn as neon line-art.
- UI: "Vehicle unconditional hotwire" moved into Vehicles, split into "Start engine unconditionally (once / auto-retry)", auto-unchecks on success.
- Renamed: "Bypass debug mode prohibition (type 12)" → "Unlock debug privileges (SP)", five affected toggles gain a "(SP)" suffix.
- Renamed: "Disable recoil" → "Marksman Mode" (multiplayer fire-rate safety bound); "Timed Action Instant Cheat" → "Instant progress bars"; "Multi-Hit on zombies" → "Group-Hit on zombies" (CN/EN/RU).
- Fixed: debug-privilege unlock aligned with B42 (multiplayer never touches Core.debug); Info tab anti-cheat status now reports Permission/Movement separately.
- Fixed: the Players tab info text leaking outside the panel while scrolling.
- Docs: Farming tab plan finalized, not yet implemented.
- Build: version bumped to 3.1.9.

## [3.1.8]

- Feature: Headshot only for firearms — every hit is a headshot (3x damage).
- UI: closing the menu now hides it — reopening restores the previous tab and scroll position.
- Fixed: language switch takes effect immediately (texts no longer require a page switch to refresh).
- UI: "Headshot only" and "Auto-repair inventory items" checkboxes swapped positions.
- Fixed: NoClip / Invisible / God mode / Zombies don't attack now work in single-player again (requires enabling "Bypass debug mode ban (type 12)"). Note: ineffective in multiplayer.
- Feature: Vehicle unconditional hotwire — instantly hotwires and starts the vehicle locally; auto-disables after 30s. Toggle moved to the Character tab, above "Auto-repair inventory items".
- Fixed: fishing-rod item spawn — restored the Loot page spawn UI (previously overwritten during a sync) and the fishSpawn* globals (removed exposeFishingSpawn caused an error on spawn); list position/height adjusted.
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
- Fixed: server sync protection — because the server force-rolls back skills/stats, level-ups only take effect locally while the feature is enabled and are lost after a reconnect.

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
