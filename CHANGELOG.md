# World Notes & Coordinates - Changelog

All notable changes to the mod are documented here.

## 0.1.5-dev

This is a development branch. The `0.1.5-dev` version is intended for development and testing purposes and is not a stable release.

### Favorite bookmarks

- Added a separate Favorites view above Hidden Bookmarks.
- Added favorite bookmark support with the same filtering, details, editing, and teleport actions as the main and hidden-bookmark views.

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
