package dev.mcfriendfinder.addon.api;

import java.util.List;

/**
 * Mirrors `ServerListResponse` from the Rust API's `GET /api/v1/servers`.
 */
public class ServerListResponse {
    public List<FoundServer> servers;
    public long limit;
    public long offset;
}
