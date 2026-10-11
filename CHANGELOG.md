# World Notes & Coordinates - Changelog

All notable changes to the mod are documented here.

## 1.0.0 stable

- Fixed movement restarting cave scans: verified short walking paths and shifted cached masks, floor pixels, queued work, and pending chunk frontiers instead of starting over.
- Added a normal Nether terrain view with floor/lava sampling below the bedrock roof and local enclosure detection for actual Nether tunnels.
- Retained normal terrain while cave views are active and used standing floors to keep short jumps from changing cave bands.
- Added movement, dense/diagonal mask shifts, unfinished queues, delayed frontiers, Nether enclosure, roof exclusion, lava, and jump-boundary regression coverage.

- Fixed repeated Nether cave-map resets by retaining unprocessed seeds/frontiers and rebuilding only for actual passage changes.
- Updated cave floor colours in place for visual block changes, preserving the current scan and discovered terrain.
- Added expired-budget, missing-seed, blocked-seed recovery, pending-player-position, repeated nonblocking-update, and floor-colour regression coverage.

- Replaced custom-position percentage buttons with a live drag editor, precise saved anchors, Save & play, cancellation, and a reset-position action.
- Shared placement geometry between the HUD and preview, preserved legacy positions, and kept maps inside window bounds after resizing.
- Added placement precision, persistence, resize, edge-clamping, legacy migration, and draft-isolation regression coverage.

- Added automatic connected cave layers in every dimension, with 16-block height bands, isolated-cave masking, matching entity/bookmark filtering, and active-layer labels.
- Kept persistent surface history out of cave views; resumed scans at missing chunk frontiers and rebuilt connectivity when passages change.
- Disabled Cheat unless both operator-level permission and locate/teleport command access are granted, with live permission updates.
- Added sealed/stacked cave, negative-layer, delayed-chunk, passage-change, and combined-permission regression tests.

- Replaced zoom buttons with mouse-wheel zoom over the expanded map, including fractional scrolling and bounded zoom levels.
- Prioritized visible terrain, increased bounded sampling to up to 32 chunks per tick, separated parallel cache reads from writes, and displayed terrain coverage.
- Reduced the minimap player arrow and added dimension-aware facing gradients and two-colour cardinal edge letters.
- Added Settings > Cheat with searchable, version-available, dimension-filtered structure locations and permission-aware teleport requests with an explicit arrival height.
- Added structure-response validation, catalog availability/dimension, zoom boundary, and terrain scan-order regression coverage.

- Replaced coarse terrain samples with 256 independent block colours per chunk and an efficient nearest-neighbour map texture; introduced a separately versioned block-resolution cache.
- Matched terrain and entity range to Minecraft's effective render distance, replacing the fixed radius with 1x–16x visual zoom.
- Added a rotating player arrow and eight-direction facing labels, with snapshot-specific texture API support.

- Improved radar readability with smaller category symbols, spatial grouping and counts, full hover details, nearby-height filtering, and a clearly labelled player ring.
- Added clickable category filters and responsive expanded-map controls/sidebar, with a gentler terrain palette and clearer scale labels.

- Expanded the minimap to a default 32-chunk radius, with persistent terrain history separated by world, dimension, and cave elevation.
- Added M to open/close an expanded map, hover identification, and markers for items, neutral/passive mobs, hostile mobs, and other players received by the client.
- Added settings above the hidden list for minimap visibility, marker categories, size, radius, corner/custom placement, and client-only rainbow tracking outlines.
- Added dimension text gradients, moved the day counter below coordinates, and replaced the icon with a diary and feather quill.
- Distinguished all loader jars and mod-list entries while retaining version 1.0.0.
- Added terrain corruption, negative-coordinate, context separation, restart restoration, and settings regression coverage.

- Added Minecraft 26.1.2 targets for Fabric, Forge, and NeoForge.
- Added native client-only NeoForge entrypoints, key mappings, HUD layers, and client tick integration for 26.1.2 and 26.3.
- Added compile-checked version-specific HUD adapters for Minecraft 26.1.2 and newer versions.
- Expanded local build scripts and CI to all eight supported loader/version targets.
- Added release-tag publication of stable client jars and SHA-256 checksums after successful CI; snapshot jars remain experimental artifacts.
- Retained existing local bookmark and minimap data formats across all loaders.

## 0.2.2-beta

- Added bookmark search, responsive pagination, bounded previews, and empty/search result messages.
- Added a deepslate and cartography background with consistent dimension-coloured panels and minimap framing.
- Fixed draft edits leaking into saved entries, Escape skipping cancellation, and custom dimensions being reset by the editor.
- Preserved text and coordinate drafts across window resizing; made validation and save failures visible.
- Added transactional bookmark writes, previous-version backups, corruption recovery, and protection against overwriting unreadable data.
- Fixed cross-world ID collisions, stale tracked entries after edits/deletion, unsafe dimension strings, numeric overflow, and surrogate splitting in gradients.
- Replaced premature teleport-success messages with request messages and checked cross-dimension command availability.
- Cached minimap marker lists and added gold edge markers for tracked destinations outside map coverage.
- Added persistence regression tests on the Forge development classpath.
- Added a batch build launcher for Windows installations where PowerShell script execution is disabled.

