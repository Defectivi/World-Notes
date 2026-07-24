# World Notes & Coordinates

A client-side Fabric mod for Minecraft Java 26.2. The latest release is **0.1.3**.

Press **B** to open the bookmark manager. The keybind can be changed in **Settings -> Controls** under the **World Notes & Coordinates** category.

## Version history

### 0.1.3 - Interface, gradients, and world profiles

#### Bookmark interface

- Bookmark cards display the name, optional player note, coordinates, and dimension on separate lines.
- Selecting a bookmark opens read-only details.
- A separate Book icon opens the editable bookmark screen.
- A Nether Star appears beside each bookmark name.
- Cards expand when names or notes need additional lines.
- Cards use dimension-themed backgrounds:
  - Overworld: dirt-brown
  - Nether: netherrack-red
  - End: endstone-gray
- Add-current-position and add-manual-bookmark buttons use native Book and Quill icons.
- Teleport uses a compact Ender Pearl button with a tooltip.

#### Colors and gradients

- Bookmark names support editable red, yellow, blue, orange, and purple gradients.
- Dimension labels use gradients throughout the HUD, tracking panel, filters, and bookmark cards:
  - Overworld: green
  - Nether: red
  - End: purple
  - All Dimensions: cyan
- Stored gradient values are normalized so capitalization and whitespace do not prevent rendering.

#### World profiles and permissions

- Singleplayer bookmarks are separated by each world’s actual save path.
- Existing bookmarks from the previous world-name profile format are migrated automatically.
- Teleport buttons are disabled when the player lacks the required permission.
- Disabled teleport buttons explain the required permission when hovered.
- Successful teleports display a custom confirmation containing the colored bookmark name, coordinates, and dimension.

#### Settings and assets

- The B keybind is registered under the World Notes category and can be changed in Minecraft’s Controls settings.
- The supplied PNG is configured as the mod icon.
- Version `0.1.3` is declared through Gradle and Fabric metadata for correct Modrinth identification.

### 0.1.2 - Bookmark tracking

- Added Track and Stop tracking controls to bookmark details and editing screens.
- Only one bookmark can be tracked at a time within a world.
- Disabled Track buttons explain when another bookmark is already active.
- Replaced world-space coordinate markers with a tracking HUD.
- Tracking displays:
  - Tracked bookmark name
  - Direction, including diagonal directions
  - Remaining distance in blocks
  - Destination dimension
- Tracking is isolated between worlds.

## Building

Install **JDK 25+**, then run:

```powershell
.\gradlew.bat build
```

The finished mod JAR is placed in `build/libs`.

Bookmark data is stored locally at:

```text
.minecraft/config/worldnotes/bookmarks.json
```

See [CHANGELOG.md](CHANGELOG.md) for the complete project history.

