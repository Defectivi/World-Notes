# World Notes & Coordinates

A client-side Fabric, Forge, and NeoForge terrain minimap and bookmark manager for Minecraft Java.

Press **B** to open the bookmark manager. The keybind can be changed in Minecraft's Controls settings.

## Current version: 1.0.0 stable

Version targets checked on October 11, 2026 against [Fabric metadata](https://meta.fabricmc.net/v2/versions/game):

| Build channel | Minecraft | Fabric API |
| --- | --- | --- |
| release (default) | 26.3 | 0.162.0+26.3 |
| snapshot | 26.4-snapshot-3 | 0.162.2+26.4 |
| legacy | 26.2 | 0.161.0+26.2 |
| lts | 26.1.2 | 0.155.3+26.1.2 |

Install only the jar matching your Minecraft version, with Fabric Loader 0.19.5 or newer and the matching Fabric API. Java 25 is required. Each jar declares its exact Minecraft target; future snapshots need a dependency update and a new build before they can be claimed as supported.

### Forge

Use the separate `world-notes-forge` jar with **Minecraft 26.3 / Forge 66.0.10+ within 66.x**, or **Minecraft 26.1.2 / Forge 64.1.3+ within 64.x**. Install it on your client; servers do not need the mod. The Forge jar includes the same minimap, bookmark screens, tracking, keys, and local JSON saves, and does not need Fabric API. Choose the jar for your installed loader and Minecraft version.

[Forge's official version list](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html) has no 26.4 snapshot build as of October 11, 2026; the snapshot channel remains Fabric-only.

### NeoForge

Use the separate `world-notes-neoforge` jar with **Minecraft 26.1.2 / NeoForge 26.1.2.116+ within 26.1.2**, or **Minecraft 26.3 / NeoForge 26.3.0.69-beta+ within 26.3**. These dependencies were checked against [NeoForge's official Maven metadata](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml). NeoForge's 26.3 loader is currently beta; World Notes 1.0.0 is the mod's stable version. Forge and NeoForge are distinct loaders and need their respective jars. NeoForge does not need Fabric API.

The `lts` channel is a pinned 26.1.2 compatibility target; it does not promise upstream long-term maintenance. Separate compile-checked HUD adapters handle Minecraft's 26.1.2 and newer GUI APIs.

## Minimap

- North-up map with your position and facing direction. The default **32-chunk radius** covers 1,024 blocks across (64 chunks).
- **M** opens/closes the expanded map; **N** cycles 8, 16, and 32-chunk radii. Both keys are configurable in Controls. Hover a marker in the expanded map to identify it.
- Purple markers show visible bookmarks; the tracked bookmark is gold. Hidden bookmarks stay hidden.
- Yellow identifies dropped items, green identifies neutral/passive mobs, red identifies hostile mobs, and cyan identifies other players. Only entities currently known to your client are shown; terrain history does not store entities.
- Crowded entities share a marker with a count; mixed groups show their category colours. Hover to see category totals, names, and exact coordinates. Your position has a white ring and a **You** label. Click the expanded map's legend buttons to filter categories; controls move to a sidebar on wide windows.
- Items and mobs within 24 blocks of your elevation are shown by default, so underground entities do not cover the surface map. Disable **Nearby heights** in settings to show all elevations. Other players are shown at every elevation.
- Samples only chunks already received by the client. Unknown terrain appears dark, even if the server generated it elsewhere. The map never asks the server to load distant chunks.
- Explored terrain is saved asynchronously in `config/worldnotes/terrain/`, separated by world/server, dimension, and Nether elevation band. Cached terrain remains visible after chunks unload and after restarting. Samples use four-block spacing and a fixed budget per tick. Historical terrain is a snapshot; remove its cache to resurvey changed terrain.
- Nether samples look for cave floors near your elevation. Above the roof, the map uses the surface again.
- The settings button above the hidden-bookmarks button controls minimap visibility, entity categories, rainbow highlighting, size, radius, corner placement, and custom horizontal/vertical percentages. Done saves; Cancel discards changes. Settings persist in `config/worldnotes/minimap.json`.
- The HUD respects F1 and hides the minimap when the scaled window is too small. The expanded map remains available with M when the minimap is disabled.
- Dimension labels use green Overworld, fiery Nether, and purple End gradients. The day counter sits directly below the coordinates.
- Tracking a visible bookmark highlights the exact block containing its coordinates with an animated rainbow outline and soft halo, within 1,024 blocks and in the same dimension. This is client-only rendering and never changes block state or sends glow updates to the server. Negative and fractional coordinates use Minecraft block-floor rules.
- A diary and feather-quill icon identifies the mod. Jar filenames and mod-list names distinguish Fabric, Forge, and NeoForge while retaining version 1.0.0.

## Build

```powershell
.\gradlew.bat build
.\gradlew.bat build -Pchannel=snapshot
.\gradlew.bat build -Pchannel=legacy
.\gradlew.bat build -Pchannel=lts
.\gradlew.bat -p forge build
.\gradlew.bat -p forge build -Pchannel=lts
.\gradlew.bat -p neoforge build
.\gradlew.bat -p neoforge build -Pchannel=lts
# Or build all eight targets:
.\build-all.bat
```

Fabric jars are written to `build/libs/`; Forge jars go to `forge/build/libs/`; NeoForge jars go to `neoforge/build/libs/`. CI compiles all eight targets independently. Compilation checks API compatibility; in-game testing is still needed for rendering and interaction.

The common client features live in `src/client/java`; version-specific HUD calls live in `src/compat/`. Fabric registration lives in `src/fabric/java`; Forge and NeoForge registration and metadata live in their respective `src/main` folders. All loaders use `config/worldnotes/bookmarks.json` and `config/worldnotes/minimap.json` in their game directory.

Pushing a `v<mod_version>` tag publishes a GitHub stable release after all CI builds pass. Downloads include seven stable client jars and SHA-256 checksums; snapshot and source jars are excluded. See [RELEASE_NOTES.md](RELEASE_NOTES.md) for installation targets and validation limits.

## Bookmarks

- Search names, notes, dimensions, and coordinates without losing typing focus.
- Compact previews and responsive pagination keep long notes inside the list; open a bookmark to read/edit the full values.
- Deepslate textures, faint cartography grids, dimension accents, and tracked-location highlights provide a consistent background for the manager, editor, and minimap.
- Editing uses a separate draft. Cancel or Escape discards it; changing colour or dimension never saves implicitly. New bookmarks must be saved before tracking.
- Invalid or non-finite coordinates show an error instead of silently falling back. Horizontal coordinates must be within Â±30 million and Y within Â±20 million.
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
