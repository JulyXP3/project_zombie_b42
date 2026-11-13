# Project Zomboid B42 - EtherHack Community Build

A community-maintained build of [EtherHack 3.1.0 (B42)](https://github.com/dei0/EtherHack) for Project Zomboid Build 42.

The main additions over the original mod are **Farming / Map teleport + vehicle teleport / Vehicle navigation mode (auto-drive along the road network) / full-vehicle auto-repair / free building / Reveal Map / True Night Vision / Combat enhancements + Temp Weapon / Item spawning (Item Spawn / Corpse spawn / Unbox spawn / Worn spawn) / ESP / Item Radar + Minimap Markers**, plus several fixes and robustness improvements for the B42 client Lua environment (Kahlua). See the "Feature Overview" below for the full list.

> Current version: **3.2.8-B42** (for Project Zomboid **42.22.2**).

> **Important:** **Any form of commercial use is prohibited** (including selling and paywalled downloads), and forks/modifications **must credit the original authors**. See the License section at the end for details.

## Feature Overview

UI: cyberpunk-style icon+label nav tiles, instant CN/EN/RU language switching, the menu reopens on your last tab and scroll position; the menu key and every hotkey can be rebound on the Settings page. Features grouped by nav page:

### Info

- Anticheat status (privilege / movement anticheat / custom log system / BikiniTools availability), warnings, authors & contact

### Survival

- **Items & carry**: auto-repair inventory items / unlimited carry weight
- **Moodles & needs**: infinite stamina / disable muscle strain / disable every moodle & need (fatigue/hunger/thirst/drunk/anger/fear/pain/panic/boredom/unhappiness/wetness/infection/false infection/...) / keep optimal calories / keep optimal weight / repair worn clothing / pad worn clothing with leather strips
- **Debug privileges (SP only)**: God mode / NoClip / Invisible / Creative mode / instant progress bars — requires "Unlock debug privileges (SP)" on the Other page first
- **Special modes**: Night Vision / **True Night Vision** (render-level full brightness — night tint and vision-cone overlay removed, unlit interiors no longer pitch black; client-side only) / Zombies don't attack the player (MP-ready)

### Combat

- **Combat enhancements**: attack-speed multiplier (1–2.5) / attack-range bonus (0–4) / One-Shot Kill / 90% Critical Chance (your ordinary weapon attacks against zombies; shoves, unarmed attacks and special weapon rules are excluded) / Headshot only for firearms (every hit is a headshot, 3× damage) / firearms never miss (ignores environment) / Group-Hit / No jamming / Shoot through closed windows (in vehicle) (with a firearm in hand, hit zombies outside while seated even with the window fully shut; melee and shoving are unaffected)
- **Super Group-Hit**: per-swing hit cap 10–20 (hits every enemy around you; the vanilla damage falloff across targets is removed, so every zombie takes full damage — combine with 90% Critical Chance / One-Shot Kill)
- **Temp Weapon**: locally spawn a firearm and swap it into your hands (searchable full firearm list + swap/restore)

### Items (SP)

- **Item creator**: filter by name/ID, grant ×1/×2/×5/×10

### Item Radar

- Full item database list (name/ID search) + "Show on map" + "ESP tracking" (three-way synced with the minimap "Items" toggle); tick several items and press "Add to list", remove entries in the "List" window, and "Track list" marks only the listed types (the list persists across restarts)

### Traps

- **Food spawn**: stand next to a placed trap, search and select a food item, then spawn; name, ID and count share one row, with repeated spawning by count.

### Swap

- Trade a **clothing/bag** item from your inventory for a chosen target item (the target list is searchable by name/ID, the sacrifice list has a refresh button; one item per swap)

### Player

- **Player info & recipes**: edit survival time / edit zombie kills / learn all available crafting recipes
- **VHS teaching (multiplayer only)**: trainable skill list (searchable) + level up the selected skill / level up all skills (requires a powered-on radio/TV/car radio within 10 tiles at volume 5+; if you have none of them, your radio is placed for you; 30s cooldown per skill, XP capped at level 3 by default depending on server config)
- **Traits**: add/remove traits; **Skills**: skill level ± / add XP / max all skills

### ESP

- Master switch + five modules: player info (nearby usernames, primary/secondary items), vehicle info (power/top speed/fuel/battery/engine condition), zombie info (overhead HP readout, zombie radar), animal info (show animal names, animal radar with distance lines within 150 tiles, multi-select species filter, track livestock trailers), standalone toggles (player radar 150 tiles, vehicle radar, 360° vision — every zombie/vehicle/player forced visible)

### Map

- **Reveal map**: reveals the entire unexplored area with one click (recorded server-side in multiplayer)
- **Right-click menu** (identical on the M world map, the panel and the minimap): fast-move (glides along walkable paths at 17 tiles/s, does not trigger the movement anticheat, unlimited range), fast-move (through walls), **vehicle teleport** (shown while seated in the driver's seat: hops the vehicle and everyone aboard to the target in steps bounded by the server's speed budget; can cross water), plus navigate-here / clear route / resume navigation
- Minimap: movable window + quick-toggle bar; show local player / other players / zombies / vehicles / items

### Loot

- **Ammo farming**: nested-bag magazine farm (multiplayer only; hold an empty magazine-fed firearm and place a loaded matching magazine inside an unequipped bag/toolbox in your main inventory)
- **Item Spawn (MP)**: with a fishing rod equipped, spawn any listed item into your inventory (~5-8s; a failed spawn leaves a log entry on the server; multiplayer only)
- **Corpse spawn**: spawn a corpse at your feet carrying the chosen items and loot it (multiplayer only; **it leaves a log entry on the server - use with caution**)
- **Unbox spawn**: keep a carrier (jar box / log stack x2-x4 / firewood bundle) in your backpack, pick a target and count, then spawn; craft the matching unpack recipe once to release any listed items (multiplayer only)
- **Multi-item list**: fishing-rod and corpse spawn accept several ticked items at once - add them to a list, adjust quantities, then spawn (fishing produces one of each item; unbox spawn stays single-item; multiplayer only)

### Build

- **Free build (blueprint placement)**: pick from a 21-entry catalog (walls/stairs/doors/furniture/crates/compost/rain barrels/campfire/floor..., searchable by name/ID; scans buildables from installed mods), place a ghost that follows the mouse (auto-facing + R to rotate), confirm (default `` ` ``, rebindable) to build and auto-chain the next blueprint, ESC/right-click to exit; no materials spent (keep planks/nails out of your pack and the nearby ground, or they will be consumed normally; multiplayer only). Note: legacy-chain "wooden floor" cannot be destroyed or dismantled once placed (labeled on the entry).

### Vehicles

- **Engine & starting**: start with any key (single shot / auto-retry) - any car key in your inventory can be inserted into the target vehicle's ignition; keyless start and guaranteed hotwire are not yet repaired and fuel/battery/engine condition still apply.
- **Repair**: auto-repair restores eligible vehicle parts to condition 95 without repair items; it excludes the engine, fuel tank, trunk, glove box, lightbar and other unsupported parts. Success depends on the character's skill level, with automatic retries and current-part, condition and attempt-count displays. Each action waits for the server result before the next step; if the result cannot be confirmed the job stops instead of resending. Stay in the same vehicle throughout; leaving or changing vehicles stops the job (multiplayer only).
- **Navigation mode**: auto-drive along the road network — get in the driver's seat, start the engine, press M and right-click "navigate here"; re-anchor while driving to reroute; cruise speed is adaptive (95% of the server speed limit) or set manually (honored as-is; going over the server limit counts as speeding on strict servers); any driving key takes over instantly; ends on arrival / exit / engine stall

### Farming

- **Crop management**: adjustable N×N range (default 3×3) — grow to next stage / grow to harvest / water to max / remove water / cure all / infect +25 / harvest / destroy / clear remains; live plant counter distinguishing stubble/empty tiles; growth applies by the next 10-minute in-game tick
- **Sowing**: full seed list with name/ID search, tool-free digging / seed-free sowing / sow on tilled ground around you, auto-watered after sowing

### Fun

- **Impersonate chat**: pick a channel (server-wide / say 30 tiles) and a target name, then send (the impersonated player does not see it)
- **Zombie bait & gather**: a toggle continuously attracts nearby zombies toward you; one click pulls available zombies into a ring around you (in multiplayer, gather only affects zombies already targeting you)
- **Zombie skin**: rotten face / heavily decayed / slightly decayed / restore skin
- **Fishing**: Perfect Fight (while fighting a fish your line never snaps or slips off and reeling is much faster — hold the reel button to bring it in quickly) / Auto Net Harvest (place a fishing net in water then enable it; while you stay nearby it keeps harvesting automatically — full bait, 15 hours' worth of catches per cycle, the net never breaks — and also grants fishing XP; turning it off stops it) / Fish Radar (outlines underwater fish schools and chum spots within 60 tiles; cyan highlighted tiles = bite speed (brighter = faster); red circle = fish present (bigger circle, more fish); orange circle = depleted or spooked school; brown circle = your chum spot)

### Create Char

- **Custom Edit**: freely add/remove traits (searchable list, click to toggle) and set skill levels (0-10) for your new character; lists are persisted
- **Creation Boost**: all traits / max skills / unlock all clothing (the game's own full outfit picker appears at character creation — dress freely) / trait points (slider)
- Everything applies the moment you confirm creation; untick before confirming to opt out

### Other

- Unlock debug privileges (SP) / Connect-time grant (multiplayer God mode + Invisible; arm before joining, changes need a reconnect) / Server sync protection (stops the server from rolling back stats & skills) / admin menu and admin-privilege attempt / debug menu (main) · game debug menu · vehicle mechanics menu · medical menu / grant all materials of the selected recipe / block the default logger / block files mentioning cheats / block files with suspicious words

### Settings

- UI language (CN/EN/RU) / key bindings (separate sub-panel) / accent colour / player · vehicle · zombie UI element colours / profile list (save/load/delete/reset to defaults) / reload all Lua elements / reset to defaults

### Other changes / fixes

See [UPDATELOG_EN.md](UPDATELOG_EN.md) for the full change history.

## Installation

**The game PC must have a full Windows x64 JDK 25 installed** (not a reduced JRE). The package no longer includes or downloads Java. Installation, launch coordination and removal use this external JDK; the game itself continues to use its bundled Java.

- Prefer setting `JAVA_HOME` to the JDK root, not its `bin` folder. If unset, `java.exe` is located through `PATH`, so adding the JDK's `bin` folder to `PATH` also works.
- An invalid explicitly set `JAVA_HOME` does not fall back to `PATH`. An unsupported version, architecture or missing component stops the operation before changing launch files or starting the game.
- Once the JDK is installed, the three-file installation, launch and removal flow needs neither network access nor an additional PowerShell installation.

Build requirements: a **full Windows x64 JDK 25**, **PowerShell 7 (`pwsh` available on PATH)** and the included **Gradle 9.1.0 wrapper**:

1. Copy the target game's `projectzomboid.jar` into `etherhack-src/lib/`, keeping the original name. Do not modify the game's original file.
2. Open `etherhack-src/build-fast.bat` in a text editor and fill in `BUILD_JAVA_HOME` (JDK root) and `BUILD_GAME_DIR` (game root) at the top.
3. Run `etherhack-src/build-fast.bat`. (Run `build.bat` if you need the full build, but it takes about 5-10 minutes.)
4. Take `modcore-3.2.8-B42.jar` from `etherhack-src/build/`, and copy it together with `etherhack-src/install.bat` and `etherhack-src/uninstall.bat` into the game root folder.
5. Run `install.bat` and wait until it finishes.

## Launch (two ways)

| Entry | Result |
| --- | --- |
| `ProjectZomboid64.bat` (or ShowConsole / OpenGLDebug variants) | Starts with the EtherHack menu |
| `ProjectZomboid64.exe` (including Steam's "Play" button) | Vanilla start, no EtherHack menu |

Press **Insert** in-game to open the panel (it is recommended to rebind the menu key from Insert in Settings).

## Uninstall

1. Quit the game completely (also exit Steam; do not stay on the main menu).
2. Copy `uninstall.bat` into the game root (the folder with `ProjectZomboid64.exe`).
3. Double-click `uninstall.bat` and wait until it reports completion.
4. Start the game with `ProjectZomboid64.exe` or Steam "Play" - it is the vanilla game with no EtherHack menu.

- Only `uninstall.bat` is needed; `install.bat` and `modcore-*.jar` are not required.
- Like installation, this needs JDK 25 on the PC.
- Saves, settings and logs are kept. If it reports a failure, make sure the game is fully closed and run it again.
- Older test builds may have left files behind. The current uninstaller does not guarantee removal of leftovers from every past version. Contact the maintainer before deleting anything from the game folder.

## Building from source

Requirements: a full Windows x64 JDK 25, PowerShell 7 and the included Gradle wrapper. Fill in the local paths at the top of both BAT files first:

```bat
set "BUILD_JAVA_HOME=E:\JDK25"
set "BUILD_GAME_DIR=E:\Steam\steamapps\common\ProjectZomboid"
```

Then run `etherhack-src/build.bat` (full tests and packaging), or `etherhack-src/build-fast.bat` (compilation and packaging only). Both check that the configured game JAR matches `lib/projectzomboid.jar`; the fast entry does not require a copied game runtime.

The output JAR is `etherhack-src/build/modcore-3.2.8-B42.jar`. The full entry manages its test copy under `temp/native-runtime-build/`: missing copies/files are rebuilt, and a changed game runtime selects a new copy. Unknown content, changed ownership records, redirected paths or concurrent builds are refused rather than removed automatically. Keep other tools and evidence in `temp/`; there is no need to empty the whole directory.

Automation can override the top configuration with `MODCORE_BUILD_JAVA_HOME` and `MODCORE_BUILD_GAME_DIR`; a positional game path takes precedence, and `PZ_GAME_DIR` remains supported. An invalid explicit configuration does not fall back to system Java. Set `MODCORE_NO_PAUSE=1` to disable the final pause. `build.bat --fast` still skips only some tests, so its output is also not fully verified.

Full acceptance requires zero failed, errored or skipped tests; `gradlew.bat jar` or the fast entry alone only packages code.

## Repository layout

| Path | Description |
|---|---|
| `etherhack-src/build/modcore-3.2.8-B42.jar` | 3.2.8 artifact (for 42.22.2): isolated checks passed; auto-repair accepted in-game (other features per their docs) |
| `etherhack-src/` | Full source (Gradle project, includes `build.bat` / `install.bat` / `uninstall.bat`) |
| `tests/` | Lua smoke tests + Kahlua compatibility checker |
| `analysis/` | Reverse-engineering evidence and design docs (`analysis/**/*.md` is tracked; decompiled sources stay local only) |

## Known limitations

- **The filter is a plain substring match** (name/ID, case-insensitive): ticking "Show on map" or "ESP tracking" with nothing selected tracks **every** item in the filtered list — e.g. searching `Wrench` also tracks `Ratchet Wrench`. To track a single item, select it in the list first.
- The item radar only scans **loaded** tiles within 56 tiles of the player (client-side limitation; the server's `processItems` registry is always empty on the client), and refreshes are throttled: inventory changes trigger a rescan after a 1s debounce; movement triggers a rescan after 5+ tiles but at most once every 4 seconds — hit positions are not per-frame live.
- Player inventories/equipment are intentionally excluded; other players' items appear with the 1–2s sync delay.
- Loot encryption on servers hides container contents from the client entirely.

## Acknowledgments

- Original mod [EtherHack](https://github.com/Yeet-Masta/Project-Zomboid-EtherHack) by Quzile & Yeet-Masta
- B42 port: [EtherHack B42](https://github.com/dei0/EtherHack) by dei0 (original repo no longer accessible; link kept for attribution)
- Maintained & extended by JulyXP3

## License

- The EtherHack base is © 2023 Quzile, licensed under the [MIT License](etherhack-src/LICENSE.txt);
- This repository's modifications and additions are licensed under the [PolyForm Noncommercial 1.0.0](LICENSE): free to use, modify and redistribute for noncommercial purposes; **any commercial use (selling, paywalled downloads, monetization) requires prior written permission from the author**;
- Forks and redistributions must credit the original authors (Quzile, Yeet-Masta, dei0, JulyXP3) and retain this license notice.
