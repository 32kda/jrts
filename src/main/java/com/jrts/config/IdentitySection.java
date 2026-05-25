package com.jrts.config;

import java.util.List;

/**
 * Identity section parsed from TOML [identity] block.
 */
public record IdentitySection(
        String name,
        String displayName,
        String category,
        String owner,
        boolean nominal,
        boolean insignificant,
        boolean legalTarget,
        boolean selectable,
        boolean radarVisible,
        boolean radarInvisible,
        boolean cloakable,
        boolean cloakStop,
        String type) {

    public IdentitySection {
        if (type == null || type.isEmpty()) {
            type = "unit";
        }
    }
}
