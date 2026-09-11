# Changelog

## [3.2.4] - Current

- Fixed: autodrive no longer gets wrongly boxed in by a wreck that sits off to the **side** of the road (it used to keep braking even after steering past it).
- Fixed: junctions that were wrongly treated as unreachable (neighbourhoods reached via Patton St etc.) - destinations inside road-connected neighbourhoods now get a proper all-on-road route instead of a bogus detour across open country.
- Fixed: navigation still dropped to 10 km/h for no apparent reason while passing obstacles (it now holds a steady 20 km/h throughout the pass and only brakes when right behind an obstacle).
- Fixed: routes to a few destinations still had a stretch running across open country (the line now stays on roads; when no road reaches the destination the car stops at the nearest road and tells you to drive the rest).
- Changed: navigation no longer cuts across open country to reach a destination - if no road reaches it, the car stops at the nearest road and tells you to drive the rest yourself.
- Improved: road network connectivity for navigation - stretches of road split into separate pieces by the map data are now joined correctly, reducing pointless detours.
- Fixed: navigation drew detours that swung out and doubled back at some junctions, or cut diagonally across grass (it now goes through the junction along actual roads).
- Fixed: navigation could stay frozen for a long time after getting wedged among parked cars (it now backs up a short distance and finds a way through).
- Improved: the "crush by creeping" wiggle and the navigation blocked-creep now share the same motion (same rhythm, range and speed cap).
- Fixed: navigation to destinations inside compounds / custom maps drew a route straight across open country (it now detours along actual roads - a longer road route is preferred over cutting across terrain).
- Fixed: navigation came to a complete standstill at rows of parked cars (it now squeezes slowly through passable gaps; when truly boxed in it keeps manoeuvring instead of stopping dead).
- Fixed: obstacle pass-through speed differed by travel direction on the same road (normal one way, only 10 km/h the other way).
- Improved: route planning overhaul - routes used to take huge detours or cut across open country (side roads and estate driveways were missed); they now follow actual roads closely.
- Fixed: left-right weaving while driving straight along the navigation line (now tracks the line smoothly).
- Fixed: creeping in place and never getting past staggered parked cars (now slaloms through the gaps; stops and waits only when truly boxed in).
- Fixed: gear hunting 1↔N and a flashing cruise-control light during auto-drive (now drives on the game's vanilla cruise control at a steady speed).
- Fixed: cruise speed on the vehicle dashboard showing decimals (now integer, same as vanilla).
- Changed: obstacle pass-through speed raised from 10 km/h to 20 km/h.
- Fixed: auto-drive could freeze in place after reversing out of the way at a blocked intersection (it now continues the detour).
- Improved: cornering cruise speed raised from 10 km/h to 30 km/h (sharper corners still slow down further automatically).
- Fixed: the menu could not be opened after joining a multiplayer server (the loader cache now reloads automatically when the game resets its environment).
- Improved: navigation now uses the fuller map road data - in areas without named streets (tracks, rural roads, custom maps) it follows visible roads instead of cutting straight across.
- Fixed: navigation braked too late for corners and overshot (it now slows down ahead of the bend).
- Fixed: navigation could not get through intersections / staggered parked cars, or scraped other vehicles (it now picks a passable gap; when fully blocked it holds in place to stay mobile instead of stopping dead).
- Added: "Diagnostics log" toggle in the "Navigation Mode" module (troubleshooting: record a drive and share the log to help pinpoint issues).
- Added: keybind panel - customize menu hotkeys; settings survive reinstalls.
- Changed: loot page UI simplified, count input removed (fishing-rod spawn creates one item at a time).
- Fixed: with "Super Multi-Hit" enabled, only the closest few zombies took meaningful damage per swing (all targets in range now take full damage; combined with attack range bonus, a single swing clears a whole horde).
- Fixed: repeated braking and re-accelerating when auto-driving through turns on the navigation line (corners are now passed at a steady low speed).
- Fixed: being pushed back or stopped dead by roadside objects (signs, mailboxes, fences) while no-clipping.
- Fixed: still slowing down when driving through trees, lamp posts, trash cans (now all static obstacles at full speed).
- Fixed: overly sharp steering when merging back onto the road after detouring (now merges smoothly).
- Improved: detours now maintain a steady crawl like the game cruise control, no more surging and braking.
- Improved: detours now pass obstacles at a steady crawl (10 km/h) instead of surging and braking.
- Fixed: detour geometry is now aligned with the road direction, fixing erratic back-and-forth weaving after passing.
- Fixed: vehicle circling endlessly around an obstacle after detouring instead of merging back onto the route.
- Fixed: swinging back and forth when merging back onto the road after detouring (now merges smoothly).
- Fixed: circling in place behind an obstacle after detouring; stuttering during detours (now smooth).
- Fixed: stuttering brake-accelerate cycles while detouring around obstacles (now a smooth low-speed detour).
- Fixed: vehicles feeling sticky and extremely slow when no-clipping through trees/bushes/shrubs (now same speed as buildings).
- Improved: auto-drive now brakes smoothly by distance to a blocking vehicle ahead (no more crashing into blocking vehicles under any circumstance).
- Fixed: detour passing too close to the obstacle; overshooting the destination after a detour; blocked-wiggle rushing toward the obstacle from far away.
- Fixed: auto-drive avoidance crashing head-on into blocking vehicles on straight roads (detour side flip-flopping, plus detour line grazing the obstacle).
- Improved: "Wiggle kill" now includes instant kill on contact, and wiggle stays locked in a small back-and-forth range instead of drifting backwards.
- Changed: removed the hint text from the "Utility" module.
- Fixed: temp weapon jamming with no way to clear it (temp weapons no longer jam).
- Changed: new Vehicle page module renamed to "Utility".
- Added: "Combat" module on the Vehicle page (wiggle kill / vehicle instakill / vehicle no-clip). All three are always ON during navigation; the toggles only apply to manual driving. Settings are saved automatically.
- Added: route display on the map — after right-click anchoring, the navigation route is drawn as a solid blue line on both the world map and minimap (along street centerlines; orange straight line when no road network), destination marker kept.
- Improved: navigation now follows main roads strictly — route planning uses the exact same street data as the map itself, so the route sticks to street centerlines and never cuts through blocks; one right-click anchor plans the whole trip, cross-map trips complete in one go, with a straight-line fallback when no road network is available.
- Fixed: navigation not following streets and beelining straight on certain map mod combinations.
- Improved: long cross-town route planning — gaps not covered by street data are bridged with short straight segments, cross-map routes plan in one go.
- Improved: route display — after manually taking over or reaching the destination, the navigation line and destination marker stay on the map until a new destination is anchored.
- Fixed: occasional black screen and vehicle sinking into the ground when taking back manual control while driving through walls.
- Fixed: navigation repeatedly rocking back and forth at corners and U-turns; also repeatedly reversing again just as it was about to rejoin the route.
- Improved: navigation steering rewritten — steadier on straights, tighter corner tracking, oscillation and wobble issues resolved at the root.
- Improved: navigation speed control rewritten — smooth deceleration before corners, steady stopping behind obstacles, no more sudden braking or crawling on straights, and stable gears.
- Changed: navigation is now pure line-following — it drives straight along the route to the destination, plowing straight through/pushing past everything on the way (zombies die on contact), no longer slowing, stopping or reversing around obstacles; only unloaded map areas still make it stop and wait. U-turns are now done as a single forward arc.
- Fixed: navigation mistaking roadside parked cars and street lamps for obstacles dead ahead, causing stop-go driving and even repeated reversing on straights.
- Fixed: cruise speed setting being capped by the default speed limit — the set cruise speed now applies directly (still capped by the vehicle's physical top speed).
- Fixed: navigation taking a detour when anchoring straight ahead on the same road — the route now extends directly forward from the road under the vehicle.
- Added: "Clear navigation line" — a map right-click menu option to manually remove the navigation line and destination marker (for when you take over and head elsewhere).
- Added: "Resume navigation" — after taking over manually, one click in the map right-click menu resumes auto-drive along the original route to the original destination.
- Added: "Autopilot" (Vehicle tab) — sit in the driver seat, press M and right-click "Auto-drive here" on the map: speed, corners, obstacle avoidance, waiting for unloaded map areas and arrival braking are all automatic; re-anchor at any time to reroute, any driving key takes over instantly; the Vehicle tab offers cruise speed (0 = auto) and obstacle policy (slow detour / stop and wait / low-speed push).
- Added: "Super multi-hit" (Combat tab) — one swing hits up to 10-20 targets (adjustable) in all directions (behind you too); "Apply" = active, "Reset" = back to vanilla.
- Added: "Temp weapon" (Combat tab) — temporarily swap your held weapon for any firearm from the list: full ammo and condition, ready to fire; "Swap back" instantly restores the original item; zero server traces (gunshots are audible server-wide).
- Changed: new "Combat" tab — combat boost, super multi-hit and temp weapon live here; the former "Character" tab is renamed to "Survival".
- Fixed: temp weapon list appearing empty; its search bar now matches the Exchange page design (Name/ID fields with clear buttons).
- Changed: temp weapon no longer shows the red warning note (visible only on screen, not verifiable in server logs).
- Changed: the VHS teaching search (Player tab) now matches the Exchange page design (Name/ID fields with clear buttons).
- Fixed: the Player tab erroring and failing to open.
- Changed: the Combat tab icon has been redrawn to match the other tab icons.
- Fixed: autopilot — the full map (M) right-click now offers "Auto-drive here" too; fixed the error when starting after anchoring a target; fixed the error when switching the obstacle policy.
- Fixed: the "Temp weapon — swap" button erroring out on click (generating and swapping the held weapon now works).
- Fixed: the temp weapon search bar layout where labels and input boxes overlapped into a jumble.
- Improved: autopilot obstacle handling — static obstacles (fences, lampposts, guardrails, walls) now trigger early slowdown and detours instead of head-on collisions; rerouting after getting stuck is more responsive.
- Improved: autopilot keeps a steady gear at low speed and while waiting (no more N/1 flip-flopping).
- Fixed: errors on every gunshot and on "Swap back" in temp weapon (shooting itself worked).
- Fixed: autopilot not moving after anchoring a target (steering worked, car stayed put).
- Changed: the multi-hit family is renamed for consistency — "Group-Hit on zombies" is now "Multi-Hit", and the advanced version is "Multi-Hit Pro" (Russian: "Групповой удар" / "Групповой удар Pro").
- Changed: the Multi-Hit Pro note has been reworded and now sits right below the feature (all languages).
- Fixed: autopilot grinding through zombie crowds at low speed — it now stops in front and detours through visible gaps, prioritizing the destination.
- Improved: autopilot cornering and edge tracking are much steadier, greatly reducing scrapes with lampposts and fences.
- Changed: the obstacle policy no longer includes "Stop and wait" — only "Slow detour / Low-speed push" remain.
- Fixed: the autopilot destination marker staying on the map after arrival (or after leaving the driver seat).
- Fixed: autopilot getting stuck dead against bushes, lampposts or fences — it now reverses out, re-plans the route, and gives up with a clear notice only when a passage truly is impossible, instead of grinding in place forever.
- Improved: autopilot now also senses bushes and hedges it previously could not "see", slowing down and detouring early.
- Changed: "Autopilot" is renamed to "Pseudo-Autopilot" (all languages) — it was never a real autopilot anyway.
- Added: while "Pseudo-Autopilot" is driving, the vehicle can pass through small obstacles such as fences, lampposts and bushes (no more slowing to a grind, no more getting stuck — it just slips through; zombies and other vehicles are still avoided); normal collisions are restored on arrival or exit.
- Changed: the obstacle policy option is gone — zombies are now always pushed through at low speed (no more stopping to detour; routes stay smooth and steady), while other vehicles and unloaded map areas are still avoided as before.
- Changed: "Pseudo-Autopilot" simplified — after anchoring, it follows the road network straight to the destination (grass and fields don't count as roads) with no frequent re-planning; zombies along the way die on contact while the car takes zero damage and is never stalled by crowds; narrow alleys no longer fail with "no route found".
- Changed: "Pseudo-Autopilot" is renamed to "Navigation Mode" (all languages) — the map context menu now reads "Navigate here".
- Fixed: occasional spinning in place before departing; cornering no longer overshoots off the road into woods as easily; the pass-through obstacle speed limit is raised from 12 to 30 km/h.
- Changed: the Navigation Mode panel is streamlined — the stop button is removed (any driving key takes over instantly, which stops it) along with the duplicated status text while driving; also fixes vanilla error spam when browsing seat containers of a vehicle you have driven far away from.
- Build: version bumped to 3.2.4.

## [3.2.3]

- Added: "Guns always hit (ignores environment)" (Character tab - Combat) — firearms no longer miss due to rain, fog, darkness, panic, movement or other conditions; stacks freely with "Headshot only" and "Crit Max".
- Changed: "Instant kill" no longer extends weapon range; engagement range is back to the weapon's original values. One-hit-kill capability unchanged.
- Reworked: the "Players" tab — player info and recipes merged into one module, a new "VHS lessons" module added, traits and skills shown as separate sections.
- Added: "VHS lessons" (Players tab) — search and boost any skill in one click; requires a playing radio/TV nearby (vehicle radios work), and an inventory radio is placed out automatically when none is around; the server's media XP cap setting may limit the effect.
- Fixed: hint text overlap on the "Loot" tab.
- Build: version bumped to 3.2.3.

## [3.2.2]

- Added: "Fun" tab — ① "Send as another player": post chat messages under any online player's name (server-wide or say/30-tile channel; the target player can't see the message themselves, multiplayer only); ② "Zombie skin": wear a rotting zombie skin in one click (light/heavy rot plus a rotten-face option, a new look each click, skin and face combine freely).
- Added: "Swap" tab — consume clothing from your inventory to obtain any clothing/bags/armor in the list (mod items included).
- Added: "Attack range bonus" (Character tab - Combat, adjustable 0-4 tiles).
- Changed: "Attack speed" cap tightened from 3.0 to 2.5.
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
- Fixed: fishing-rod item spawn — restored the Loot page spawn UI (previously overwritten during a sync).
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
