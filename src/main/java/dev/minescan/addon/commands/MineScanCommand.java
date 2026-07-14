package dev.minescan.addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.minescan.addon.api.ApiClient;
import dev.minescan.addon.api.FoundServer;
import dev.minescan.addon.api.SearchFilters;
import dev.minescan.addon.gui.MineScanScreen;
import dev.minescan.addon.modules.MineScanModule;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

/**
 * A non-GUI alternative to the module's "Browse Servers" button:
 * `;minescan` (alias `;ms`) opens the browser screen, `;minescan list`
 * prints a quick top-10 summary straight into chat, and `;minescan list
 * address <ip>` / `;minescan list player <username>` narrow that lookup to
 * a specific IP or username - mirroring the Discord bot's `/search`.
 */
public class MineScanCommand extends Command {
    private final ApiClient apiClient = new ApiClient();

    public MineScanCommand() {
        super("minescan", "Opens the MineScan browser, or lists results in chat.", "ms");
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
        MineScanModule module = Modules.get().get(MineScanModule.class);
        mc.setScreen(new MineScanScreen(GuiThemes.get(), module));
    }

    private void listInChat(String address, String player) {
        MineScanModule module = Modules.get().get(MineScanModule.class);

        if (module.userApiKey.get().isBlank()) {
            error("Set a User Access Code in the MineScan module's settings first (join our Discord and run /register).");
            return;
        }

        SearchFilters filters = new SearchFilters();
        filters.limit = 10;
        filters.address = address;
        filters.player = player;

        apiClient.listServers(
            MineScanModule.DEFAULT_API_BASE_URL,
            module.userApiKey.get(),
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
