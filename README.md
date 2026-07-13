# Meteor Client Server Searcher

A [Meteor Client](https://meteorclient.com/) addon that allows users to browse servers indexed by my personal server scanner database.

This repo contains only the client-side addon. It talks to a companion scanner + REST API. By default it points at my own hosted instance, so it works without any setup - but you can point **API Base URL** at a different instance instead (your own self-hosted one, or a friend's) if you'd rather not depend on mine.

This project doesn't accept external code contributions - see [CONTRIBUTING.md](CONTRIBUTING.md) if you want to run a modified version yourself.

## Download

Grab the latest built jar from the [Releases page](../../releases/latest) -
every push to `main` automatically builds and publishes it there, no manual
build required.

## Usage

1. Join the project's Discord and run `/register` there - it'll DM you a
   **User API Key**. This is required; there's no other way to get one.
2. Enable the **Server Finder** module (category **Friend Finder**) and set
   **User API Key** to the key from step 1. **API Base URL** already
   defaults to the maintainer's hosted instance, so you don't need to
   change it unless you want to point at a different (self-hosted or a
   friend's) instance - if that instance set its own **Server Password**,
   fill that in too.
3. Open the browser either via the module's "Open Server Finder" button, the
  new **Find Servers** button on the vanilla Multiplayer screen, or the
   `;server-finder` (alias `;sf`) chat command.
4. Search/filter, then **Add** a result to your normal Multiplayer server
  list, or **Connect** to join it immediately.

