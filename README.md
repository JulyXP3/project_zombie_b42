# Project Zomboid B42 - EtherHack Community Build

A community-maintained build of [EtherHack 3.1.0 (B42)](https://github.com/dei0/EtherHack) for Project Zomboid Build 42.

The main additions over the original mod are **Farming / Map teleport + vehicle teleport / Vehicle navigation mode (auto-drive along the road network) / Reveal Map / True Night Vision / Combat enhancements + Temp Weapon / Loot reroll + item spawning (Item Spawn / Corpse spawn / Timed spawn / Unbox spawn) / ESP / Item Radar + Minimap Markers**, plus several fixes and robustness improvements for the B42 client Lua environment (Kahlua). See the "Feature Overview" below for the full list.

> **Important:** **Any form of commercial use is prohibited** (including selling and paywalled downloads), and forks/modifications **must credit the original authors**. See the License section at the end for details.

## Feature Overview

UI: cyberpunk-style icon+label nav tiles, instant CN/EN/RU language switching, the menu reopens on your last tab and scroll position; the menu key and every hotkey can be rebound on the Settings page. Features grouped by nav page:

### Info

- Anticheat status (privilege / movement anticheat / custom log system / BikiniTools availability), warnings, authors & contact

### Survival

- **Items & carry**: auto-repair inventory items / unlimited carry weight
- **Moodles & needs**: infinite stamina / fast health regen (not godmode) / disable muscle strain / disable every moodle & need (fatigue/hunger/thirst/drunk/anger/fear/pain/panic/boredom/unhappiness/wetness/infection/false infection/...) / keep optimal calories / keep optimal weight / repair worn clothing / pad worn clothing with leather strips
- **Debug privileges (SP only)**: God mode / NoClip / Invisible / Creative mode / instant progress bars — requires "Unlock debug privileges (SP)" on the Other page first
- **Special modes**: Night Vision / **True Night Vision** (render-level full brightness — night tint and vision-cone overlay removed, unlit interiors no longer pitch black; client-side only) / Zombies don't attack the player (MP-ready)

### Combat

- **Combat enhancements**: attack-speed multiplier (1–2.5) / attack-range bonus (0–4) / One-Shot Kill / CritMax / Headshot only for firearms (every hit is a headshot, 3× damage) / firearms never miss (ignores environment) / Group-Hit / Increase Fire Rate / Infinite ammo (auto-refill; the refill count is set on the Loot page) / No jamming
- **Super Group-Hit**: per-swing hit cap 10–20 (hits every enemy around you; the vanilla damage falloff across targets is removed, so every zombie takes full damage — combine with CritMax / One-Shot Kill)
- **Temp Weapon**: locally spawn a firearm and swap it into your hands (searchable full firearm list + swap/restore)

### Items (SP)

- **Item creator**: filter by name/ID, grant ×1/×2/×5/×10

### Item Radar

- Full item database list (name/ID search) + "Show on map" + "ESP tracking" (three-way synced with the minimap "Items" toggle)

### Traps

- **Mode switch**: food / weapon; search + click to spawn (food: stand next to a placed trap; weapon: the trap is placed at your feet automatically and auto-collected, a few seconds per item); count can be looped

### Swap

- Trade a **clothing/bag** item from your inventory for a chosen target item (the target list is searchable by name/ID, the sacrifice list has a refresh button; one item per swap)

### Player

- **Player info & recipes**: edit survival time / edit zombie kills / learn all available crafting recipes
- **VHS teaching (multiplayer only)**: trainable skill list (searchable) + level up the selected skill / level up all skills (requires a powered-on radio/TV/car radio within 10 tiles at volume 5+; if you have none of them, your radio is placed for you; 30s cooldown per skill, XP capped at level 3 by default depending on server config)
- **Traits**: add/remove traits; **Skills**: skill level ± / add XP / max all skills

### ESP

- Master switch + four modules: player info (nearby usernames, primary/secondary items), vehicle info (power/top speed), zombie info (overhead HP readout, zombie radar), standalone toggles (player radar 150 tiles, vehicle radar, 360° vision — every zombie/vehicle/player forced visible)

### Map

- **Reveal map**: reveals the entire unexplored area with one click (recorded server-side in multiplayer)
- **Right-click menu** (identical on the M world map, the panel and the minimap): fast-move (glides along walkable paths at 17 tiles/s, does not trigger the movement anticheat, unlimited range), fast-move (through walls), **vehicle teleport** (shown while seated in the driver's seat: hops the vehicle and everyone aboard to the target in steps bounded by the server's speed budget; can cross water), plus navigate-here / clear route / resume navigation
- Minimap: movable window + quick-toggle bar; show local player / other players / zombies / vehicles / items

### Loot

- **Reset loot (F9)**: adjustable radius (default 10); reopened containers get re-rolled (gun cabinets/ammo boxes can yield weapons and ammo; multiplayer only)
- **Ammo farming**: spawn ammo per magazine/weapon type
- **Item Spawn (MP)**: with a fishing rod equipped, spawn any listed item into your inventory (~5-8s; a failed spawn leaves a log entry on the server; multiplayer only)
- **Corpse spawn**: spawn a corpse at your feet carrying the chosen items and loot it (multiplayer only; **it leaves a log entry on the server - use with caution**)
- **Timed spawn**: uses the game's own action so the item is real and persistent (visible to others), with an "Accelerate" toggle for its speed (multiplayer only)
- **Unbox spawn**: keep a carrier (jar box / log stack x2-x4 / firewood bundle) in your backpack, pick a target and count, then spawn; craft the matching unpack recipe once to release any listed items (multiplayer only)
- **Free build**: place walls without spending materials (keep planks/nails out of your pack and the nearby ground, or they will be consumed normally; multiplayer only)

### Vehicles

- **Engine & starting**: start engine unconditionally (once / auto-retry, auto-unchecks on success; the engine still needs fuel and battery)
- **Repair & supply**: repair vehicle (uses items already in this vehicle's trunk/glovebox/seats as tokens by default, so nothing is consumed; per-step overhead feedback) / direct repair (instant full condition, one server log line per part) / refuel
- **Remote entry**: enter the nearest vehicle within 20m (driver seat) / stuff the nearest other online player into the driver seat of the vehicle near them (multiplayer)
- **Navigation mode**: auto-drive along the road network — get in the driver's seat, start the engine, press M and right-click "navigate here"; re-anchor while driving to reroute; cruise speed is adaptive or set manually (hard-capped by the server speed limit); any driving key takes over instantly; ends on arrival / exit / engine stall

### Farming

- **Crop management**: adjustable N×N range (default 3×3) — grow to next stage / grow to harvest / water to max / remove water / cure all / infect +25 / harvest / destroy / clear remains; live plant counter distinguishing stubble/empty tiles; growth applies by the next 10-minute in-game tick
- **Sowing**: full seed list with name/ID search, tool-free digging / seed-free sowing / sow on tilled ground around you, auto-watered after sowing

### Fun

- **Impersonate chat**: pick a channel (server-wide / say 30 tiles) and a target name, then send (the impersonated player does not see it)
- **Zombie skin**: rotten face / heavily decayed / slightly decayed / restore skin

### Create Char

- **Custom Edit**: freely add/remove traits (searchable list, click to toggle) and set skill levels (0-10) for your new character; lists are persisted
- **Creation Boost**: all traits / max skills / unlock all clothing (the game's own full outfit picker appears at character creation — dress freely) / trait points (slider)
- Everything applies the moment you confirm creation; untick before confirming to opt out

### Other

- Unlock debug privileges (SP) / Server sync protection (stops the server from rolling back stats & skills) / admin menu and admin-privilege attempt / debug menu (main) · game debug menu · vehicle mechanics menu · medical menu / grant all materials of the selected recipe / block the default logger / block files mentioning cheats / block files with suspicious words

### Settings

- UI language (CN/EN/RU) / key bindings (separate sub-panel) / accent colour / player · vehicle · zombie UI element colours / profile list (save/load/delete/reset to defaults) / reload all Lua elements / reset to defaults

### Other changes / fixes

See [UPDATELOG_EN.md](UPDATELOG_EN.md) for the full change history.

## Installation

Requirements: **JDK 25** and **Gradle 9.1.0**. The build runs through the included Gradle wrapper (`gradlew.bat`), which downloads Gradle automatically on first run — or you can use a locally installed Gradle.

1. Prepare the build dependency: copy `projectzomboid.jar` from the game root directory into `etherhack-src/lib/`, renamed to `zombie.jar` (it is only read at compile time and never modified).
2. Open `etherhack-src/build.bat`, fill in your `JAVA_HOME` path, save, and run it.
3. Take `modcore-3.2.5-B42.jar` from the `build` directory.
4. Copy the jar together with `etherhack-src/install.bat` (and optionally `etherhack-src/uninstall.bat`) into the **game root directory**.
5. Run `install.bat` to install the mod.

The installer deletes the jar automatically after a successful install. To uninstall: either run `install.bat` again later (with **no** `modcore-*.jar` in the folder - the merged script switches to uninstall mode), or run the standalone `uninstall.bat` (works at any time, whether or not the jar is present). Both remove the injected `zombie\` folder, the unpacked `modcore\` folder and `%USERPROFILE%\Zomboid\modcore.bin`, and never touch the game itself. Never delete `projectzomboid.jar` - that is the game itself.

In-game: press **Insert** to open the EtherHack panel.

## Building from source

Requirements: JDK 25, Gradle wrapper included.

```bat
cd etherhack-src
gradlew.bat jar
```

The output jar is at `etherhack-src/build/modcore-3.2.5-B42.jar`. The build embeds the Lua sources from `src/main/resources/modcore/lua/`.

## Testing

```bat
rem Lua smoke test (scan + debounce + movement refresh + toggle logic)
temp\tools\lua51\lua5.1.exe tests\run_scan_test.lua etherhack-src\src\main\resources\modcore\lua\components\ui\EtherItemSearch.lua

rem Kahlua compatibility static check (banned API patterns)
temp\tools\lua51\lua5.1.exe tests\check_kahlua_compat.lua etherhack-src\src\main\resources\modcore\lua\components\ui\EtherItemSearch.lua etherhack-src\src\main\resources\modcore\lua\components\ui\UIItemTables.lua etherhack-src\src\main\resources\modcore\lua\components\ui\UIMap.lua etherhack-src\src\main\resources\modcore\lua\components\ui\UIMovableMiniMap.lua
```

Note: `temp/` is a local scratch directory and is not part of the repository.

## Repository layout

| Path | Description |
|---|---|
| `etherhack-src/build/modcore-3.2.5-B42.jar` | Ready-to-use build (current release) |
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
