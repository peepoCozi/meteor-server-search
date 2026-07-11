package dev.mcfriendfinder.addon.api;

import com.google.gson.annotations.SerializedName;

/**
 * Mirrors `ServerSummary` from the Rust API (`scanner/api/src/routes/servers.rs`).
 */
public class FoundServer {
    public String address;
    public int port;
    public String motd;

    @SerializedName("version_name")
    public String versionName;

    @SerializedName("version_protocol")
    public int versionProtocol;

    public boolean cracked;

    @SerializedName("max_players")
    public Integer maxPlayers;

    @SerializedName("online_players")
    public Integer onlinePlayers;

    @SerializedName("last_seen")
    public String lastSeen;

    public String hostAndPort() {
        return address + ":" + port;
    }
}