## 0.2.1-beta

- Added a client-side Forge build for Minecraft 26.3 / Forge 66.0.10.
- Shared minimap, bookmark screens, tracking, and JSON saves between Fabric and Forge.
- Isolated loader-specific entrypoints, key registration, client tick callbacks, and HUD registration.
- Added Forge build instructions, a Forge CI job, and Forge to the all-target build script.

## 0.2.0-beta

- Added a client-side north-up terrain minimap with player direction and dimension-aware bookmark markers.
- Added persistent minimap visibility and three zoom levels, controlled by configurable M/N keybindings.
- Bounded terrain sampling per tick, reused samples when moving, and avoided requesting unloaded chunks.
- Moved tracked-bookmark guidance below the minimap and respected hidden HUD mode.
- Added separate exact-version builds for Minecraft 26.3, 26.4-snapshot-3, and 26.2.
- Updated Fabric Loader, Fabric API, Loom, and Gradle; added a three-version CI build matrix.

## 0.1.4-stable

This is the main branch and the stable release line for `0.1.4-stable`.

### Bug fixes

- Fixed bookmark pagination showing an empty page after deleting the final bookmark on a later page.
- Ignored non-finite coordinate input instead of saving invalid teleport coordinates.
- Hardened bookmark sorting and updates against incomplete records in the saved JSON file.
- Removed a Gradle configuration warning caused by accessing project properties during task execution.
- Fixed the keybinding category displaying its translation key instead of its name.
- Removed the Delete button while creating a new bookmark and moved Cancel into its place.

## 0.1.4-beta

### Hidden bookmarks

- Added a checkbox in the bookmark editor to hide a bookmark from the main list.
- Added a Barrier button to open hidden bookmarks.
- Added dimension filtering to the hidden-bookmarks view.
- Added editing support for hidden bookmarks.
- Added a Night Vision button to unhide bookmarks.
- Added confirmation text: "Are you sure you want to unhide this Bookmark?"
- Disabled the hide option while editing a bookmark from the hidden-bookmarks view.
- Replaced the unhide potion bottle with the Night Vision effect icon.
- Removed HUD background panels for cleaner text-only navigation, coordinate, dimension, tracking, and day displays.
- Declared Fabric compatibility with Minecraft Client 26.1 through 26.2.
- Fixed the bookmark menu crashing on Minecraft 26.1 when opening it with the B key.
- Fixed cancelling a new bookmark after tracking it from saving the bookmark unintentionally, including repeated tracking attempts.

## 0.1.3-beta

### Bookmark interface

- Reworked bookmark entries into readable cards containing:
  - Bookmark name
  - Optional player note
  - Coordinates in `x, y, z` format
  - Dimension
- Added read-only bookmark details when selecting a bookmark.
- Added a separate Book icon button for editing bookmarks.
- Added a Nether Star icon beside every bookmark name.
- Added dimension-themed card backgrounds for the Overworld, Nether, and End.
- Added native Book and Quill icons to the add-bookmark buttons.
- Added a compact Ender Pearl teleport button with a tooltip.
- Newly created and saved bookmark coordinates are rounded to two decimal places for X, Y, and Z.
- Added a centered mod title to the bookmark manager.
- Added purpose tooltips to bookmark manager and editor controls.
- Added an explicit delete confirmation that warns deletion cannot be undone.

### Tracking

- Added Track and Stop tracking controls to bookmark details and editing screens.
- Only one bookmark can be tracked at a time within a world.
- Disabled Track buttons explain when another bookmark is already active.
- Tracking displays direction, remaining distance, target dimension, and the tracked bookmark name.
- The tracked bookmark name uses its selected gradient in the tracking HUD.
- Tracking is isolated between worlds.

### Colors and gradients

- Added editable name colors and gradients: black, red, yellow, blue, orange, purple, green, and pink.
- Applied dimension gradients throughout the interface:
  - Overworld: green
  - Nether: red
  - End: purple
  - All Dimensions filter: cyan
- Normalized stored gradient values so capitalization and whitespace do not prevent rendering.

### Worlds, permissions, and settings

- Separated singleplayer bookmarks by each world’s actual save path.
- Added migration for bookmarks created with the previous world-name profile format.
- Teleport buttons are disabled when the player lacks the required command permission.
- Added a permission tooltip for disabled teleport buttons.
- Added custom teleport confirmation messages that preserve the bookmark name gradient.
- Registered the B keybind under the World Notes category so it can be changed in Minecraft’s Controls settings.

### Assets and versioning

- Added the supplied PNG as the mod icon.
- Updated the Modrinth/Fabric metadata version to `0.1.3-beta`.

## 0.1.2-beta

- Added client-side bookmark tracking.
- Added direction and block-distance guidance for tracked coordinates.
- Added tracked bookmark names and dimensions to the HUD.
- Replaced world-space markers with the tracking interface.

## 0.1.1-beta

- Added the live coordinate HUD.
- Added a cardinal-direction HUD display.
- Added day counter display.

## 0.1.0

- Initial client-side bookmark manager.
- Added current-position and manual bookmark creation.
- Added notes, dimensions, editing, deletion, filtering, and teleport support.
