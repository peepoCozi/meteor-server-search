package dev.mcfriendfinder.addon.api;

/**
 * Client-side representation of the query params accepted by
 * `GET /api/v1/servers`. Any field left `null` is simply omitted from the
 * request.
 */
public class SearchFilters {
    public String versionName;
    public Integer minPlayers;
    public Integer maxPlayers;
    public Boolean cracked;
    public String motdContains;
    public long limit = 50;
    public long offset = 0;
}
