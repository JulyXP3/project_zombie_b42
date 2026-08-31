# Project Zomboid B42 - EtherHack Community Build

A community-maintained build of [EtherHack 3.1.0 (B42)](https://github.com/dei0/EtherHack) for Project Zomboid Build 42.

The main additions over the original mod are **Farming / Map teleport (pathfind & fast-move) / Reveal Map / True Night Vision / Vehicles / Loot reroll / ESP / Item Search + Minimap Markers**, plus several fixes and robustness improvements for the B42 client Lua environment (Kahlua). See the "Feature Overview" below for the full list.

> **Important:** **Any form of commercial use is prohibited** (including selling and paywalled downloads), and forks/modifications **must credit the original authors**. See the License section at the end for details.

## Feature Overview

UI: cyberpunk-style icon+label nav tiles, instant CN/EN/RU language switching, the menu reopens on your last tab and scroll position. Features grouped by nav page:

### Character

- **Combat**: One-Shot Kill / CritMax / Headshot only for firearms (every hit is a headshot, 3× damage) / Increase Fire Rate / Group-Hit on zombies / Zombies don't attack the player (MP-ready) / Infinite ammo (auto-refill, ammo count configurable) / No jamming / Infinite durability for held items / Auto-repair inventory items
- **Survival**: Unlimited carry / Infinite stamina / Fast health regen (not godmode) / Disable muscle strain / Disable every moodle & need (fatigue/hunger/thirst/drunk/anger/fear/pain/panic/boredom/unhappiness/wetness/infection/false infection/...) / Keep optimal calories & weight
- **Special modes**: Creative mode (high risk) / Night Vision / **True Night Vision** (render-level full brightness — night tint and vision-cone overlay removed, unlit interiors no longer pitch black; client-side only) / God mode / NoClip / Invisible (last three: SP only, requires "Unlock debug privileges (SP)")

### Items

- **Item creator**: filter by name/category/ID, grant ×1/×2/×5/×10
- **Item search + minimap markers**: scans loaded tiles within 56 tiles (floor ±1) — furniture/containers, ground items & bags, corpses, vehicle containers; matches shown as gray squares (with counts); markers refresh as you move; minimap quick-toggle bar (Me/Players/Vehicles/Zombies/Items) two-way synced with the Map tab checkboxes

### Traps

- Search & spawn food (stand next to a placed trap; multiplayer)

### Player

- Skill levels ± / add XP / max all skills; trait add/remove; calorie editing; survival-days / zombie-kill editing (enable "Server sync protection" in multiplayer)

### ESP

- Master switch + four modules: player info (nearby usernames, primary/secondary items), vehicle info (power/top speed), zombie info (overhead HP bar, zombie radar), standalone toggles (player radar 150 tiles, vehicle radar, 360° vision)

### Map

- **Reveal map**: reveals the entire unexplored area with one click (recorded server-side in multiplayer)
- **Pathfind & fast-move**: right-click any spot on the map — glides at 18 tiles/s along walkable paths, no longer triggers the movement anticheat, unlimited range, WASD/Space cancels anytime; single-player keeps instant teleport
- Minimap: movable window + quick-toggle bar; show local player / other players / zombies / vehicles / items

### Loot

- **Reset loot (F9)**: adjustable radius (default 10); reopened containers get re-rolled (gun cabinets/ammo boxes can yield weapons and ammo; multiplayer only)
- **Ammo farming**: spawn ammo per magazine/weapon type

### Vehicles

- **Start engine unconditionally** (once / auto-retry, auto-unchecks on success) / repair / refuel (the engine still needs fuel and battery; must be seated in the vehicle)

### Farming

- **Crop management**: adjustable N×N range (default 3×3) — grow to next stage / grow to harvest / water to max / remove water / cure all / infect +25 / harvest / destroy / clear remains; live plant counter distinguishing stubble/empty tiles; growth applies by the next 10-minute in-game tick
- **Sowing**: full seed list with name/ID search, tool-free digging, seed-free sowing, auto-watered after sowing

### Create Char

- **Custom Edit**: freely add/remove traits (searchable list, click to toggle) and set skill levels (0-10) for your new character; lists are persisted
- **Creation Boost**: all traits / max skills / unlock all clothing (the game's own full outfit picker appears at character creation — dress freely) / trait points (slider)
- Everything applies the moment you confirm creation; untick before confirming to opt out

### Other changes / fixes

See [UPDATELOG_EN.md](UPDATELOG_EN.md) for the full change history.

## Installation

Requirements: **JDK 25** and **Gradle 9.1.0**. The build runs through the included Gradle wrapper (`gradlew.bat`), which downloads Gradle automatically on first run — or you can use a locally installed Gradle.

1. Prepare the build dependency: copy `projectzomboid.jar` from the game root directory into `etherhack-src/lib/`, renamed to `zombie.jar` (it is only read at compile time and never modified).
2. Open `etherhack-src/build.bat`, fill in your `JAVA_HOME` path, save, and run it.
3. Take `EtherHack-3.2.1-B42.jar` from the `build` directory.
4. Copy the jar together with `etherhack-src/install.bat` into the **game root directory**.
5. Run `install.bat` to install the mod (requires a JDK on the system).

In-game: press **Insert** to open the EtherHack panel.

## Building from source

Requirements: JDK 25, Gradle wrapper included.

```bat
cd etherhack-src
gradlew.bat jar
```

The output jar is at `etherhack-src/build/EtherHack-3.2.1-B42.jar`. The build embeds the Lua sources from `src/main/resources/EtherHack/lua/`.

## Testing

```bat
rem Lua smoke test (scan + debounce + movement refresh + toggle logic)
temp\tools\lua51\lua5.1.exe tests\run_scan_test.lua etherhack-src\src\main\resources\EtherHack\lua\components\ui\EtherItemSearch.lua

rem Kahlua compatibility static check (banned API patterns)
temp\tools\lua51\lua5.1.exe tests\check_kahlua_compat.lua etherhack-src\src\main\resources\EtherHack\lua\components\ui\EtherItemSearch.lua etherhack-src\src\main\resources\EtherHack\lua\components\ui\UIItemTables.lua etherhack-src\src\main\resources\EtherHack\lua\components\ui\UIMap.lua etherhack-src\src\main\resources\EtherHack\lua\components\ui\UIMovableMiniMap.lua
```

Note: `temp/` is a local scratch directory and is not part of the repository.

## Repository layout

| Path | Description |
|---|---|
| `etherhack-src/build/EtherHack-3.2.1-B42.jar` | Ready-to-use build (current release) |
| `etherhack-src/` | Full source (Gradle project, includes `build.bat` / `install.bat`) |
| `tests/` | Lua smoke tests + Kahlua compatibility checker |
| `analysis/` | Decompiled class extracts used for verification |

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
