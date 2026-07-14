package dev.minescan.addon.api;

import com.google.gson.Gson;
import dev.minescan.addon.MineScanAddon;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * A tiny HTTP client for talking to the official MineScan API
 * (see {@code scanner/api} in the repo root). Every call runs on
 * {@link MeteorExecutor}'s background thread pool - never on the render
 * thread - and hands results back via {@code mc.execute(...)} so callbacks
 * are always safe to touch game/GUI state from.
 */
public class ApiClient {
    private static final Gson GSON = new Gson();
    // NEVER: we follow redirects ourselves so {@code X-User-Api-Key} is kept.
    // Java's built-in follow can drop auth-like headers on redirect (e.g.
    // Cloudflare http→https), which then shows up as a confusing HTTP 401.
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    public void listServers(
        String baseUrl,
        String userApiKey,
        SearchFilters filters,
        Consumer<ServerListResponse> onSuccess,
        Consumer<Throwable> onError
    ) {
        MeteorExecutor.execute(() -> {
            try {
                String key = normalizeUserApiKey(userApiKey);
                if (key.isEmpty()) {
                    throw new RuntimeException(
                        "User Access Code is empty. Paste the code from your Discord DM into the MineScan module settings."
                    );
                }

                URI uri = buildServersUri(baseUrl, filters);

                HttpResponse<String> response = sendGet(uri, key);
                // Cloudflare (and similar) may 301/302 http→https; re-send with the same headers.
                for (int hop = 0; hop < 3; hop++) {
                    int code = response.statusCode();
                    if (code != 301 && code != 302 && code != 307 && code != 308) {
                        break;
                    }
                    Optional<String> location = response.headers().firstValue("Location");
                    if (location.isEmpty()) {
                        break;
                    }
                    uri = uri.resolve(location.get());
                    response = sendGet(uri, key);
                }

                if (response.statusCode() != 200) {
                    throw new RuntimeException(formatHttpError(response.statusCode(), response.body(), key.length()));
                }

                ServerListResponse parsed = GSON.fromJson(response.body(), ServerListResponse.class);
                if (parsed.servers == null) parsed.servers = List.of();

                mc.execute(() -> onSuccess.accept(parsed));
            } catch (Exception e) {
                MineScanAddon.LOG.warn("Failed to fetch server list from {}", baseUrl, e);
                mc.execute(() -> onError.accept(e));
            }
        });
    }

    private HttpResponse<String> sendGet(URI uri, String userApiKey) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", "MineScan-Addon/1.0")
            .header("Accept", "application/json")
            .header("X-User-Api-Key", userApiKey)
            .GET();

        return HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Keys get pasted out of Discord code fences / with accidental whitespace
     * or dashed UUID form. Normalize so a curl-working key matches what
     * {@code blake3} hashed at registration.
     */
    public static String normalizeUserApiKey(String raw) {
        if (raw == null) {
            return "";
        }
        String key = raw.strip();
        if (key.startsWith("```")) {
            int start = key.indexOf('\n');
            int end = key.lastIndexOf("```");
            if (start >= 0 && end > start) {
                key = key.substring(start + 1, end).strip();
            } else {
                key = key.replace("```", "").strip();
            }
        }
        while (key.startsWith("`")) {
            key = key.substring(1).strip();
        }
        while (key.endsWith("`")) {
            key = key.substring(0, key.length() - 1).strip();
        }
        // Uuid::simple() is 32 hex chars; users sometimes paste the dashed form.
        if (key.length() == 36 && key.charAt(8) == '-' && key.charAt(13) == '-'
            && key.charAt(18) == '-' && key.charAt(23) == '-') {
            key = key.replace("-", "");
        }
        return key.strip();
    }

    private static String formatHttpError(int status, String body, int keyLen) {
        String detail = (body == null || body.isBlank()) ? "(empty body)" : body;
        if (status == 401) {
            return "API returned HTTP 401 (unauthorized). Your User Access Code was rejected. "
                + "Re-copy the newest code from the bot DM into MineScan settings "
                + "(code length now=" + keyLen + "). Verify with: curl -H \"X-User-Api-Key: YOUR_CODE\" "
                + "\"https://api.minescan.net/api/v1/servers?limit=1\". Body: " + detail;
        }
        if (status == 403) {
            return "API returned HTTP 403 (forbidden) - your code is revoked. Ask an admin to /unrevoke you. Body: " + detail;
        }
        if (status == 521) {
            return "API returned HTTP 521 - the MineScan API is unreachable right now. "
                + "Cloudflare could not reach the origin server (this is not an access-code problem). "
                + "Try again later once the API host is back online.";
        }
        if (status == 522 || status == 523 || status == 524) {
            return "API returned HTTP " + status + " - the MineScan API host is not responding. "
                + "Try again later once the server is back online.";
        }
        return "API returned HTTP " + status + ": " + detail;
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
        if (filters.whitelistStatus != null && !filters.whitelistStatus.isBlank()) appendParam(query, "whitelist_status", filters.whitelistStatus);
        if (filters.address != null && !filters.address.isBlank()) appendParam(query, "address", filters.address);
        if (filters.player != null && !filters.player.isBlank()) appendParam(query, "player", filters.player);

        return URI.create(base + "/api/v1/servers?" + query);
    }

    /**
     * {@link HttpRequest} requires an absolute URI with an explicit scheme
     * and throws a cryptic {@code IllegalArgumentException: URI with
     * undefined scheme} otherwise. Users very naturally type just
     * {@code host:port} into the API Base URL setting (omitting {@code
     * http://}), so default to that instead of surfacing that exception.
     * <p>
     * Also upgrades bare {@code http://api.minescan.net} to HTTPS so
     * Cloudflare's Always-Use-HTTPS redirect doesn't strip auth headers.
     */
    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.strip();
        if (!trimmed.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            trimmed = "http://" + trimmed;
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        URI uri = URI.create(trimmed);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if ("http".equalsIgnoreCase(uri.getScheme())
            && (host.equals("api.minescan.net") || host.equals("minescan.net"))) {
            int port = uri.getPort();
            if (port < 0 || port == 80 || port == 443) {
                trimmed = "https://" + host + (uri.getRawPath() == null ? "" : uri.getRawPath());
            }
        }
        return trimmed;
    }

    private void appendParam(StringBuilder query, String key, String value) {
        if (!query.isEmpty()) query.append('&');
        query.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
