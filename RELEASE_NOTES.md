# World Notes 1.0.0

The first 1.0 stable release adds Minecraft 26.1.2 compatibility and native NeoForge support to the client-side minimap and bookmark manager.

Install one jar matching both your Minecraft version and mod loader. Java 25 is required. Servers do not need World Notes.

| Loader | Minecraft | Required dependency |
| --- | --- | --- |
| Fabric | 26.1.2 | Fabric Loader 0.19.5+, Fabric API 0.155.3+26.1.2 |
| Fabric | 26.2 | Fabric Loader 0.19.5+, Fabric API 0.161.0+26.2 |
| Fabric | 26.3 | Fabric Loader 0.19.5+, Fabric API 0.162.0+26.3 |
| Forge | 26.3 | Forge 66.0.10+ within 66.x |
| Forge | 26.1.2 | Forge 64.1.3+ within 64.x |
| NeoForge | 26.1.2 | NeoForge 26.1.2.116+ within 26.1.2 |
| NeoForge | 26.3 | NeoForge 26.3.0.69-beta+ within 26.3 |

NeoForge's 26.3 loader is currently beta. Fabric 26.4-snapshot-3 builds are experimental CI artifacts and are excluded from stable release downloads.

- North-up minimap with a block-resolution texture, automatic effective-render-distance coverage for terrain and radar, persistent client-observed terrain history, and an expanded map opened with M. Scroll over the map to zoom; N remains an optional shortcut.
- Faster visible-first terrain sampling with a 6 ms tick budget, up to 32 chunks per tick, independent parallel cache reads, and a coverage indicator. Server chunk delivery remains outside the mod's control; no additional dependency is needed.
- Smaller minimap player arrow, dimension-aware direction gradients, and two-colour N/E/S/W map edges.
- Automatic 16-block cave layers follow the connected space around the player in every dimension, hiding disconnected and stacked caves and filtering markers to the same space. Cave scans use received chunks and rebuild after nearby block updates; persistent surface history stays separate. Active layers are labelled on the map.
- Settings > Cheat searches built-in structures for the installed version and current dimension using native server commands. The button and actions stay disabled unless both operator/cheats level 2+ and locate/teleport command permissions are granted. Review the located X/Z and choose a safe arrival Y before requesting teleport; locate does not supply a safe height.
- Item, neutral/passive mob, hostile mob, and player markers with expanded-map hover identification. Entity information is limited to what the server sends to the client.
- Smaller category symbols, crowded-entity groups with counts, clickable legend filters, a rotating player arrow with eight-direction facing labels, nearby-height filtering, and responsive controls improve map readability at large GUI scales.
- Settings above the hidden list control map visibility, position, size, marker categories, and the client-only rainbow outline on the exact tracked coordinate block.
- Custom placement opens a live drag editor. Move the map with the left mouse button and choose Save & play to save and return to the game; Cancel/Escape discards the drag. Precise anchors adapt to window/GUI-scale changes, old percentage positions stay compatible, and Reset position restores the top-right preset.
- Dimension gradients, a day counter below coordinates, a diary-and-quill icon, and loader names in jar filenames and mod-list entries. Version remains 1.0.0.
- Searchable bookmarks with notes, hidden entries, colours, and a deepslate/cartography interface.
- Separate world/server profiles, safe editor cancellation, validated coordinates, transactional saves, and automatic backup recovery.
- Compile-checked HUD adapters preserve visibility and status messages on both Minecraft 26.1.2 and newer targets.

Existing bookmark and minimap files in `config/worldnotes/` are shared across loaders when using the same game directory. Keep a copy before switching game versions.

All supported targets are built before publication, with persistence, terrain-cache, and settings regression tests on the Forge classpath. In-game rendering and interaction have not been manually verified for this release.
