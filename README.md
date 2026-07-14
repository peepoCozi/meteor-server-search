# MineScan (Meteor addon)

A [Meteor Client](https://meteorclient.com/) addon for browsing servers indexed
by MineScan.

This repo contains only the client-side addon. It talks to the official
MineScan scanner + REST API at `https://api.minescan.net`. There's no
self-hosting option - every request goes to the official API.

This project doesn't accept external code contributions.

## Download

Grab the latest built jar from the [Releases page](../../releases/latest) -
every push to `main` automatically builds and publishes it there, no manual
build required.

## Usage

1. Join the project's Discord and run `/register` there - it'll DM you a
   **User Access Code**. This is required to use the addon; there's no
   other way to get one. (You don't need this to use the bot's own
   `/search`, `/server`, or `/stats` commands in Discord.)
2. Enable the **MineScan** module and set **User Access Code** to the code
   from step 1. Requests always go to `https://api.minescan.net`.
3. Open the browser either via the module's **Browse Servers** button, the
   **MineScan** button on the vanilla Multiplayer screen, or the `;minescan`
   chat command (alias `;ms`).
4. Search/filter, then **Add** a result to your normal Multiplayer server
  list, or **Connect** to join it immediately.
