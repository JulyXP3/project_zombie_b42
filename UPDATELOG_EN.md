# Changelog

## [3.1.10] - Current

- Fixed: "Zombies do not attack the player" no longer requires enabling "Unlock debug privileges" first in single-player — it now intercepts the zombie's attack decision directly (when the toggle is on and the target is the local player, the zombie won't attack), working in both single-player and multiplayer with no debug mode needed.
- Fixed: enabling "Zombies do not attack the player" no longer crashes (kicks you back to the menu) when a zombie gets close — the zombie's perception routine now ignores the local player entirely instead of clearing its target mid-routine (which left a null target and threw a NullPointerException); zombies won't lock onto, chase, or attack you.
- Fixed: enabling then disabling "Unlock debug privileges (SP)" no longer restarts the game — debug mode is only turned on in-game and never off (B42 doesn't support disabling it at runtime); unchecking now takes effect on the next game launch instead.
- Renamed: "Marksman Mode" → "Increase Fire Rate" (CN/EN/RU; the name now matches what it actually does — it boosts fire rate, not accuracy). Full-auto fire rate is pushed further (more aggressive while staying inside the multiplayer anti-cheat safety margin), and semi-auto/single fire no longer touches any weapon data at all (no more model distortion or animation lockup risk).
- Fixed: "Vehicle unconditional hotwire" now auto-disables the moment the engine actually starts (instead of always waiting 30s); the 30s timeout is kept only as a fallback for when the engine can't start (no fuel/battery). The "(auto-off after 30s)" label suffix is removed.
- UI: the "Exploit" nav tab is renamed "Other" and moved from the end to between "Vehicles" and "Settings" (CN/EN/RU); the in-page hints pointing at it were updated to match.
- UI: the Players tab now shows a hint to the right of the "Remove" and "Max all skills" buttons — in multiplayer you must enable "Other → Server sync protection" first.
- Removed: "No corpse sickness" — on review the toggle did not do what its name promised: it only hid the "noxious smell" moodle icon and did **not** stop the nausea/food sickness caused by corpse piles (the game recomputes the sickness every tick from the surrounding corpse count, independently of the value being zeroed). It has been removed entirely. If it ever seemed to work, the health regen toggle was most likely healing the damage back.
- Renamed: "Full limb restore" → "Fast health regen (not godmode)" — all it really does is keep every body part's health topped up (so you basically won't die from losing health), while bleeding, fractures (a splint is still needed) and zombie infection from bites all remain. The name no longer implies invincibility.
- Renamed: "No muscle strain" → "Disable muscle strain" (CN/EN/RU; behaviour unchanged — soreness pain from exercise/exertion is cleared instantly, so you can train continuously).
- UI: the "Creation Boost" hint is shortened to "Applies when creating a new character (MP join / reroll)".
- Build: version bumped to 3.1.10.

## [3.1.9]

