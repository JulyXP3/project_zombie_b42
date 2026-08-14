# Project Zomboid B42 - EtherHack Community Build

A community-maintained build of [EtherHack 3.1.0 (B42)](https://github.com/dei0/EtherHack) for Project Zomboid Build 42.

The main addition over the original mod is an **Item Search + Minimap Marker** feature, plus several fixes and robustness improvements for the B42 client Lua environment (Kahlua).

> **Important:** This project is only **temporarily maintained**. Anyone who needs it may fork/clone this repository and continue development. Thanks!

## Feature Overview

### Item Search & Minimap Markers (new)

- In the **Item Creator** page (main panel), filter items by name or ID, select an item, and click **"Show on map"**.
- The mod scans loaded world tiles within a radius of 48 tiles (player floor 卤1) for matching items:
  - Furniture/container contents (`IsoObject` containers)
  - Floor items and bags on the ground (including bag contents)
  - Items on corpses (corpse container via `getDeadBodys()` 鈫?`getContainer()`)
  - Vehicle part containers (trunk, seats, etc.)
- Matches are drawn on the movable minimap as **gray squares**, same size as player/zombie markers; multiple items on one tile show a count.
- Markers follow the player: re-scan triggers when you walk 5+ tiles (cooldown 2s), when your inventory count changes (debounced 1s), or when the inventory window container set changes.
- **Minimap quick-toggle bar** (top of the movable minimap window): `Me / Players / Vehicles / Zombies / Items` 鈥?white = on, gray = off. Turning "Items" off clears the markers and drops all per-tick cost; turning it back on silently re-scans the last search target.
- The **Map** tab checkboxes (Show local player / Show other players / Show vehicles / Show zombies / Show items) are **two-way synced** with the minimap quick-toggle bar (toggling either side updates the other immediately, and the state is persisted to config).
- Close the minimap: click the **X** button on the window.

### Other changes / fixes

See [CHANGELOG.md](CHANGELOG.md) for the full change history.

## Installation

Requirements: **JDK 17+** and **Gradle 9.1.0**. The build runs through the included Gradle wrapper (`gradlew.bat`), which downloads Gradle automatically on first run — or you can use a locally installed Gradle.

1. Prepare the build dependency: copy `projectzomboid.jar` from the game root directory into `etherhack-src/lib/`, renamed to `zombie.jar` (it is only read at compile time and never modified).
2. Open `etherhack-src/build.bat`, fill in your `JAVA_HOME` path, save, and run it.
3. Take `EtherHack-3.1.6-B42.jar` from the `build` directory.
4. Copy the jar together with `etherhack-src/install.bat` into the **game root directory**.
5. Run `install.bat` to install the mod (requires a JDK on the system).

In-game: press **Insert** to open the EtherHack panel.

## Building from source

Requirements: JDK 17+, Gradle wrapper included.

```bat
cd etherhack-src
gradlew.bat jar
```

The output jar is at `etherhack-src/build/EtherHack-3.1.6-B42.jar`. The build embeds the Lua sources from `src/main/resources/EtherHack/lua/`.

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
| `EtherHack-3.1.6-B42.jar` | Ready-to-use build (current release) |
| `etherhack-src/` | Full source (Gradle project, includes `build.bat` / `install.bat`) |
| `tests/` | Lua smoke tests + Kahlua compatibility checker |
| `鍒嗘瀽鎶ュ憡.md` | Analysis report (Chinese): feasibility study, decompilation evidence, scanning design and limitations |
| `analysis/` | Decompiled class extracts used for verification |

## Known limitations

- **Item radar name-match bug**: when a search filter is active, clicking "Show on map" tracks **every** item in the filtered list (the name filter is a substring match). For example, searching `Wrench` will also track `Ratchet Wrench`. To track a single item only, clear the filter text and select the item directly.
- Only **loaded** chunks around the player can be scanned (client-side limitation; the server's `processItems` registry is empty on the client).
- Player inventories/equipment are intentionally excluded; other players' items appear with the 1鈥?s sync delay.
- Loot encryption on servers hides container contents from the client entirely.

## Acknowledgments

- Original mod: [EtherHack](https://github.com/dei0/EtherHack) by Quzile
- B42 port: dei0

## Disclaimer

- This project was developed **using only the Deepseek V4F model**. Using GPT or Claude models would likely produce better results.
- This is a temporary maintenance fork. If you find it useful, feel free to fork and continue development. Thanks!
