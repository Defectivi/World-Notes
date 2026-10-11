# World Notes & Coordinates

A client-side Fabric and Forge terrain minimap and bookmark manager for Minecraft Java.

Press **B** to open the bookmark manager. The keybind can be changed in Minecraft's Controls settings.

## Current version: 0.2.2-beta

Version targets checked on October 11, 2026 against [Fabric metadata](https://meta.fabricmc.net/v2/versions/game):

| Build channel | Minecraft | Fabric API |
| --- | --- | --- |
| release (default) | 26.3 | 0.162.0+26.3 |
| snapshot | 26.4-snapshot-3 | 0.162.2+26.4 |
| legacy | 26.2 | 0.161.0+26.2 |

Install only the jar matching your Minecraft version, with Fabric Loader 0.19.5 or newer and the matching Fabric API. Java 25 is required. Each jar declares its exact Minecraft target; future snapshots need a dependency update and a new build before they can be claimed as supported.

### Forge

Use the separate `world-notes-forge` jar with **Minecraft 26.3 and Forge 66.0.10 or newer in the 66.x series**. Install it on your client; servers do not need the mod. The Forge jar includes the same minimap, bookmark screens, tracking, keys, and local JSON saves, and does not need Fabric API. Choose the jar for your installed loader.

Forge support targets 26.3 only. [Forge's official version list](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html) has no 26.4 snapshot build as of October 11, 2026; the snapshot channel remains Fabric-only. Forge and NeoForge are separate loaders; this build targets Forge.

## Minimap

- North-up terrain map in the top-right corner, with your position and facing direction.
- **M** toggles the map; **N** cycles 48, 96, and 192 blocks of coverage. Both keys are configurable in Controls.
- Cyan markers show visible bookmarks in the current world/server and dimension; the tracked bookmark is gold. Hidden bookmarks stay hidden.
- Samples only chunks already loaded on the client. Unknown terrain appears dark. No server installation or permissions are needed.
- Terrain refreshes incrementally, with a fixed sampling budget per client tick. This is a local surface map, with no entity radar or persistent terrain history.
- In ceiling dimensions such as the Nether, samples look for cave floors near your elevation; columns with no nearby floor appear dark. Above the roof, the map uses the surface again.
- Visibility and zoom are saved in `config/worldnotes/minimap.json`. The map respects F1 and hides when the scaled window is too small.
- Tracking guidance appears beneath the map.

## Build

```powershell
.\gradlew.bat build
.\gradlew.bat build -Pchannel=snapshot
.\gradlew.bat build -Pchannel=legacy
.\gradlew.bat -p forge build
# Or build all Fabric channels and Forge:
.\build-all.bat
```

Fabric jars are written to `build/libs/`; Forge jars go to `forge/build/libs/`. CI compiles all Fabric channels and Forge independently. Compilation checks API compatibility; in-game testing is still needed for rendering and interaction.

The common client features live in `src/client/java`. Fabric registration lives in `src/fabric/java`; Forge registration and metadata live in `forge/src/main`. Both loaders use `config/worldnotes/bookmarks.json` and `config/worldnotes/minimap.json` in their game directory.

## Bookmarks

- Search names, notes, dimensions, and coordinates without losing typing focus.
- Compact previews and responsive pagination keep long notes inside the list; open a bookmark to read/edit the full values.
- Deepslate textures, faint cartography grids, dimension accents, and tracked-location highlights provide a consistent background for the manager, editor, and minimap.
- Editing uses a separate draft. Cancel or Escape discards it; changing colour or dimension never saves implicitly. New bookmarks must be saved before tracking.
- Invalid or non-finite coordinates show an error instead of silently falling back. Horizontal coordinates must be within ±30 million and Y within ±20 million.
- Saves use temporary files and atomic replacement where supported, retaining the previous document in `config/worldnotes/bookmarks.json.bak`. A damaged primary loads the backup; if neither file can be read, saving is blocked to preserve the damaged data. Check the game log and restore a valid JSON file before restarting.
- Teleport messages report a request; the server decides whether it succeeds. Cross-dimension teleport buttons also check availability of `execute in`.

The Forge build runs persistence regression tests for draft isolation, world separation, backup recovery, write failures, and validation. Run them with `.\gradlew.bat -p forge test`.

- Create bookmarks at the player's current position or enter coordinates manually.
- Store a name, optional note, dimension, and coordinates rounded to two decimal places.
- Track one bookmark per world with direction, distance, and destination dimension guidance.
- View bookmark details separately from editing.
- Use dimension filters for Overworld, Nether, End, or all dimensions.
- Use native Minecraft icons for adding, editing, and teleporting bookmarks.
- Teleport controls respect the player's command permissions and show request messages.
- Choose black, red, yellow, blue, orange, purple, green, or pink name colors and gradients.
- View dimension-specific colors and backgrounds throughout the interface.
- Bookmarks are separated between singleplayer worlds.
- Delete actions require confirmation because they cannot be undone.
- Hide bookmarks from the main list and manage them from the hidden-bookmarks view.

## Version history

### 0.1.4-stable

Fixed pagination after deleting bookmarks, rejected invalid coordinate values, and hardened saved bookmark handling.

### 0.1.4-beta

Added hidden-bookmark management with filtering, editing, and confirmed unhide actions.

### 0.1.3-beta

Introduced the redesigned bookmark interface, editable name colors, dimension styling, world-separated bookmarks, permission-aware teleporting, custom teleport confirmations, tooltips, and irreversible-delete confirmation.

### 0.1.2-beta

Introduced single-bookmark tracking with direction, distance, target dimension, and tracked bookmark name guidance.

See [CHANGELOG.md](CHANGELOG.md) for the complete release history.
