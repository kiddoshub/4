
package com.gaspoweredcake.client33.utils.misc;

public enum ListMode {
    Whitelist,
    Blacklist;

    public boolean allows(boolean contains) {
        return switch (this) {
            case Whitelist -> contains;
            case Blacklist -> !contains;
        };
    }
}
