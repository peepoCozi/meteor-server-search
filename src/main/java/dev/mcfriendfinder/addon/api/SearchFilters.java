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
    /** One of {@code ServerTypeFilter}'s API values (e.g. "paper"), or null for any. */
    public String serverType;
    /** One of {@code WhitelistFilter}'s API values ("open"/"whitelisted"/"unknown"), or null for any. */
    public String whitelistStatus;
    /** Exact IP match. Can legitimately return more than one row (nearby-port scan pass). */
    public String address;
    /** Case-insensitive substring match against any username ever seen on the server. */
    public String player;
    public long limit = 50;
    public long offset = 0;
}
