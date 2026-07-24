# World Notes & Coordinates

A client-side Fabric mod for Minecraft Java 26.2. Press **B** in game to open the bookmark manager. Version 0.1.1 adds a live coordinate and cardinal-direction HUD.

## Features

- Bookmark coordinates with a name, note, and dimension.
- Create a bookmark at the current location, or enter coordinates manually.
- Filter the list by Overworld, Nether, End, or all dimensions.
- Edit and delete bookmarks.
- Copy a location to chat.
- Send an appropriate vanilla teleport command for a bookmark. The server remains authoritative: teleport only works when it grants the player permission for the command.

## Build

Install a **JDK 25+** and Gradle, then run `gradle build`. The finished mod JAR is placed in `build/libs`.

Bookmark data is kept locally at `.minecraft/config/worldnotes/bookmarks.json` and is separated by server/world.
