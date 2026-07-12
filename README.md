# Meteor Client Server Searcher

A [Meteor Client](https://meteorclient.com/) addon that allows users to browse servers indexed by my personal server scanner database which is still currently in private developement.

This repo contains only the client-side addon. It talks to a companion scanner + REST API and it doesn't ship or require any specific hosted backend. You'll need the base URL (and optionally an API key) of an instance someone is running for you, or to self-host that backend yourself.

## Usage

1. Enable the **Server Finder** module (category **Friend Finder**) and set
  **API Base URL** to a running MC Friend Finder API instance, e.g.
   `http://your-server:8080`. If that instance requires an API key, also
   fill in **API Key**.
2. Open the browser either via the module's "Open Server Finder" button, the
  new **Find Servers** button on the vanilla Multiplayer screen, or the
   `;server-finder` (alias `;sf`) chat command.
3. Search/filter, then **Add** a result to your normal Multiplayer server
  list, or **Connect** to join it immediately.

