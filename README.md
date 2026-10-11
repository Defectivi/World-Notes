# World Notes & Coordinates

A client-side Fabric terrain minimap and bookmark manager for Minecraft Java.

Press **B** to open the bookmark manager. The keybind can be changed in Minecraft's Controls settings.

## Current version: 0.2.0-beta

Version targets checked on October 11, 2026 against [Fabric metadata](https://meta.fabricmc.net/v2/versions/game):

| Build channel | Minecraft | Fabric API |
| --- | --- | --- |
| release (default) | 26.3 | 0.162.0+26.3 |
| snapshot | 26.4-snapshot-3 | 0.162.2+26.4 |
| legacy | 26.2 | 0.161.0+26.2 |

Install only the jar matching your Minecraft version, with Fabric Loader 0.19.5 or newer and the matching Fabric API. Java 25 is required. Each jar declares its exact Minecraft target; future snapshots need a dependency update and a new build before they can be claimed as supported.

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
# Or build all three:
.\build-all.ps1
```

Jars are written to `build/libs/` with the Minecraft version in the filename. CI compiles each channel independently. Compilation checks API compatibility; in-game testing is still needed for rendering and interaction.

## Bookmarks

- Create bookmarks at the player's current position or enter coordinates manually.
- Store a name, optional note, dimension, and coordinates rounded to two decimal places.
- Track one bookmark per world with direction, distance, and destination dimension guidance.
- View bookmark details separately from editing.
- Use dimension filters for Overworld, Nether, End, or all dimensions.
- Use native Minecraft icons for adding, editing, and teleporting bookmarks.
- Teleport controls respect the player's command permissions and provide confirmation messages.
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
