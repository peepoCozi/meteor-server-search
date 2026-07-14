package dev.minescan.addon.api;

/**
 * Tri-state filter over a server's online-mode status, mapped to the API's
 * nullable {@code cracked} query param (see {@link SearchFilters#cracked}).
 * {@code toString()} is what Meteor's {@code WDropdown} renders as the
 * option label.
 */
public enum CrackedFilter {
    ANY,
    CRACKED_ONLY,
    PREMIUM_ONLY;

    /** {@code null} means "omit the filter" - matches both. */
    public Boolean toApiValue() {
        return switch (this) {
            case ANY -> null;
            case CRACKED_ONLY -> Boolean.TRUE;
            case PREMIUM_ONLY -> Boolean.FALSE;
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case ANY -> "Any";
            case CRACKED_ONLY -> "Cracked Only";
            case PREMIUM_ONLY -> "Premium Only";
        };
    }
}
