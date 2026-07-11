package dev.mcfriendfinder.addon.commands;

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
 * A non-GUI alternative to the module's "Open Server Finder" button:
 * `;server-finder` (or `;sf`) opens the browser screen, and
 * `;server-finder list` prints a quick top-10 summary straight into chat.
 */
public class ServerFinderCommand extends Command {
    private final ApiClient apiClient = new ApiClient();

    public ServerFinderCommand() {
        super("server-finder", "Opens the Server Finder browser, or lists results in chat.", "sf");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            openScreen();
            return SINGLE_SUCCESS;
        });

        builder.then(literal("list").executes(context -> {
            listInChat();
            return SINGLE_SUCCESS;
        }));
    }

    private void openScreen() {
        ServerFinderModule module = Modules.get().get(ServerFinderModule.class);
        mc.setScreen(new ServerFinderScreen(GuiThemes.get(), module));
    }

    private void listInChat() {
        ServerFinderModule module = Modules.get().get(ServerFinderModule.class);

        if (module.apiBaseUrl.get().isBlank()) {
            error("Set an API Base URL in the Server Finder module's settings first.");
            return;
        }

        SearchFilters filters = new SearchFilters();
        filters.limit = 10;

        apiClient.listServers(
            module.apiBaseUrl.get(),
            module.apiKey.get(),
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
