package dev.mcfriendfinder.addon.api;

/**
 * Tri-state filter over a server's detected whitelist status, mapped to the
 * API's nullable {@code whitelist_status} query param (see
 * {@link SearchFilters#whitelistStatus}). Online-mode servers always show as
 * {@code UNKNOWN} - see the root README's ethics notice for why. {@code
 * toString()} is what Meteor's {@code WDropdown} renders as the option label.
 */
public enum WhitelistFilter {
    ANY,
    OPEN_ONLY,
    WHITELISTED_ONLY,
    UNKNOWN_ONLY;

    /** {@code null} means "omit the filter" - matches any status. */
    public String toApiValue() {
        return switch (this) {
            case ANY -> null;
            case OPEN_ONLY -> "open";
            case WHITELISTED_ONLY -> "whitelisted";
            case UNKNOWN_ONLY -> "unknown";
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case ANY -> "Any";
            case OPEN_ONLY -> "Open Only";
            case WHITELISTED_ONLY -> "Whitelisted Only";
            case UNKNOWN_ONLY -> "Unknown Only";
        };
    }
}