- Fixed: five issues from playtesting — the Character tab failed to open (two table-closer typos in the creation module broke the whole file); **Marksman Mode model distortion** (the old implementation zeroed the weapon's recoil-delay field, which maxed out the recoil-pose variable and ran the shot animation at 1.8x, freezing the character after one shot until re-aiming; now the player-side fire gate is zeroed per frame instead, keeping pose/animation vanilla with continuous fire); "Repair vehicle" error (the repair-count argument must be a number, a boolean crashed the argument check); the Vehicles nav icon redrawn as a neon line-art car (previously borrowed the vanilla fuel-pump texture, clashing with the icon set).
- UI: "Vehicle unconditional hotwire" moved from the Character tab into Vehicles → "Engine & Start"; split into "Start engine unconditionally (once)" (manual single shot) and "Start engine unconditionally (auto-retry)" (re-sends the start command every second, **auto-unchecks on success**, unchecking stops immediately); the module gained the hint "Starting still requires fuel and battery, otherwise it fails"; the service hint was simplified to "You must be inside a vehicle".
- Renamed: "Multi-Hit on zombies" → "Group-Hit on zombies" (CN/EN/RU).
- Features: 9 items from the 2026-08-19 plan implemented — new Vehicles tab (unconditional engine start / start now / repair / refuel via server vehicle commands that have no permission checks; the engine still needs fuel and battery); "No jamming" added to Combat; "No muscle strain / Full limb restore / No corpse sickness" added to Status; Unlimited carry now works in multiplayer (same PlayerDamage self-report channel as the above, 20/s resend outpacing the server's per-tick recalculation); "Zombies do not attack the player" is no longer SP-only (blocks the local zombie-simulation target, which the server adopts unvalidated — no chasing, no biting) and moved to Special Modes; new "Creation Boost" module (all positive traits / max skills / starting outfit, applied on the next character creation). Fullbright: the planned injection point turned out to be dead code (zero callers), the plan is withdrawn pending a new approach (intercepting lightInfo would make zombies see you more easily; world rendering lives in the shader layer) and is not implemented this round.
- UI: **full cyberpunk-style redesign** — icon+label nav tiles, adaptive multi-column layout on feature pages (falls back to a single column on narrow panels), smaller and easier-to-read hint texts, unified chamfered neon skin across all panels/floating windows/dialogs, and fully aligned CN/EN/RU translations.
- UI: the "Visuals" tab is now "ESP", reorganized into four titled modules (Player Info / Standalone Features / Vehicle Info / Zombie Info) with flat toggles — radar lines no longer require enabling the matching info toggle first; "Enable cheat information rendering" and "Draw visual effects for a local player" removed; the master switch now defaults to ON.
- Feature: zombie info is now a health bar above each zombie's head (no more "Zombie / Health" text); new zombie radar (lines from zombies within 150 tiles + distance); vehicles show their localized name + top speed; toggle labels renamed across CN/EN/RU (Player radar (150 tiles) / Vehicle radar / Zombie radar / Show zombie HP, etc.).
- Feature: Reset Loot (F9) now refreshes instantly — containers are re-requested from the server right after the reset, so loot re-rolls immediately without reopening containers.
- Fixed: ESP round three from in-game feedback — the zombie HP bar is now a rounded pill (was diamond-shaped due to thick-line pointy ends); radar lines are now thin (0.5px); overlapping multi-line texts now step by font height (vehicle power/top speed, players' hand items); ESP no longer drifts behind entities when the camera is zoomed (removed a B41-era zoom division from the world-to-screen conversion); vehicles now show engine power/top speed instead of the name; the master switch is renamed "ESP master switch"; module order is now Standalone → Players → Vehicles → Zombies.
- Fixed: ESP round four from in-game feedback — ESP no longer misaligns after mouse-wheel zoom (reverted the previous world-to-screen change; the zoom division is required after all); the zombie HP bar is back to plain text "HP: xx"; all ESP texts are ~3 sizes smaller (same scaling as the Info page hints); vehicle power now matches the mechanics UI (was 10x the in-game value: 2188 → 218.8).
- Fixed: the Players tab no longer leaks its header info text outside the panel while scrolling — the manually drawn info rows were painted after the stencil was cleared, so scrolled-away lines weren't clipped; the form base class now has a renderContent hook that draws inside the clip region.
- UI: the Character tab is reorganized into five titled modules (Debug-Privilege Cheats / Combat / Items & Carry / Special Modes / Status & Needs) in the same boxed form as the Loot tab; checkboxes inside a module adapt to 1-3 columns, and the attack-speed multiplier entry moved into the Combat module.
- Renamed: "Bypass debug mode prohibition (Type 12)" is now "Unlock debug privileges (SP)"; the five features it unlocks are suffixed "(SP)" in CN/EN/RU (God mode / No clip / Invisibility / Zombies do not attack / Timed action instant).
- Fixed: the debug-privilege unlock no longer stomps the global Core.debug flag — multiplayer never touches it (previously it silently enabled vanilla debug hotkeys), and turning it off in single-player restores the value captured at startup instead of wiping -debug mode every frame; the dead read of the removed B41 option AntiCheatProtectionType12 is gone.
- Fixed: the Info tab anti-cheat status labels were misleading — "type 12" actually showed the movement anti-cheat; they now report Permission anti-cheat (AntiCheatPermission) and Movement anti-cheat (AntiCheatSpeed) separately, and the stale "disable types 12 and 8" hint text is updated (B42 has no numbered anti-cheat options).
- Renamed: "Timed Action Instant Cheat" is now "Instant progress bars" (CN/EN/RU).
- Renamed + fixed: "Disable recoil" is now "Marksman Mode" — clearing the recoil delay now has a multiplayer safety bound: the server rate-checks hits in a sliding window (150ms/hit for non-auto firearms, 15ms for full-auto; exceeding logs SuspiciousActivity and is handled per AntiCheatHit, kick by default), so in multiplayer full-auto guns get the delay lowered to 8 instead of zeroed — shots stay at least 8 frames apart (30ms at 266fps, a 2× margin over the 15ms threshold) — and the stock value is restored on fire-mode switch, feature toggle-off, or unequip so the boosted value can't linger on a semi-auto gun and break its 150ms limit; non-auto guns keep the stock value; singleplayer zeroes everything. Guaranteed crit/knockdown/instant aiming already worked in multiplayer (crit is rolled on the attacking client and travels inside the hit packet unvalidated; zombie knockdown is applied by the attacking client for locally-simulated zombies — the earlier claim based on the PVP-only KnockedDownAllowed gate was wrong and is corrected).
- Docs: the Farming tab plan is finalized but not yet implemented (analysis/耕种选项卡方案(待实现).md) — remove the farming cheat toggle from the Character tab and add a dedicated Farming tab covering all its operations (grow/water/pests/health/harvest/seedless sowing/batch); the server-side farming command path was verified line-by-line to have zero validation. Second revision: target plants are now everything in the player's own 3×3 ring (no nearest-plant lock), and wording follows the official CN term for plowing (ContextMenu_Dig, "翻土"); the notes state that tiles must be plowed before sowing.
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
