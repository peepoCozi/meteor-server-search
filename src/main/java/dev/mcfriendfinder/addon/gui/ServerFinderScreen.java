package dev.mcfriendfinder.addon.gui;

import dev.mcfriendfinder.addon.ServerFinderAddon;
import dev.mcfriendfinder.addon.api.ApiClient;
import dev.mcfriendfinder.addon.api.FoundServer;
import dev.mcfriendfinder.addon.api.SearchFilters;
import dev.mcfriendfinder.addon.api.ServerListResponse;
import dev.mcfriendfinder.addon.modules.ServerFinderModule;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

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
    private WCheckbox crackedBox;

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

        WHorizontalList controls = add(theme.horizontalList()).expandX().widget();

        motdBox = controls.add(theme.textBox(module.motdContains.get(), "Search MOTD...")).expandX().minWidth(200d).widget();
        motdBox.action = this::resetAndSearch;

        crackedBox = controls.add(theme.checkbox(module.crackedOnly.get())).widget();
        controls.add(theme.label("Cracked only"));

        WButton search = controls.add(theme.button("Search")).widget();
        search.action = this::resetAndSearch;

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

        SearchFilters filters = new SearchFilters();
        filters.versionName = blankToNull(module.versionFilter.get());
        filters.minPlayers = module.minPlayers.get() > 0 ? module.minPlayers.get() : null;
        filters.maxPlayers = module.maxPlayers.get() > 0 ? module.maxPlayers.get() : null;
        filters.cracked = crackedBox.checked ? Boolean.TRUE : null;
        filters.motdContains = blankToNull(motdBox.get());
        filters.limit = module.pageSize.get();
        filters.offset = offset;

        apiClient.listServers(
            module.apiBaseUrl.get(),
            module.apiKey.get(),
            filters,
            this::onResults,
            this::onError
        );
    }

    private void onResults(ServerListResponse response) {
        loading = false;
        offset = response.offset;
        table.clear();

        if (response.servers.isEmpty()) {
            statusLabel.set("No servers found for this search.");
        } else {
            statusLabel.set(response.servers.size() + " servers (offset " + response.offset + ")");
        }

        for (FoundServer server : response.servers) {
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
            // NOTE: ConnectScreen.connect(Minecraft, ServerAddress, ServerData, TransferState)
            // and TransferState.NONE are inferred from meteor-client's own ConnectScreenMixin
            // (see mixin/ConnectScreenMixin.java in the meteor-client source) rather than
            // hand-verified against compiled game code. Double check this against the exact
            // pinned Minecraft/Meteor version if it fails to compile.
            ServerAddress address = ServerAddress.parseString(hostAndPort);
            ServerData serverData = new ServerData(hostAndPort, hostAndPort, ServerData.Type.OTHER);

            onClose();
            ConnectScreen.connect(mc, address, serverData, TransferState.NONE);
        } catch (Exception e) {
            ServerFinderAddon.LOG.error("Failed to connect to {}", hostAndPort, e);
            statusLabel.set("Failed to connect: " + e.getMessage());
        }
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
