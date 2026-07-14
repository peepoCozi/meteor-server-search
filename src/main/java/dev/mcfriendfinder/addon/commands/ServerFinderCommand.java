package dev.mcfriendfinder.addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mcfriendfinder.addon.api.ApiClient;
import dev.mcfriendfinder.addon.api.FoundServer;
import dev.mcfriendfinder.addon.api.SearchFilters;
import dev.mcfriendfinder.addon.gui.ServerFinderScreen;
import dev.mcfriendfinder.addon.modules.ServerFinderModule;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

/**
 * A non-GUI alternative to the module's "Browse Servers" button:
 * `;minescan` (aliases `;ms`, `;sf`, `;server-finder`) opens the browser
 * screen, `;minescan list` prints a quick top-10 summary straight into chat,
 * and `;minescan list address <ip>` / `;minescan list player <username>` narrow
 * that lookup to a specific IP or username - mirroring the Discord bot's
 * `/search`.
 */
public class ServerFinderCommand extends Command {
    private final ApiClient apiClient = new ApiClient();

    public ServerFinderCommand() {
        super("minescan", "Opens the MineScan browser, or lists results in chat.", "ms", "sf", "server-finder");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            openScreen();
            return SINGLE_SUCCESS;
        });

        builder.then(literal("list")
            .executes(context -> {
                listInChat(null, null);
                return SINGLE_SUCCESS;
            })
            .then(literal("address").then(argument("address", StringArgumentType.word()).executes(context -> {
                listInChat(StringArgumentType.getString(context, "address"), null);
                return SINGLE_SUCCESS;
            })))
            .then(literal("player").then(argument("player", StringArgumentType.word()).executes(context -> {
                listInChat(null, StringArgumentType.getString(context, "player"));
                return SINGLE_SUCCESS;
            })))
        );
    }

    private void openScreen() {
        ServerFinderModule module = Modules.get().get(ServerFinderModule.class);
        mc.setScreen(new ServerFinderScreen(GuiThemes.get(), module));
    }

    private void listInChat(String address, String player) {
        ServerFinderModule module = Modules.get().get(ServerFinderModule.class);

        if (module.userApiKey.get().isBlank()) {
            error("Set a User API Key in the MineScan module's settings first (join our Discord and run /register).");
            return;
        }

        if (module.selfHostedScanner.get() && module.apiBaseUrl.get().isBlank()) {
            error("Self-Hosted Scanner is on, but API Base URL is empty.");
            return;
        }

        SearchFilters filters = new SearchFilters();
        filters.limit = 10;
        filters.address = address;
        filters.player = player;

        apiClient.listServers(
            module.getEffectiveApiBaseUrl(),
            module.userApiKey.get(),
            module.getEffectiveServerPassword(),
            filters,
            response -> {
                if (response.servers.isEmpty()) {
                    info("No servers found.");
                    return;
                }

                for (FoundServer server : response.servers) {
                    info(
                        "%s - %s/%s players - %s",
                        server.hostAndPort(),
                        server.onlinePlayers == null ? "?" : server.onlinePlayers,
                        server.maxPlayers == null ? "?" : server.maxPlayers,
                        server.versionName == null ? "?" : server.versionName
                    );
                }
            },
            err -> error("Failed to contact API: " + err.getMessage())
        );
    }
}
