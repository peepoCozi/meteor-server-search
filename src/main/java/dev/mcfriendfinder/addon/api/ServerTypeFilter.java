package dev.mcfriendfinder.addon.api;

/**
 * Filter over a server's detected software, mirroring
 * {@code mcff_core::software::ServerSoftware} on the API side. {@code
 * toString()} is what Meteor's {@code WDropdown} renders as the option
 * label.
 */
public enum ServerTypeFilter {
    ANY,
    VANILLA,
    PAPER,
    FOLIA,
    PURPUR,
    PUFFERFISH,
    SPIGOT,
    BUKKIT,
    FABRIC,
    QUILT,
    NEOFORGE,
    FORGE,
    VELOCITY,
    WATERFALL,
    BUNGEECORD;

    /** {@code null} means "omit the filter" - matches every server type. */
    public String toApiValue() {
        return this == ANY ? null : name().toLowerCase();
    }

    @Override
    public String toString() {
        return switch (this) {
            case ANY -> "Any";
            case VANILLA -> "Vanilla";
            case PAPER -> "Paper";
            case FOLIA -> "Folia";
            case PURPUR -> "Purpur";
            case PUFFERFISH -> "Pufferfish";
            case SPIGOT -> "Spigot";
            case BUKKIT -> "Bukkit";
            case FABRIC -> "Fabric";
            case QUILT -> "Quilt";
            case NEOFORGE -> "NeoForge";
            case FORGE -> "Forge";
            case VELOCITY -> "Velocity";
            case WATERFALL -> "Waterfall";
            case BUNGEECORD -> "BungeeCord";
        };
    }
}
