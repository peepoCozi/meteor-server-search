package dev.mcfriendfinder.addon.gui;

import dev.mcfriendfinder.addon.ServerFinderAddon;
import dev.mcfriendfinder.addon.api.ApiClient;
import dev.mcfriendfinder.addon.api.CrackedFilter;
import dev.mcfriendfinder.addon.api.FoundServer;
import dev.mcfriendfinder.addon.api.SearchFilters;
import dev.mcfriendfinder.addon.api.ServerListResponse;
import dev.mcfriendfinder.addon.api.ServerTypeFilter;
import dev.mcfriendfinder.addon.modules.ServerFinderModule;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * The main "browser" UI: search/filter controls, a paginated results table,
 * and per-row actions to add a server to the vanilla Multiplayer list or
 * connect to it immediately.
 */
public class ServerFinderScreen extends WindowScreen {
    private static final int MOTD_PREVIEW_LENGTH = 40;

    private final ServerFinderModule module;
    private final ApiClient apiClient = new ApiClient();

    private WLabel statusLabel;
    private WTable table;
    private WTextBox motdBox;
    private WTextBox versionBox;
    private WIntEdit minPlayersEdit;
    private WIntEdit maxPlayersEdit;
    private WDropdown<CrackedFilter> crackedDropdown;
    private WDropdown<ServerTypeFilter> serverTypeDropdown;
    private WCheckbox hideJoinedBox;

    private long offset = 0;
    private boolean loading = false;

    public ServerFinderScreen(GuiTheme theme, ServerFinderModule module) {
        super(theme, "Server Finder");
        this.module = module;
    }

    @Override
    public void initWidgets() {
        if (module.apiBaseUrl.get().isBlank()) {
            add(theme.label("Set an API Base URL in the Server Finder module's settings first."))
                .expandX();
            add(theme.label("(See the project README for how to run your own scanner + API.)"))
                .expandX();
            return;
        }

        if (module.userApiKey.get().isBlank()) {
            add(theme.label("Set a User API Key in the Server Finder module's settings first."))
                .expandX();
            add(theme.label("(Join our Discord and run /register to get one.)"))
                .expandX();
            return;
        }

        WHorizontalList controls = add(theme.horizontalList()).expandX().widget();

        motdBox = controls.add(theme.textBox(module.motdContains.get(), "Search MOTD...")).expandX().minWidth(200d).widget();
        motdBox.action = this::resetAndSearch;

        crackedDropdown = controls.add(theme.dropdown(module.crackedFilter.get())).widget();
        crackedDropdown.action = this::resetAndSearch;

        serverTypeDropdown = controls.add(theme.dropdown(module.serverTypeFilter.get())).widget();
        serverTypeDropdown.action = this::resetAndSearch;

        WButton search = controls.add(theme.button("Search")).widget();
        search.action = this::resetAndSearch;

        // Second row: the remaining filters that were previously only
        // reachable from the module's own settings screen, so this screen
        // (whether opened from the module widget or the multiplayer menu's
        // "Find Servers" button) has the same filter access either way.
        WHorizontalList moreControls = add(theme.horizontalList()).expandX().widget();

        moreControls.add(theme.label("Version:"));
        versionBox = moreControls.add(theme.textBox(module.versionFilter.get(), "e.g. 1.21")).minWidth(100d).widget();
        versionBox.action = this::resetAndSearch;

        moreControls.add(theme.label("Min players:"));
        minPlayersEdit = moreControls.add(theme.intEdit(module.minPlayers.get(), 0, Integer.MAX_VALUE, true)).widget();
        minPlayersEdit.actionOnRelease = this::resetAndSearch;

        moreControls.add(theme.label("Max players:"));
        maxPlayersEdit = moreControls.add(theme.intEdit(module.maxPlayers.get(), 0, Integer.MAX_VALUE, true)).widget();
        maxPlayersEdit.actionOnRelease = this::resetAndSearch;

        hideJoinedBox = moreControls.add(theme.checkbox(module.hideJoinedServers.get())).widget();
        hideJoinedBox.action = this::resetAndSearch;
        moreControls.add(theme.label("Hide joined"));

        WButton clearHistory = moreControls.add(theme.button("Clear Joined History")).widget();
        clearHistory.tooltip = "Forget every server you've connected to";
        clearHistory.action = () -> {
            module.joinedServers.set(new ArrayList<>());
            resetAndSearch();
        };

        statusLabel = add(theme.label("")).expandX().widget();

        table = add(theme.table()).expandX().minWidth(520d).widget();

        WHorizontalList pagination = add(theme.horizontalList()).expandX().widget();

        WButton previous = pagination.add(theme.button("Previous Page")).widget();
        previous.action = () -> {
            if (loading) return;
            offset = Math.max(0, offset - module.pageSize.get());
            search();
        };

        WButton next = pagination.add(theme.button("Next Page")).widget();
        next.action = () -> {
            if (loading) return;
            offset += module.pageSize.get();
            search();
        };

        search();
    }

    private void resetAndSearch() {
        offset = 0;
        search();
    }

