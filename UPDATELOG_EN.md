# Changelog

## [3.2.0] - Current

- Added: "Farming" tab — Crop Management (adjustable range, default 3×3: grow / mature / water / drain / cure all / harvest / destroy / clear remains, etc.) + Sowing (full seed list with name/ID search, tool-free digging, seed-free sowing, auto-watered); uses the vanilla farming command channel, works in SP and MP.
- Removed: the Character-page "Cheat farming mode" toggle (superseded by the Farming tab).
- Added: a "Create Char" tab — Custom Edit (freely add/remove traits and set skill levels 0-10 at creation; lists are persisted) + Creation Boost (all traits / max skills / unlock all clothing / trait points); unlocking all clothing shows the game's own full outfit picker at creation; everything applies on confirming creation, lists reset when entering a game. Skill levels are additive (stacked on top of profession/trait boosts, capped at 10).
- Added: "True Night Vision" — render-level full brightness, night tint and vision-cone overlay removed, unlit interiors no longer pitch black (client-side only).
- Fixed: True Night Vision interiors staying dark.
- Added: "Reveal Map" button on the Map tab — reveals the entire unexplored area, recorded server-side in multiplayer.
- Feature: map teleport reworked into "Pathfind & fast-move" — glides along walkable paths, no longer triggers the movement anticheat, WASD/Space cancels anytime; single-player keeps instant teleport.
- Fixed: the Farming tab rendering blank and the batch-buttons crash; "Grow to Harvest" taking hours to apply (growth schedule reset added).
- Fixed: "Zombies do not attack the player" no longer requires debug privileges (SP and MP); fixed the crash / kick-to-menu when a zombie got close while enabled.
- Fixed: disabling "Unlock debug privileges (SP)" no longer restarts the game (debug mode is on-only).
- Renamed: "Marksman Mode" → "Increase Fire Rate"; full-auto pushed further, semi/single fire no longer touches weapon data.
- Fixed: "Vehicle unconditional hotwire" now auto-disables the moment the engine starts.
- UI: the "Exploit" nav tab renamed "Other" and moved between "Vehicles" and "Settings"; the Players tab gained a multiplayer sync-protection hint.
- Removed: "No corpse sickness" — it only hid the noxious-smell moodle.
- Renamed: "Full limb restore" → "Fast health regen (not godmode)"; "No muscle strain" → "Disable muscle strain"; "Creation Boost" hint shortened.
- Fixed: "Auto-repair inventory items" not working in multiplayer; also auto-maxes item sharpness (full damage).
- Added: "Repair worn clothing" toggle — cleans blood/dirt/holes and restores condition on clothing, synced server-side in multiplayer.
- Fixed: "Repair worn clothing" only restored condition without cleaning blood/dirt; in multiplayer other players now also see clean clothing.
- Fixed: right-click context menu crash in some mods (e.g. Skill Recovery Journal).
- Improved: the three execution-block toggles in the "Other" tab now persist across restarts; "Block files containing suspicious words" now defaults to off.
- Added: "Unlimited ammo" now covers magazines in your backpack — all magazines auto-refill, no manual reloading.
- Feature: "Trap" tab now has a weapon mode — spawn any melee weapon or firearm (multiplayer), with batch count; the original food mode is kept, one-click mode switch.
- Added: "Pad worn clothing with leather strips" toggle — add max-level leather padding to all worn clothing, greatly improving bite/scratch/bullet protection (no materials or skill needed, SP and MP).
- Build: version bumped to 3.2.0.

## [3.2.1] - Current

- Added: item-radar "ESP tracking lines" — besides the minimap markers, searched items can be shown in the world view with tracking lines (name/count/floor/distance), drawn even off-screen, the distance labeled on the player's side of the line.
- Fixed: searching an exact full item name (e.g. magazine titles containing parentheses) failed to match the item itself and could track a different item of the same series; a full name now locks onto exactly that item, and a selected list entry takes priority.
- Rework: the "Items" page got a lighter single list (icon + name, no module sub-tabs, opens noticeably faster); "Show on map" moved to the new page.
- Added: an "Item Radar" tab — browse and search the whole item database (name/ID); "Show on map" (minimap markers) and "ESP tracking lines" (world-view lines) are two independent switches, freely combinable (minimap only / both / lines only); "Show on map" stays in sync with the minimap "Item" button in real time.
- Fixed: tracking-line endpoints and labels sat too high — they now point exactly at the item's position; cross-floor entries are labeled "upstairs/downstairs"; the count separator rendering blank is fixed.
- Adjusted: the item radar tracking range is now 56 tiles, and the auto-refresh interval after moving is more relaxed — smoother overall.
- Fixed: tracking markers disappearing after toggling the minimap "Item" button off and back on.
- Fixed: the Trap tab hint text sitting too high; searching item names containing special characters on the Trap/Loot tabs failing to match.
- UI: the Russian navigation label shortened so it no longer overflows the nav bar.
- Improved: UI runtime overhead (no behavior change).
- UI: switching to another tab and back no longer loses text input, checkbox states, list selection or minimap drag position.
- Added: a "Reset to defaults" button in Settings — restores every setting (theme colors and all feature toggles) to defaults in one click.
- UI: the "Attack speed" label shortened to "Attack speed (1-3)"; the input box and buttons now sit right next to the label (matching the crop-management row).
- UI: the "Items" nav tile is now labeled "Items(SP)" (all languages) — item spawning on that page is single-player only; use the Trap tab in multiplayer.
- Fixed: switching the language had no effect — the panel-reuse cache kept the old-language texts; language switching and "Reset to defaults" now redraw every page with the new language and settings.
- Feature: "Unlimited condition (held item)" and "Auto-repair items" now also restore a weapon's "Head condition" — the second durability bar of axe/hammer-type weapons; previously a head-broken weapon kept working but stayed at 0% head condition with halved damage, now both switches repair it automatically.
- Build: version bumped to 3.2.1.

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
