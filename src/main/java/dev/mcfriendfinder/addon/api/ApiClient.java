package dev.mcfriendfinder.addon.api;

import com.google.gson.Gson;
import dev.mcfriendfinder.addon.ServerFinderAddon;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * A tiny HTTP client for talking to a self-hosted MC Friend Finder API
 * instance (see {@code scanner/api} in the repo root). Every call runs on
 * {@link MeteorExecutor}'s background thread pool - never on the render
 * thread - and hands results back via {@code mc.execute(...)} so callbacks
 * are always safe to touch game/GUI state from.
 */
public class ApiClient {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public void listServers(
        String baseUrl,
        String apiKey,
        SearchFilters filters,
        Consumer<ServerListResponse> onSuccess,
        Consumer<Throwable> onError
    ) {
        MeteorExecutor.execute(() -> {
            try {
                URI uri = buildServersUri(baseUrl, filters);
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .GET();

                if (apiKey != null && !apiKey.isBlank()) {
                    requestBuilder.header("X-API-Key", apiKey);
                }

                HttpResponse<String> response = HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("API returned HTTP " + response.statusCode() + ": " + response.body());
                }

                ServerListResponse parsed = GSON.fromJson(response.body(), ServerListResponse.class);
                if (parsed.servers == null) parsed.servers = List.of();

                mc.execute(() -> onSuccess.accept(parsed));
            } catch (Exception e) {
                ServerFinderAddon.LOG.warn("Failed to fetch server list from {}", baseUrl, e);
                mc.execute(() -> onError.accept(e));
            }
        });
    }

    private URI buildServersUri(String baseUrl, SearchFilters filters) {
        String base = normalizeBaseUrl(baseUrl);
        StringBuilder query = new StringBuilder();

        appendParam(query, "limit", Long.toString(filters.limit));
        appendParam(query, "offset", Long.toString(filters.offset));
        if (filters.versionName != null && !filters.versionName.isBlank()) appendParam(query, "version_name", filters.versionName);
        if (filters.minPlayers != null) appendParam(query, "min_players", Integer.toString(filters.minPlayers));
        if (filters.maxPlayers != null) appendParam(query, "max_players", Integer.toString(filters.maxPlayers));
        if (filters.cracked != null) appendParam(query, "cracked", filters.cracked.toString());
        if (filters.motdContains != null && !filters.motdContains.isBlank()) appendParam(query, "motd_contains", filters.motdContains);
        if (filters.serverType != null && !filters.serverType.isBlank()) appendParam(query, "server_type", filters.serverType);

        return URI.create(base + "/api/v1/servers?" + query);
    }

    /**
     * {@link HttpRequest} requires an absolute URI with an explicit scheme
     * and throws a cryptic {@code IllegalArgumentException: URI with
     * undefined scheme} otherwise. Users very naturally type just
     * {@code host:port} into the API Base URL setting (omitting {@code
     * http://}), so default to that instead of surfacing that exception.
     */
    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.strip();
        if (!trimmed.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            trimmed = "http://" + trimmed;
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private void appendParam(StringBuilder query, String key, String value) {
        if (!query.isEmpty()) query.append('&');
        query.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