    private void search() {
        if (loading) return;
        loading = true;
        statusLabel.set("Loading...");

        // Keep the module's settings in sync with whatever's live in the
        // screen, so reopening it (or the module's own settings screen)
        // reflects the last search rather than stale defaults.
        module.versionFilter.set(versionBox.get());
        module.minPlayers.set(minPlayersEdit.get());
        module.maxPlayers.set(maxPlayersEdit.get());
        module.motdContains.set(motdBox.get());
        module.crackedFilter.set(crackedDropdown.get());
        module.serverTypeFilter.set(serverTypeDropdown.get());
        module.hideJoinedServers.set(hideJoinedBox.checked);

        SearchFilters filters = new SearchFilters();
        filters.versionName = blankToNull(versionBox.get());
        filters.minPlayers = minPlayersEdit.get() > 0 ? minPlayersEdit.get() : null;
        filters.maxPlayers = maxPlayersEdit.get() > 0 ? maxPlayersEdit.get() : null;
        filters.cracked = crackedDropdown.get().toApiValue();
        filters.motdContains = blankToNull(motdBox.get());
        filters.serverType = serverTypeDropdown.get().toApiValue();
        filters.limit = module.pageSize.get();
        filters.offset = offset;

        apiClient.listServers(
            module.apiBaseUrl.get(),
            module.userApiKey.get(),
            module.serverPassword.get(),
            filters,
            this::onResults,
            this::onError
        );
    }

    private void onResults(ServerListResponse response) {
        loading = false;
        offset = response.offset;
        table.clear();

        List<FoundServer> servers = response.servers;
        if (hideJoinedBox.checked) {
            List<String> joined = module.joinedServers.get();
            servers = servers.stream().filter(s -> !joined.contains(s.hostAndPort())).toList();
        }

        if (servers.isEmpty()) {
            statusLabel.set("No servers found for this search.");
        } else {
            statusLabel.set(servers.size() + " servers (offset " + response.offset + ")");
        }

        for (FoundServer server : servers) {
            addRow(server);
        }
    }

    private void onError(Throwable error) {
        loading = false;
        statusLabel.set("Error contacting API: " + error.getMessage());
    }

    private void addRow(FoundServer server) {
        table.add(theme.label(server.hostAndPort())).widget();
        table.add(theme.label(server.versionName == null ? "?" : server.versionName)).widget();
        table.add(theme.label(formatSoftware(server.software))).widget();
        table.add(theme.label(formatPlayers(server))).widget();
        table.add(theme.label(server.cracked ? "Cracked" : "Online")).widget();
        table.add(theme.label(formatMotd(server.motd))).expandCellX().widget();

        WButton add = table.add(theme.button("Add")).widget();
        add.tooltip = "Add to your vanilla Multiplayer server list";
        add.action = () -> addToServerList(server);

        WButton connect = table.add(theme.button("Connect")).widget();
        connect.tooltip = "Connect immediately without adding it to your list";
        connect.action = () -> connectTo(server);

        table.row();
    }

    private void addToServerList(FoundServer server) {
        String hostAndPort = server.hostAndPort();

        // NOTE: ServerList's constructor/add/save signatures are long-standing, stable
        // Minecraft client APIs but aren't directly exercised anywhere in meteor-client's
        // own source, so they're inferred rather than hand-verified here. Double check
        // against the exact pinned Minecraft version if this fails to compile.
        ServerList serverList = new ServerList(mc);
        serverList.load();
        serverList.add(new ServerData(hostAndPort, hostAndPort, ServerData.Type.OTHER), false);
        serverList.save();

        statusLabel.set("Added " + hostAndPort + " to your server list.");
    }

    private void connectTo(FoundServer server) {
        String hostAndPort = server.hostAndPort();

        try {
            // Verified against the actual net.minecraft.client.gui.screens.ConnectScreen
            // class for this Minecraft version (26.1.2 / Yarn 1.21.11+build.3) via javap:
            // ConnectScreen.connect(...) is a private instance method, not a usable public
            // API. The real public entry point - and what vanilla's JoinMultiplayerScreen
            // itself calls when connecting to a server list entry - is the static
            // startConnecting(Screen, Minecraft, ServerAddress, ServerData, boolean,
            // @Nullable TransferState) method, passing false/null for the last two params
            // for a normal (non-transfer) connection.
            ServerAddress address = ServerAddress.parseString(hostAndPort);
            ServerData serverData = new ServerData(hostAndPort, hostAndPort, ServerData.Type.OTHER);

            // No need to record "joined" bookkeeping here - the module
            // listens for ServerConnectEndEvent itself, which fires for
            // every successful connection regardless of how it was started.
            onClose();
            ConnectScreen.startConnecting(this, mc, address, serverData, false, null);
        } catch (Exception e) {
            ServerFinderAddon.LOG.error("Failed to connect to {}", hostAndPort, e);
            statusLabel.set("Failed to connect: " + e.getMessage());
        }
    }

    private String formatSoftware(String software) {
        if (software == null || software.isBlank() || software.equals("other")) return "?";
        return switch (software) {
            case "neoforge" -> "NeoForge";
            case "bungeecord" -> "BungeeCord";
            default -> software.substring(0, 1).toUpperCase() + software.substring(1);
        };
    }

    private String formatPlayers(FoundServer server) {
        String online = server.onlinePlayers == null ? "?" : server.onlinePlayers.toString();
        String max = server.maxPlayers == null ? "?" : server.maxPlayers.toString();
        return online + "/" + max;
    }

    private String formatMotd(String motd) {
        if (motd == null || motd.isBlank()) return "";

        String stripped = motd.replaceAll("\u00a7.", "").replace('\n', ' ').trim();
        if (stripped.length() > MOTD_PREVIEW_LENGTH) {
            stripped = stripped.substring(0, MOTD_PREVIEW_LENGTH) + "...";
        }

        return stripped;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
