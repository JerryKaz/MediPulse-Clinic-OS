package com.ayaresa.security;

/** How much a role can do inside a module. Ordered from least to most access. */
public enum AccessLevel {
    /** The module is hidden and cannot be opened. */
    NONE,
    /** Read-only: can open the module but not create, edit or delete. */
    VIEW,
    /** Full access to the module. */
    FULL;

    public boolean atLeast(AccessLevel other) {
        return compareTo(other) >= 0;
    }
}
