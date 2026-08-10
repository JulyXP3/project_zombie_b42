# Project Zomboid B42 - EtherHack Community Build

A community-maintained build of [EtherHack 3.1.0 (B42)](https://github.com/dei0/EtherHack) for Project Zomboid Build 42.

The main addition over the original mod is an **Item Search + Minimap Marker** feature, plus several fixes and robustness improvements for the B42 client Lua environment (Kahlua).

> **Important:** This project is only **temporarily maintained**. Anyone who needs it may fork/clone this repository and continue development. Thanks!

## Feature Overview

### Item Search & Minimap Markers (new)

- In the **Item Creator** page (main panel), filter items by name or ID, select an item, and click **"Show on map"**.
- The mod scans loaded world tiles within a radius of 48 tiles (player floor ±1) for matching items:
  - Furniture/container contents (`IsoObject` containers)
  - Floor items and bags on the ground (including bag contents)
  - Vehicle part containers (trunk, seats, etc.)
- Matches are drawn on the movable minimap as **gray squares**, same size as player/zombie markers; multiple items on one tile show a count.
- Markers follow the player: re-scan triggers when you walk 5+ tiles (cooldown 2s), when your inventory count changes (debounced 1s), or when the inventory window container set changes.
- **Minimap quick-toggle bar** (top of the movable minimap window): `Me / Players / Vehicles / Zombies / Items` — white = on, gray = off. Turning "Items" off clears the markers and drops all per-tick cost; turning it back on silently re-scans the last search target.
- Close the minimap: click the **X** button on the window.

### Other changes / fixes

- **Event-driven refresh** instead of a timer: zero idle cost while markers are hidden.
- **Kahlua compatibility hardening** — replaced APIs that do not exist in the B42 Lua VM (Kahlua): `next()`, `ISUIElement.getVisible()`, `VehicleParts.getParts()`, `ItemContainer.size()`. A static checker (`tests/check_kahlua_compat.lua`) blocks these patterns at build time.
- Minimap default size changed to 300×300.
- Credits string updated (see `Info.java`).

## Installation

1. Open `etherhack-src/build.bat`, fill in your `JAVA_HOME` path, save, and run it.
2. Take `EtherHack-3.1.2-B42.jar` from the `build` directory.
3. Copy the jar together with `etherhack-src/install.bat` into the **game root directory**.
4. Run `install.bat` to install the mod (requires a JDK on the system).

In-game: press **Insert** to open the EtherHack panel.

## Building from source

Requirements: JDK 17+, Gradle wrapper included.

```bat
cd etherhack-src
gradlew.bat jar
```

The output jar is at `etherhack-src/build/EtherHack-3.1.2-B42.jar`. The build embeds the Lua sources from `src/main/resources/EtherHack/lua/`.

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
| `EtherHack-3.1.2-B42.jar` | Ready-to-use build (current release) |
| `etherhack-src/` | Full source (Gradle project, includes `build.bat` / `install.bat`) |
| `tests/` | Lua smoke tests + Kahlua compatibility checker |
| `分析报告.md` | Analysis report (Chinese): feasibility study, decompilation evidence, scanning design and limitations |
| `analysis/` | Decompiled class extracts used for verification |

## Known limitations

- Only **loaded** chunks around the player can be scanned (client-side limitation; the server's `processItems` registry is empty on the client).
- Player inventories/equipment are intentionally excluded; other players' items appear with the 1–2s sync delay.
- Loot encryption on servers hides container contents from the client entirely.

## Acknowledgments

- Original mod: [EtherHack](https://github.com/dei0/EtherHack) by Quzile
- B42 port: dei0

## Disclaimer

- This project was developed **using only the Deepseek V4F model**. Using GPT or Claude models would likely produce better results.
- This is a temporary maintenance fork. If you find it useful, feel free to fork and continue development. Thanks!
