# MC Friend Finder addon

A [Meteor Client](https://meteorclient.com/) addon, scaffolded from
[meteor-addon-template](https://github.com/MeteorDevelopment/meteor-addon-template),
that adds a **Server Finder** module for browsing servers indexed by a
self-hosted [`scanner`/`api`](../scanner) instance and joining them from the
vanilla Multiplayer menu.

## Layout

```text
src/main/java/dev/mcfriendfinder/addon/
├── ServerFinderAddon.java     - entrypoint, registers the module/command/category
├── modules/ServerFinderModule.java   - settings: apiBaseUrl, apiKey, default filters
├── gui/ServerFinderScreen.java       - the browser window (search + results table)
├── api/                               - HTTP client + JSON DTOs for the REST API
├── mixin/MultiplayerScreenMixin.java  - adds a "Find Servers" button to vanilla's
│                                        Multiplayer screen
└── commands/ServerFinderCommand.java - `;server-finder` / `;sf` chat command
```

## Building

Requires a JDK matching `gradle/libs.versions.toml`'s `jdk` version and
network access to Fabric's and Meteor's Maven repos (the build script pulls
the Minecraft/Yarn/Fabric Loader/Meteor Client artifacts it needs
automatically via Fabric Loom).

```bash
./gradlew build
```

The output jar will be in `build/libs/`. Drop it into your `mods` folder
alongside Meteor Client itself.

For iterative development, run the `Minecraft Client` Gradle/IDE run
configuration that Loom generates - this launches Minecraft with both Meteor
Client and this addon loaded.

## Using it

1. Enable the **Server Finder** module (category **Friend Finder**) and set
   **API Base URL** to a running `mc-scanner-api` instance, e.g.
   `http://your-server:8080` (see [`../scanner/README.md`](../scanner/README.md)).
   If that instance was started with `[api].api_key` set, also fill in
   **API Key**.
2. Open the browser either via the module's "Open Server Finder" button, the
   new **Find Servers** button on the vanilla Multiplayer screen, or the
   `;server-finder` (alias `;sf`) chat command.
3. Search/filter, then **Add** a result to your normal Multiplayer server
   list, or **Connect** to join it immediately.

## A note on API accuracy

This addon's Minecraft-side code (`ServerData`, `ServerList`,
`ConnectScreen.connect(...)`, `TransferState.NONE`) was written by
cross-referencing the actual `meteor-client` source for the pinned
Minecraft/Meteor version wherever possible (see `mixin/MultiplayerScreenMixin.java`
and `gui/ServerFinderScreen.java` for the exact classes/methods this was
checked against). A couple of calls - noted with `NOTE:` comments in
`ServerFinderScreen` - rely on long-standing, well-known Minecraft client
APIs that weren't directly exercised anywhere in `meteor-client`'s own code,
so they're inferred rather than hand-verified. If the build fails on one of
those lines, check the actual method/class in your IDE (with dependencies
resolved) and adjust - the surrounding logic doesn't need to change, just
the exact API call.
